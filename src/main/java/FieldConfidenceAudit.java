import java.util.*;

/**
 * Central confidence policy for Base/DLC research fields.
 *
 * Generated nearby labels are evidence, not proof. A field is CONFIRMED only
 * when a focused comparison or an established editor mapping isolates its role.
 * Repeated family labels such as DAMAGE_01 / ACCURACY_01 are deliberately kept
 * as suspected components unless one exact row has independent evidence.
 */
final class FieldConfidenceAudit {
    enum Confidence { CONFIRMED, STRONG_SUSPECTED, SUSPECTED, UNIDENTIFIED }

    static final class Assessment {
        final Confidence confidence;
        final String name;
        final String evidence;
        final boolean saveRisk;
        Assessment(Confidence confidence, String name, String evidence, boolean saveRisk) {
            this.confidence=confidence; this.name=name; this.evidence=evidence; this.saveRisk=saveRisk;
        }
    }

    private static final HashSet<String> UPGRADE_COST_FAMILIES = new HashSet<>(Arrays.asList(
            "Accuracy", "Ammo Capacity", "Damage", "Fire Pattern", "Rate of Fire", "Recoil", "Burst"
    ));
    private static Set<String> dlcUpgradeCostPatterns;

    private FieldConfidenceAudit() {}

    static boolean isBaseConfirmed(BaseResearchProfiles.Field f) {
        if (f==null) return false;
        RawArchiveAuditCatalog.Mapping raw=rawBaseMapping(f);
        if (raw!=null) return raw.level==RawArchiveAuditCatalog.Level.CONFIRMED;
        if (f.category==null || !f.category.startsWith("KNOWN")) return false;
        // Generated weapon-family labels are never proof by themselves. The only
        // currently confirmed base weapon-stat rows are isolated magazine mappings.
        if (weaponFamily(f.label)!=null) {
            return f.offset==4937661L || f.offset==4981373L;
        }
        return true;
    }

    static Assessment assessBase(BaseResearchProfiles.Field f, String nearbySpecific, String nearbyContext) {
        if (f==null) return unidentified();
        RawArchiveAuditCatalog.Mapping raw=rawBaseMapping(f);
        if (raw!=null) return rawAssessment(raw);
        if (isBaseConfirmed(f)) {
            String name=f.label;
            if (f.label!=null && f.label.startsWith("Ammo Stack") && nearbySpecific!=null && !nearbySpecific.trim().isEmpty())
                name=nearbySpecific+" Ammo Stack";
            return new Assessment(Confidence.CONFIRMED, name, baseConfirmedEvidence(f), isBaseSaveRisk(f));
        }

        String family=weaponFamily(f.label);
        if (family!=null) {
            String subject=(nearbySpecific==null||nearbySpecific.trim().isEmpty())?"Weapon":nearbySpecific;
            if (isBaseUpgradeCostCandidate(f, family)) {
                return new Assessment(Confidence.STRONG_SUSPECTED,
                        "Strong suspected: "+subject+" "+family+" Upgrade Parts Cost — "+seriesAndComponent(f.label),
                        "Adam 3.0 changes this short field to zero. That mod explicitly removes the Weapon Parts required to upgrade weapons, so this row is a strong upgrade-cost candidate rather than a confirmed "+family+" stat.", false);
            }
            return new Assessment(Confidence.SUSPECTED,
                    "Suspected: "+subject+" Weapon Stat — "+friendlyFamilyComponent(f.label),
                    baseWeaponEvidence(f, family), false);
        }

        String u=(f.label==null?"":f.label.toUpperCase(Locale.ROOT));
        String c=(f.category==null?"":f.category.toUpperCase(Locale.ROOT));
        if (u.startsWith("RAW FIELD") && (c.contains("ADAM 3.0") || c.contains("WEAPON") || nearbySpecific!=null)) {
            return new Assessment(Confidence.SUSPECTED, "Suspected: Weapon Stat — Unknown",
                    "This row changes in a weapon-affecting comparison or sits in weapon context, but the available comparisons do not isolate one exact property.", false);
        }
        if (c.contains("SUSPECTED • HEALTH REGENERATION"))
            return new Assessment(Confidence.SUSPECTED, "Suspected: "+cleanGeneratedLabel(f.label), "Comparison evidence suggests health-regeneration behavior, but the exact sub-field is not isolated.", false);
        if (c.contains("SUSPECTED • CONSUMABLE EFFECTS"))
            return new Assessment(Confidence.SUSPECTED, "Suspected: "+cleanGeneratedLabel(f.label), "Comparison evidence suggests a consumable effect, but the exact sub-field is not isolated.", false);
        if (c.contains("WEAPONS / COMBAT"))
            return new Assessment(Confidence.SUSPECTED, "Suspected: Weapon Stat — "+cleanGeneratedLabel(f.label), "The row is part of the weapon/combat research region, but its precise role is not independently isolated.", false);
        if (c.contains("AUGMENT"))
            return new Assessment(Confidence.SUSPECTED, "Suspected: Augmentation Attribute — "+cleanGeneratedLabel(f.label), "The row is associated with augmentation comparisons, but the precise role is not independently isolated.", false);
        return unidentified();
    }

