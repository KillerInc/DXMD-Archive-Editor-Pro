import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds conservative human-readable context for Base Fields rows directly from
 * the archive's nearby ASCII identifiers. The goal is to show structural context,
 * not to pretend every changed byte has been individually decoded.
 */
final class ArchiveContextResolver {
    private static final Pattern LONG_HEX = Pattern.compile("[0-9A-Fa-f]{16,}");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern UPPER_PHRASE = Pattern.compile("[A-Za-z0-9 .'-]+");
    private static final Pattern CALIBER = Pattern.compile("^[.0-9]+(?:MM|IN)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final int MAX_CONTEXT_DISTANCE = 1400;
    private static final int BLOCK_GAP = 52;

    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "known", "suspected", "field", "raw", "for", "the", "of", "and", "to", "current",
            "xp", "level", "lvl", "archive", "research", "unknown", "clean", "game", "value"
    )));

    private static String cachedPath;
    private static long cachedSize;
    private static long cachedModified;
    private static ArchiveContextResolver cached;

    private final ArrayList<Token> readable = new ArrayList<>();
    private final ArrayList<Block> blocks = new ArrayList<>();

    private static final class Token {
        final long start, end;
        final String text;
        Token(long start, long end, String text) { this.start = start; this.end = end; this.text = text; }
    }

    private static final class Block {
        long start, end;
        final ArrayList<Token> tokens = new ArrayList<>();
        final HashSet<String> words = new HashSet<>();
        void add(Token t) {
            if (tokens.isEmpty()) start = t.start;
            tokens.add(t);
            end = t.end;
            words.addAll(words(t.text));
        }
    }

    private ArchiveContextResolver(byte[] bytes) {
        scan(bytes);
        buildBlocks();
    }

    static synchronized String[] resolve(File archive, List<BaseResearchProfiles.Field> fields) throws IOException {
        if (archive == null || fields == null) return new String[0];
        String path = archive.getCanonicalPath();
        long size = archive.length();
        long modified = archive.lastModified();
        if (cached == null || !path.equals(cachedPath) || size != cachedSize || modified != cachedModified) {
            cached = new ArchiveContextResolver(Files.readAllBytes(archive.toPath()));
            cachedPath = path;
            cachedSize = size;
            cachedModified = modified;
        }
        String[] out = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) out[i] = cached.contextFor(fields.get(i));
        return out;
    }

    static String fallback(BaseResearchProfiles.Field f) {
        if (f == null) return "No nearby readable identifier";
        String base = legacyBase(f.label);
        if (base.isEmpty() || base.toLowerCase(Locale.ROOT).startsWith("raw field"))
            return "No nearby readable identifier";
        return base;
    }

    private void scan(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            if (isPrintable(bytes[i])) {
                int start = i;
                while (i < bytes.length && isPrintable(bytes[i])) i++;
                int len = i - start;
                if (len >= 4 && len <= 120) {
                    String s = new String(bytes, start, len, StandardCharsets.US_ASCII).trim();
                    if (isReadable(s)) readable.add(new Token(start, i, s));
                }
            } else i++;
        }
    }

    private void buildBlocks() {
        Block current = null;
        for (Token t : readable) {
            if (!isStrongIdentifier(t.text)) continue;
            if (current == null || t.start - current.end > BLOCK_GAP) {
                current = new Block();
                blocks.add(current);
            }
            current.add(t);
        }
    }

    private String contextFor(BaseResearchProfiles.Field field) {
        String verified = verifiedAlias(field);
        if (verified != null) return verified;

        String legacy = legacyBase(field.label);
        Block block = bestBlock(field, legacy);
        if (block != null) {
            for (Token t : block.tokens) {
                if (!legacy.isEmpty() && !isGenericLegacy(legacy) && t.text.equalsIgnoreCase(legacy)) return t.text;
            }
            if (FieldConfidenceAudit.isBaseConfirmed(field)) {
                String matched = bestKnownToken(field, block);
                if (matched != null) return matched;
            }
            return summarize(block);
        }

        Token fallback = bestReadable(field);
        if (fallback != null) return fallback.text;
        return fallback(field);
    }

    private Block bestBlock(BaseResearchProfiles.Field field, String legacy) {
        if (blocks.isEmpty()) return null;
        int at = lowerBound(field.offset);
        Block best = null;
        double bestScore = -Double.MAX_VALUE;
        Set<String> hint = words((field.label == null ? "" : field.label) + " " + (field.category == null ? "" : field.category));
        boolean generic = isGenericLegacy(legacy);

        int from = Math.max(0, at - 24);
        int to = Math.min(blocks.size(), at + 24);
        for (int i = from; i < to; i++) {
            Block b = blocks.get(i);
            long d = distance(field.offset, b.start, b.end);
            if (d > MAX_CONTEXT_DISTANCE) continue;
            double score = -d / 8.0;
            int overlap = overlap(hint, b.words);
            score += overlap * 45.0;
            if (FieldConfidenceAudit.isBaseConfirmed(field)) score += overlap * 20.0;
            score += Math.min(8, b.tokens.size()) * 1.5;

            if (!generic) {
                String l = legacy.toLowerCase(Locale.ROOT);
                for (Token t : b.tokens) {
                    String n = t.text.toLowerCase(Locale.ROOT);
                    if (n.equals(l)) score += 180.0;
                    else if (l.length() >= 5 && (n.contains(l) || l.contains(n))) score += 60.0;
                }
            }
            for (String h : hint) {
                if (h.length() < 4) continue;
                for (Token t : b.tokens) if (t.text.toLowerCase(Locale.ROOT).contains(h)) score += 18.0;
            }
            if (score > bestScore) { bestScore = score; best = b; }
        }
        return best;
    }

    private String bestKnownToken(BaseResearchProfiles.Field field, Block block) {
        Set<String> hint = words(field.label == null ? "" : field.label);
        int best = 0;
        String bestText = null;
        for (Token t : block.tokens) {
            Set<String> tw = words(t.text);
            int score = overlap(hint, tw) * 10;
            String lower = t.text.toLowerCase(Locale.ROOT);
            for (String h : hint) if (h.length() >= 4 && lower.contains(h)) score += 3;
            if (score > best) { best = score; bestText = t.text; }
        }
        return bestText;
    }

    private Token bestReadable(BaseResearchProfiles.Field field) {
        Token best = null;
        double bestScore = -Double.MAX_VALUE;
        Set<String> hint = words((field.label == null ? "" : field.label) + " " + (field.category == null ? "" : field.category));
        for (Token t : readable) {
            long d = distance(field.offset, t.start, t.end);
            if (d > 900) continue;
            if (!looksSafeFallback(t.text)) continue;
            int overlap = overlap(hint, words(t.text));
            if (t.text.indexOf('\\') >= 0 && overlap == 0) continue;
            double score = -d / 10.0 + overlap * 40.0;
            if (t.text.indexOf('_') >= 0) score += 8.0;
            if (t.text.indexOf(' ') >= 0) score += 4.0;
            if (t.text.length() <= 48) score += 3.0;
            if (t.text.indexOf('\\') >= 0) score -= 8.0;
            if (score > bestScore) { bestScore = score; best = t; }
        }
        return best;
    }

    private int lowerBound(long offset) {
        int lo = 0, hi = blocks.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (blocks.get(mid).start < offset) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static String summarize(Block b) {
        if (b.tokens.size() == 1) return b.tokens.get(0).text;
        if (b.tokens.size() == 2) return b.tokens.get(0).text + " → " + b.tokens.get(1).text;
        return b.tokens.get(0).text + " … " + b.tokens.get(b.tokens.size() - 1).text;
    }

    private static long distance(long off, long start, long end) {
        if (off >= start && off <= end) return 0;
        return Math.min(Math.abs(off - start), Math.abs(off - end));
    }

    private static boolean isPrintable(byte b) {
        int v = b & 255;
        return v >= 32 && v <= 126;
    }

    private static boolean isReadable(String s) {
        if (s.length() < 4 || LONG_HEX.matcher(s).matches()) return false;
        int letters = 0, allowed = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) letters++;
            if (Character.isLetterOrDigit(c) || "_- .:/\\[]()'".indexOf(c) >= 0) allowed++;
        }
        return letters >= 3 && allowed * 100 >= s.length() * 95;
    }

    private static boolean isStrongIdentifier(String s) {
        if (s.length() < 4 || s.length() > 96 || LONG_HEX.matcher(s).matches()) return false;
        if (s.indexOf('\\') >= 0 || s.indexOf('/') >= 0) return false;
        if (s.indexOf('_') >= 0 && IDENT.matcher(s).matches()) return true;
        if (CALIBER.matcher(s).matches()) return true;
        if (s.toUpperCase(Locale.ROOT).contains("AMMO") && UPPER_PHRASE.matcher(s).matches()) return true;
        int letters = 0;
        boolean upper = true;
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) {
            letters++;
            if (!Character.isUpperCase(s.charAt(i))) upper = false;
        }
        return letters >= 4 && upper && UPPER_PHRASE.matcher(s).matches();
    }

    private static boolean looksSafeFallback(String s) {
        if (!isReadable(s)) return false;
        int letters = 0;
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) letters++;
        if (letters < 4) return false;
        return s.length() >= 6 || s.indexOf(' ') >= 0 || s.indexOf('_') >= 0;
    }

    private static String legacyBase(String label) {
        if (label == null) return "";
        String s = label.trim();
        s = s.replaceFirst("\\s+#\\d+$", "");
        s = s.replaceFirst("\\s+\\[\\d+/\\d+\\]$", "");
        s = s.replaceFirst("\\s+\\[[^\\]]*#\\d+\\]$", "");
        s = s.replaceFirst("(?i)\\s+\\(raw\\)$", "");
        return s.trim();
    }

    private static boolean isGenericLegacy(String s) {
        String l = s == null ? "" : s.toLowerCase(Locale.ROOT);
        return l.isEmpty() || l.startsWith("raw field") || l.equals("level (raw)");
    }

    private static Set<String> words(String s) {
        if (s == null || s.isEmpty()) return Collections.emptySet();
        String n = s.toLowerCase(Locale.ROOT)
                .replace("non-lethal", "nonlethal")
                .replace("non lethal", "nonlethal")
                .replace("multi-tool", "multitool")
                .replace("weapon parts", "weapon_parts");
        String[] parts = n.split("[^a-z0-9]+");
        HashSet<String> out = new HashSet<>();
        for (String p : parts) {
            if (p.isEmpty() || STOP_WORDS.contains(p)) continue;
            if (p.length() == 1 && !Character.isDigit(p.charAt(0))) continue;
            if (p.startsWith("hacking") || p.startsWith("hacker")) p = "hack";
            if (p.equals("grenades")) p = "grenade";
            if (p.equals("parts")) p = "part";
            out.add(p);
        }
        return out;
    }

    private static int overlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int n = 0;
        for (String s : a) if (b.contains(s)) n++;
        return n;
    }

    /** Verified DXMD reward-name to internal archive-name mappings. */
    private static String verifiedAlias(BaseResearchProfiles.Field f) {
        if (f == null || f.label == null) return null;
        String l = f.label.toLowerCase(Locale.ROOT);
        if (!(f.category != null && (f.category.startsWith("KNOWN") || f.category.startsWith("Suspected • XP") || f.category.startsWith("Research • XP")))) return null;

        if (l.contains("script kiddie")) return "hacking_lvl_1";
        if (l.contains("grey hat")) return "hacking_lvl_2";
        if (l.contains("black hat")) return "hacking_lvl_3";
        if (l.contains("network adept")) return "hacking_lvl_4";
        if (l.contains("master hacker")) return "hacking_lvl_5";
        if (l.contains("first try")) return "hacking_firsttry";

        if (l.contains("access granted")) return "pw_lvl_1";
        if (l.contains("free admission")) return "pw_lvl_2";
        if (l.contains("open sesame")) return "pw_lvl_3";
        if (l.contains("entering without breaking")) return "pw_lvl_4";
        if (l.contains("master felonist")) return "pw_lvl_5";

        if (l.contains("ghost")) return "obj_notseen";
        if (l.contains("smooth operator")) return "obj_noalarm";
        if (l.contains("getting things done")) return "obj_getthingsdone";
        if (l.contains("completionist")) return "obj_sidequest";
        if (l.contains("reset")) return "moodswing_cautiousreturn";

        if (l.contains("wait your turn")) return "social_interrupt_fail";
        if (l.contains("social_interrupt_split")) return "social_interrupt_split";
        if (l.contains("stop the press")) return "social_interrupt_win";
        if (l.contains("life lesson")) return "social_debate_lose";
        if (l.contains("split decision")) return "social_debate_neutral";
        if (l.contains("silver tongue")) return "social_debate_win";
        if (l.contains("read the room")) return "social_persuade_fail";
        if (l.contains("on the fence")) return "social_persuade_split";
        if (l.contains("spin doctor")) return "social_persuade_win";

        if (l.contains("machina")) return "remotehacking_success";
        if (l.contains("flawless")) return "remotehacking_nomiss_alt";
        if (l.contains("remotehacking_haywire")) return "remotehacking_haywire";
        if (l.contains("paving the way")) return "remotehacking_environment";

        if (l.contains("traveler")) return "secretarea_traveler";
        if (l.contains("explorer")) return "secretarea_explorer";
        if (l.contains("pathfinder")) return "secretarea_pathfinder";
        if (l.contains("trailblazer")) return "secretarea_trailblazer";
        if (l.contains("collect_treasure")) return "collect_treasure";
        if (l.contains("scholar")) return "collect_scholar";

        if (l.contains("sorry to disappoint")) return "combat_incap_sorrytodisappoint";
        if (l.contains("elite") && l.contains("tier")) return "combat_incap_bigdawg";
        if (l.contains("trooper") && l.contains("tier")) return "combat_incap_smallfry";
        if (l.contains("veteran") && l.contains("tier")) return "combat_incap_veteran";
        if (l.contains("merciful soul") || l.contains("non-lethal")) return "combat_xp_nonlethal";
        if (l.contains("marksman") || l.contains("headshot")) return "combat_xp_headshot";
        if (l.contains("boss-specific takedown") || l.contains("expedient")) return "combat_xp_takedown";
        if (l.contains("multitasker")) return "combat_xp_takedownmulti";
        if (l.contains("shock therapy")) return "combat_xp_tesla";
        if (l.contains("surprise")) return "combat_xp_ptw";
        if (l.contains("close shave")) return "combat_xp_nano";
        if (l.contains("dust to dust")) return "combat_xp_peps";
        if (l.contains("introvert")) return "combat_xp_typhoon";
        if (l.contains("juggernaut")) return "combat_xp_juggernaut";
        if (l.contains("crash landing")) return "combat_xp_crashland";
        if (l.contains("piece by piece")) return "combat_xp_piecebypiece";
        if (l.contains("sharpshooter")) return "combat_xp_sharpshooter";
        if (l.contains("chain reaction")) return "combat_xp_chainreaction";
        if (l.contains("master blaster")) return "combat_xp_masterblaster";
        if (l.contains("combat_xp_twister")) return "combat_xp_twister";
        if (l.contains("ring of fire")) return "combat_xp_ringoffire";
        if (l.contains("blown away")) return "combat_xp_frag";
        if (l.contains("collateral damage")) return "combat_xp_grenademulti";
        if (l.contains("combat_xp_boss")) return "combat_xp_boss";
        if (l.contains("combat_xp_explocrate")) return "combat_xp_explocrate";
        if (l.contains("combat_xp_npconnpc")) return "combat_xp_NPConNPC";
        if (l.contains("junk yard")) return "combat_disable_sentry";
        if (l.contains("scrap metal")) return "combat_disable_turret";
        if (l.contains("void warranty")) return "combat_disable_drone";
        return null;
    }
}
