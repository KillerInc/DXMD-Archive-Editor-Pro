import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds conservative human-readable context for DLC Fields rows directly from
 * the selected DLC archive. Generated profile labels are treated as research
 * hints only; they are not automatically presented as exact archive identities.
 */
final class DLCArchiveContextResolver {
    private static final Pattern HEXISH = Pattern.compile("[0-9A-Fa-f]{6,}");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern UPPER_PHRASE = Pattern.compile("[A-Za-z0-9 .'-]+");
    private static final Pattern CALIBER = Pattern.compile("^[.0-9]+(?:MM|IN)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final int MAX_KNOWN_BLOCK_DISTANCE = 1000;
    private static final int MAX_RESEARCH_BLOCK_DISTANCE = 500;
    private static final int MAX_KNOWN_FALLBACK_DISTANCE = 600;
    private static final int MAX_RESEARCH_FALLBACK_DISTANCE = 320;
    private static final int BLOCK_GAP = 52;

    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "field", "raw", "unknown", "archive", "research", "value", "current", "original",
            "suspected", "known", "dlc", "pack"
    )));

    private static String cachedPath;
    private static long cachedSize;
    private static long cachedModified;
    private static DLCArchiveContextResolver cached;

    private final ArrayList<Token> readable = new ArrayList<>();
    private final ArrayList<Block> blocks = new ArrayList<>();

    private static final class Token {
        final long start;
        final long end;
        final String text;

        Token(long start, long end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    private static final class Block {
        long start;
        long end;
        final ArrayList<Token> tokens = new ArrayList<>();
        final HashSet<String> words = new HashSet<>();

        void add(Token token) {
            if (tokens.isEmpty()) start = token.start;
            tokens.add(token);
            end = token.end;
            words.addAll(words(token.text));
        }
    }

    private DLCArchiveContextResolver(byte[] bytes) {
        scan(bytes);
        buildBlocks();
    }

    static synchronized String[] resolve(File archive, List<DLCProfiles.Field> fields) throws IOException {
        if (archive == null || fields == null) return new String[0];

        String path = archive.getCanonicalPath();
        long size = archive.length();
        long modified = archive.lastModified();
        if (cached == null || !path.equals(cachedPath) || size != cachedSize || modified != cachedModified) {
            cached = new DLCArchiveContextResolver(Files.readAllBytes(archive.toPath()));
            cachedPath = path;
            cachedSize = size;
            cachedModified = modified;
        }

        String[] out = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            DLCProfiles.Field field = fields.get(i);
            out[i] = cached.contextFor(field.offset, field.label, FieldConfidenceAudit.isDlcConfirmed(archive.getName(), field));
        }
        return out;
    }

    static String fallback(DLCProfiles.Field field) {
        if (field == null) return "No nearby readable identifier";
        String base = legacyBase(field.label);
        if (isGenericLegacy(base) || FieldConfidenceAudit.isGenericWeaponFamilyLabel(field.label)) return "No nearby readable identifier";
        return base;
    }

    private void scan(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            if (!isPrintable(bytes[i])) {
                i++;
                continue;
            }

            int start = i;
            while (i < bytes.length && isPrintable(bytes[i])) i++;
            int len = i - start;
            if (len < 4 || len > 120) continue;

            String text = new String(bytes, start, len, StandardCharsets.US_ASCII).trim();
            if (isReadable(text)) readable.add(new Token(start, i, text));
        }
    }

    private void buildBlocks() {
        Block current = null;
        for (Token token : readable) {
            if (!isStrongIdentifier(token.text)) continue;
            if (current == null || token.start - current.end > BLOCK_GAP) {
                current = new Block();
                blocks.add(current);
            }
            current.add(token);
        }
    }

    private String contextFor(long offset, String label, boolean confirmed) {
        String legacy = legacyBase(label);
        Block block = bestBlock(offset, legacy, label, confirmed);
        if (block != null) {
            for (Token token : block.tokens) {
                if (!isGenericLegacy(legacy) && token.text.equalsIgnoreCase(legacy)) return token.text;
            }
            if (confirmed) {
                String matched = bestKnownToken(label, block);
                if (matched != null) return matched;
            }
            return summarize(block);
        }

        Token fallback = bestReadable(offset, label, confirmed);
        if (fallback != null) return fallback.text;
        return fallbackLabel(label);
    }

    private Block bestBlock(long offset, String legacy, String label, boolean confirmed) {
        if (blocks.isEmpty()) return null;
        int at = lowerBound(offset);
        Block best = null;
        double bestScore = -Double.MAX_VALUE;
        Set<String> hint = words(label == null ? "" : label);
        boolean generic = isGenericLegacy(legacy);
        int maxDistance = confirmed ? MAX_KNOWN_BLOCK_DISTANCE : MAX_RESEARCH_BLOCK_DISTANCE;

        int from = Math.max(0, at - 20);
        int to = Math.min(blocks.size(), at + 20);
        for (int i = from; i < to; i++) {
            Block block = blocks.get(i);
            long distance = distance(offset, block.start, block.end);
            if (distance > maxDistance) continue;

            int overlap = overlap(hint, block.words);
            double score = -distance / 8.0 + overlap * 45.0;
            score += Math.min(8, block.tokens.size()) * 1.5;

            if (!generic) {
                String expected = legacy.toLowerCase(Locale.ROOT);
                for (Token token : block.tokens) {
                    String actual = token.text.toLowerCase(Locale.ROOT);
                    if (actual.equals(expected)) score += 180.0;
                    else if (expected.length() >= 5 && (actual.contains(expected) || expected.contains(actual))) score += 55.0;
                }
            }

            if (!confirmed && overlap == 0 && distance > 220) score -= 80.0;
            if (score > bestScore) {
                bestScore = score;
                best = block;
            }
        }

        if (best == null) return null;
        long bestDistance = distance(offset, best.start, best.end);
        if (!confirmed && bestDistance > 220 && overlap(hint, best.words) == 0) return null;
        return best;
    }

    private String bestKnownToken(String label, Block block) {
        Set<String> hint = words(label == null ? "" : label);
        int bestScore = 0;
        String bestText = null;
        for (Token token : block.tokens) {
            int score = overlap(hint, words(token.text)) * 10;
            String lower = token.text.toLowerCase(Locale.ROOT);
            for (String word : hint) if (word.length() >= 4 && lower.contains(word)) score += 3;
            if (score > bestScore) {
                bestScore = score;
                bestText = token.text;
            }
        }
        return bestText;
    }

    private Token bestReadable(long offset, String label, boolean confirmed) {
        Token best = null;
        double bestScore = -Double.MAX_VALUE;
        Set<String> hint = words(label == null ? "" : label);
        int maxDistance = confirmed ? MAX_KNOWN_FALLBACK_DISTANCE : MAX_RESEARCH_FALLBACK_DISTANCE;

        for (Token token : readable) {
            long distance = distance(offset, token.start, token.end);
            if (distance > maxDistance || !looksSafeFallback(token.text)) continue;

            int overlap = overlap(hint, words(token.text));
            if (token.text.indexOf('\\') >= 0 && overlap == 0) continue;
            if (!confirmed && overlap == 0 && distance > 160) continue;

            double score = -distance / 10.0 + overlap * 40.0;
            if (token.text.indexOf('_') >= 0) score += 8.0;
            if (token.text.indexOf(' ') >= 0) score += 4.0;
            if (token.text.length() <= 48) score += 3.0;
            if (token.text.indexOf('\\') >= 0) score -= 8.0;
            if (score > bestScore) {
                bestScore = score;
                best = token;
            }
        }
        return best;
    }

    private int lowerBound(long offset) {
        int lo = 0;
        int hi = blocks.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (blocks.get(mid).start < offset) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static String summarize(Block block) {
        if (block.tokens.size() == 1) return block.tokens.get(0).text;
        if (block.tokens.size() == 2) return block.tokens.get(0).text + " → " + block.tokens.get(1).text;
        return block.tokens.get(0).text + " … " + block.tokens.get(block.tokens.size() - 1).text;
    }

    private static long distance(long offset, long start, long end) {
        if (offset >= start && offset <= end) return 0;
        return Math.min(Math.abs(offset - start), Math.abs(offset - end));
    }

    private static boolean isPrintable(byte value) {
        int v = value & 255;
        return v >= 32 && v <= 126;
    }

    private static boolean isReadable(String text) {
        if (text.length() < 4 || isHexish(text)) return false;
        int letters = 0;
        int allowed = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) letters++;
            if (Character.isLetterOrDigit(c) || "_- .:/\\[]()'".indexOf(c) >= 0) allowed++;
        }
        return letters >= 3 && allowed * 100 >= text.length() * 95;
    }

    private static boolean isStrongIdentifier(String text) {
        if (text.length() < 4 || text.length() > 96 || isHexish(text)) return false;
        if (text.indexOf('\\') >= 0 || text.indexOf('/') >= 0) return false;
        if (text.indexOf('_') >= 0 && IDENT.matcher(text).matches()) return true;
        if (CALIBER.matcher(text).matches()) return true;
        if (text.toUpperCase(Locale.ROOT).contains("AMMO") && UPPER_PHRASE.matcher(text).matches()) return true;

        int letters = 0;
        boolean upper = true;
        for (int i = 0; i < text.length(); i++) if (Character.isLetter(text.charAt(i))) {
            letters++;
            if (!Character.isUpperCase(text.charAt(i))) upper = false;
        }
        return letters >= 4 && upper && UPPER_PHRASE.matcher(text).matches();
    }

    private static boolean looksSafeFallback(String text) {
        if (!isReadable(text) || isHexish(text)) return false;
        int letters = 0;
        for (int i = 0; i < text.length(); i++) if (Character.isLetter(text.charAt(i))) letters++;
        if (letters < 4) return false;
        return text.length() >= 6 || text.indexOf(' ') >= 0 || text.indexOf('_') >= 0;
    }

    private static boolean isHexish(String text) {
        String compact = text.replace(" ", "").replace("-", "");
        return HEXISH.matcher(compact).matches();
    }

    private static String fallbackLabel(String label) {
        String base = legacyBase(label);
        if (isGenericLegacy(base) || FieldConfidenceAudit.isGenericWeaponFamilyLabel(label)) return "No nearby readable identifier";
        return base;
    }

    private static String legacyBase(String label) {
        if (label == null) return "";
        String text = label.trim();
        text = text.replaceFirst("\\s+#\\d+$", "");
        text = text.replaceFirst("\\s+\\[\\d+/\\d+\\]$", "");
        text = text.replaceFirst("\\s+\\[[^\\]]*#\\d+\\]$", "");
        text = text.replaceFirst("(?i)\\s+\\(raw\\)$", "");
        return text.trim();
    }

    private static boolean isGenericLegacy(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return lower.isEmpty() || lower.startsWith("raw field") || lower.equals("level (raw)") || isHexish(lower);
    }

    private static Set<String> words(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        HashSet<String> out = new HashSet<>();
        for (String part : parts) {
            if (part.isEmpty() || STOP_WORDS.contains(part)) continue;
            if (part.length() == 1 && !Character.isDigit(part.charAt(0))) continue;
            out.add(part);
        }
        return out;
    }

    private static int overlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int count = 0;
        for (String value : a) if (b.contains(value)) count++;
        return count;
    }
}