    static boolean isBaseSaveRisk(BaseResearchProfiles.Field f) {
        if (f==null) return false;
        RawArchiveAuditCatalog.Mapping raw=rawBaseMapping(f);
        if (raw!=null) return raw.saveRisk;
        if (!isBaseConfirmed(f)) return false;
        String l=f.label==null?"":f.label.toUpperCase(Locale.ROOT);
        return l.contains(" WIDTH") || l.contains(" HEIGHT") || l.contains("INVENTORY SIZE");
    }

    static boolean isDlcConfirmed(String profileName, DLCProfiles.Field f) {
        if (f==null) return false;
        RawArchiveAuditCatalog.Mapping raw=rawDlcMapping(profileName,f);
        if (raw!=null) return raw.level==RawArchiveAuditCatalog.Level.CONFIRMED;
        String p=shortProfile(profileName);
        long o=f.offset;
        return (p.equals("Enforcer") && o==31578472L) ||
               (p.equals("Tactical") && (o==106868L || o==107948L));
    }

    static boolean isDlcSaveRisk(String profileName, DLCProfiles.Field f) {
        if (f==null) return false;
        RawArchiveAuditCatalog.Mapping raw=rawDlcMapping(profileName,f);
        if (raw!=null) return raw.saveRisk;
        long o=f.offset;
        String p=shortProfile(profileName);
        if ((p.equals("Enforcer") && o==31578472L) || (p.equals("Tactical") && o==106868L) || (p.equals("Assault") && o==54331L)) return true;
        String u=f.label==null?"":f.label.toUpperCase(Locale.ROOT);
        return u.contains("INVENTORY WIDTH") || u.contains("INVENTORY HEIGHT");
    }

    static Assessment assessDlc(String profileName, DLCProfiles.Field f, String nearbyContext) {
        if (f==null) return unidentified();
        RawArchiveAuditCatalog.Mapping raw=rawDlcMapping(profileName,f);
        if (raw!=null) return rawAssessment(raw);
        String p=shortProfile(profileName);
        long o=f.offset;

        // Confirmed only where focused comparison evidence isolates the effect.
        if (p.equals("Enforcer") && o==31578472L) {
            return new Assessment(Confidence.CONFIRMED, "Elite Combat Rifle Inventory Grid Control",
                    "Confirmed by independent inventory-focused comparisons: Master Inventory changes this byte 5→3, I Need The Edge variants change it 5→3 / 5→2, and IPOAO changes it 5→1.", true);
        }
        if (p.equals("Tactical") && o==106868L) {
            return new Assessment(Confidence.CONFIRMED, "Elite Tranquilizer Rifle Inventory Grid Control",
                    "Confirmed as an inventory-grid control by Master Inventory, I Need The Edge variants, and IPOAO all changing this same byte in the Tactical DLC archive.", true);
        }
        if (p.equals("Tactical") && o==107948L) {
            return new Assessment(Confidence.CONFIRMED, "Elite Tranquilizer Rifle Magazine Capacity",
                    "Confirmed by I Need The Edge v1.1/v1.2: the documented Elite Tranquilizer Rifle magazine change is 10→4 and this byte changes exactly 0x0A→0x04.", false);
        }

        // Strongly supported effects that are not precise enough to call confirmed.
        if (p.equals("Assault") && o==54331L) {
            return new Assessment(Confidence.STRONG_SUSPECTED, "Strong suspected: Elite Battle Rifle Inventory Grid Control",
                    "I Need The Edge comparison variants change this byte 5→3 and 5→2 while their optional modules resize Battle/Elite weapon inventory grids. No second independent focused comparison isolates it, so it remains suspected.", true);
        }
        if (p.equals("Enforcer") && o==31593122L) {
            return new Assessment(Confidence.STRONG_SUSPECTED, "Strong suspected: Elite Combat Rifle Suppressor Damage-Penalty Control",
                    "I Need The Edge v1.2 documents removing suppressor damage debuffs; this field changes 0x40→0xA6 in that comparison. The exact internal meaning of the byte is still not independently decoded.", false);
        }
        if (p.equals("Intruder") && o==14844588L) {
            return new Assessment(Confidence.STRONG_SUSPECTED, "Strong suspected: Elite Pistol Suppressor Damage-Penalty Control",
                    "This is the isolated Intruder DLC change in the I Need The Edge v1.2 comparison, whose documented DLC change removes suppressor damage debuffs. Exact byte semantics remain unverified.", false);
        }
        if (p.equals("Enforcer") && o==31593408L) {
            return new Assessment(Confidence.SUSPECTED, "Suspected: Elite Combat Rifle Suppressor Companion Field",
                    "This byte changes alongside the focused suppressor edit in I Need The Edge, but it is not independently isolated from the neighboring suppressor control.", false);
        }
        if (p.equals("Tactical") && (o==107324L || o==108070L)) {
            return new Assessment(Confidence.SUSPECTED, "Suspected: Micro-Assembler / Augmentation Control",
                    "The Micro-Assembler reference changes this field, but that reference archive also changes known weapon inventory/capacity bytes. It is useful evidence, not enough to call this field confirmed.", false);
        }

        String family=weaponFamily(f.label);
        if (family!=null) {
            String weapon=dlcWeapon(profileName);
            if (isDlcUpgradeCostCandidate(f,family)) {
                return new Assessment(Confidence.STRONG_SUSPECTED,
                        "Strong suspected: "+weapon+" "+family+" Upgrade Parts Cost — "+seriesAndComponent(f.label),
                        "This two-byte DLC field matches the same family/value pattern as a Base field that Adam 3.0 zeros when removing Weapon Parts upgrade costs. That structural match is strong evidence for an upgrade-cost component, not a confirmed "+family+" stat.", false);
            }
            return new Assessment(Confidence.SUSPECTED,
                    "Suspected: "+weapon+" Weapon Stat — "+friendlyFamilyComponent(f.label),
                    dlcFamilyEvidence(profileName,f,family), false);
        }

        if (looksWeaponContext(nearbyContext)) {
            return new Assessment(Confidence.SUSPECTED, "Suspected: "+dlcWeapon(profileName)+" Weapon Stat — Unknown",
                    "The loaded archive provides nearby weapon-related context, but no focused comparison isolates this row to one exact stat.", false);
        }
        return unidentified();
    }

    static String dlcEvidence(String profileName, DLCProfiles.Field f, String context) {
        return assessDlc(profileName,f,context).evidence;
    }

    static String baseEvidence(BaseResearchProfiles.Field f, String nearbySpecific, String context) {
        return assessBase(f,nearbySpecific,context).evidence;
    }

    static boolean isGenericWeaponFamilyLabel(String label) {
        if (weaponFamily(label)!=null) return true;
        String u=cleanGeneratedLabel(label).toUpperCase(Locale.ROOT);
        return u.startsWith("CHAFF") || u.startsWith("WEAPON_CORE") || u.startsWith("PREORDER");
    }

    static String weaponFamily(String label) {
        if (label==null) return null;
        String u=cleanGeneratedLabel(label).toUpperCase(Locale.ROOT);
        if (u.startsWith("ACCURACY")) return "Accuracy";
        if (u.startsWith("AMMO_CAPACITY")) return "Ammo Capacity";
        if (u.startsWith("DAMAGE")) return "Damage";
        if (u.startsWith("FIRE_PATTERN")) return "Fire Pattern";
        if (u.startsWith("RATE_OF_FIRE") || u.equals("RATE_OF_F")) return "Rate of Fire";
        if (u.startsWith("RECOIL")) return "Recoil";
        if (u.startsWith("RELOAD_SPEED")) return "Reload Speed";
        if (u.startsWith("SCOPE")) return "Scope";
        if (u.startsWith("SILENCER")) return "Silencer";
        if (u.startsWith("LASER")) return "Laser";
        if (u.startsWith("HOLO_SIGHT")) return "Holo Sight";
        if (u.startsWith("REFLEX")) return "Reflex Sight";
        if (u.startsWith("BURST")) return "Burst";
        // Chaff in DLC profiles has already been proven capable of naming an inventory-size byte.
        // Treat it as an untrusted generated hint rather than a semantic family.
        return null;
    }

    static String cleanGeneratedLabel(String label) {
        if (label==null) return "";
        String s=label.trim();
        s=s.replaceFirst("\\s+#\\d+$","");
        s=s.replaceFirst("\\s+\\[\\d+/\\d+\\]$","");
        return s.trim();
    }

    private static boolean isBaseUpgradeCostCandidate(BaseResearchProfiles.Field f, String family) {
        if (!UPGRADE_COST_FAMILIES.contains(family) || f.original==null || f.original.length!=2) return false;
        byte[] adam=f.reference("Adam 3.0");
        return adam!=null && !Arrays.equals(adam,f.original) && allZero(adam);
    }

    private static boolean isDlcUpgradeCostCandidate(DLCProfiles.Field f, String family) {
        if (!UPGRADE_COST_FAMILIES.contains(family) || f.original==null || f.original.length!=2) return false;
        return dlcUpgradeCostPatterns().contains(family+"|"+hex(f.original));
    }

    private static synchronized Set<String> dlcUpgradeCostPatterns() {
        if (dlcUpgradeCostPatterns!=null) return dlcUpgradeCostPatterns;
        HashSet<String> out=new HashSet<>();
        BaseResearchProfiles.Profile base=BaseResearchProfiles.get();
        if (base!=null) for (BaseResearchProfiles.Field b:base.fields) {
            String family=weaponFamily(b.label);
            if (!UPGRADE_COST_FAMILIES.contains(family) || b.original==null || b.original.length!=2) continue;
            byte[] adam=b.reference("Adam 3.0");
            if (adam!=null && !Arrays.equals(adam,b.original) && allZero(adam)) out.add(family+"|"+hex(b.original));
        }
        dlcUpgradeCostPatterns=Collections.unmodifiableSet(out);
        return dlcUpgradeCostPatterns;
    }

    private static String baseConfirmedEvidence(BaseResearchProfiles.Field f) {
        if (f.offset==4937661L) return "Confirmed Tranquilizer Rifle magazine capacity; exposed as a normal Weapon Stats control and isolated by focused comparison evidence.";
        if (f.offset==4981373L) return "Confirmed Lancer Rifle magazine capacity; exposed as a normal Weapon Stats control and isolated by focused comparison evidence.";
        return "Confirmed editor mapping. This row belongs to the established normal-editor dataset rather than a generated weapon-family guess.";
    }

    private static String baseWeaponEvidence(BaseResearchProfiles.Field f, String family) {
        ArrayList<String> changed=new ArrayList<>();
        for (Map.Entry<String,byte[]> e:f.references.entrySet())
            if (e.getValue()!=null && !Arrays.equals(e.getValue(),f.original)) changed.add(e.getKey());
        if (changed.contains("Hardcore Normal") || changed.contains("Hardcore Optional")) {
            return "Changed by Hardcore Revival. That mod changes weapon damage, range, accuracy, reload time, recoil and attachment bonuses together, so the generated "+family+" family is useful context but does not isolate the exact sub-stat. Candidate: "+familyHint(family)+".";
        }
        return "The row sits in a generated "+family+" family, but no focused comparison isolates its exact sub-stat. Candidate: "+familyHint(family)+".";
    }

    private static String dlcFamilyEvidence(String profileName, DLCProfiles.Field f, String family) {
        return "The original Hardcore-derived profile places this row in the "+family+" family, but Hardcore changes many weapon properties at once. It is therefore a suspected component only. Candidate: "+familyHint(family)+".";
    }

    private static String friendlyFamilyComponent(String label) {
        String family=weaponFamily(label);
        if (family==null) return cleanGeneratedLabel(label);
        return seriesAndComponent(label)+" — "+familyHint(family);
    }

    private static String seriesAndComponent(String label) {
        String base=cleanGeneratedLabel(label).replace('_',' ');
        int component=1;
        if (label!=null) {
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("#(\\d+)\\s*$").matcher(label);
            if (m.find()) component=Integer.parseInt(m.group(1));
        }
        return titleCase(base)+" component "+component;
    }

    private static String familyHint(String family) {
        if (family.equals("Accuracy")) return "horizontal / vertical / spread candidate";
        if (family.equals("Damage")) return "base damage / damage falloff / range component candidate";
        if (family.equals("Rate of Fire")) return "fire-rate / burst-delay / timing candidate";
        if (family.equals("Recoil")) return "horizontal / vertical / recovery candidate";
        if (family.equals("Reload Speed")) return "reload timing / multiplier candidate";
        if (family.equals("Fire Pattern")) return "firing-mode / burst-pattern candidate";
        if (family.equals("Ammo Capacity")) return "magazine / capacity-upgrade component candidate";
        if (family.equals("Scope")) return "zoom / accuracy-bonus component candidate";
        if (family.equals("Silencer")) return "noise / damage-penalty component candidate";
        if (family.equals("Laser")) return "accuracy / spread-bonus component candidate";
        if (family.equals("Holo Sight")) return "zoom / accuracy modifier candidate";
        if (family.equals("Reflex Sight")) return "sight modifier / upgrade component candidate";
        if (family.equals("Burst")) return "burst-count / burst-delay component candidate";
        return "unresolved component";
    }

    private static String dlcWeapon(String profileName) {
        String p=shortProfile(profileName);
        if (p.equals("Assault")) return "Elite Battle Rifle";
        if (p.equals("Classic")) return "Elite Revolver";
        if (p.equals("Enforcer")) return "Elite Combat Rifle";
        if (p.equals("Intruder")) return "Elite Pistol";
        if (p.equals("Tactical")) return "Elite Tranquilizer Rifle";
        return "DLC Weapon";
    }

    private static String shortProfile(String profileName) {
        if (profileName==null) return "";
        String n=profileName.replace("DLCPack","").replace(".layer.0.all.archive","");
        return n;
    }

    private static boolean looksWeaponContext(String context) {
        if (context==null || context.trim().isEmpty() || context.equals("No nearby readable identifier")) return false;
        String u=context.toUpperCase(Locale.ROOT);
        return u.contains("ACCURACY") || u.contains("AMMO") || u.contains("DAMAGE") || u.contains("FIRE_PATTERN") ||
                u.contains("RATE_OF_FIRE") || u.contains("RECOIL") || u.contains("RELOAD") || u.contains("SCOPE") ||
                u.contains("SILENCER") || u.contains("RIFLE") || u.contains("PISTOL") || u.contains("REVOLVER") || u.contains("TRANQUILIZER");
    }

    private static String hex(byte[] bytes) {
        StringBuilder out=new StringBuilder();
        for (byte b:bytes) out.append(String.format("%02X",b&255));
        return out.toString();
    }

    private static boolean allZero(byte[] bytes) {
        if (bytes==null || bytes.length==0) return false;
        for (byte b:bytes) if (b!=0) return false;
        return true;
    }

    private static String titleCase(String text) {
        StringBuilder out=new StringBuilder(); boolean cap=true;
        for (int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if (c==' ' || c=='-' || c=='/') { out.append(c); cap=true; }
            else { out.append(cap?Character.toUpperCase(c):Character.toLowerCase(c)); cap=false; }
        }
        return out.toString();
    }

    private static RawArchiveAuditCatalog.Mapping rawBaseMapping(BaseResearchProfiles.Field f) {
        if (f==null || f.original==null) return null;
        return RawArchiveAuditCatalog.mapping("Game.layer.1.all.archive",f.offset,f.original.length);
    }

    private static RawArchiveAuditCatalog.Mapping rawDlcMapping(String profileName,DLCProfiles.Field f) {
        if (f==null || f.original==null || profileName==null) return null;
        return RawArchiveAuditCatalog.mapping(profileName,f.offset,f.original.length);
    }

    private static Assessment rawAssessment(RawArchiveAuditCatalog.Mapping raw) {
        Confidence confidence;
        if (raw.level==RawArchiveAuditCatalog.Level.CONFIRMED) confidence=Confidence.CONFIRMED;
        else if (raw.level==RawArchiveAuditCatalog.Level.STRONG_SUSPECTED) confidence=Confidence.STRONG_SUSPECTED;
        else confidence=Confidence.SUSPECTED;
        String evidence=raw.evidence+" "+RawArchiveAuditCatalog.evidenceSummary(raw.archive,raw.offset,raw.length());
        return new Assessment(confidence,raw.name,evidence,raw.saveRisk);
    }

    private static Assessment unidentified() {
        return new Assessment(Confidence.UNIDENTIFIED, "Unidentified", "No focused comparison or trustworthy structural evidence identifies this row yet.", false);
    }
}
