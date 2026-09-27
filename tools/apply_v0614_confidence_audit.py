#!/usr/bin/env python3
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]

def read(rel): return (ROOT/rel).read_text(encoding="utf-8")
def write(rel,text): (ROOT/rel).write_text(text,encoding="utf-8")

def sub_once(text, pattern, repl, label, flags=re.S):
    new,n=re.subn(pattern,repl,text,count=1,flags=flags)
    if n!=1: raise SystemExit(f"{label}: expected one match, got {n}")
    return new

# Base Fields: centralize confirmation and weapon-family confidence.
p="src/main/java/BaseFieldsPanel.java"
s=read(p)
s=s.replace('JLabel known=new JLabel("Known / confirmed field"); known.setForeground(new Color(0,128,0));',
'''JLabel known=new JLabel("Confirmed / isolated field"); known.setForeground(new Color(0,128,0));\n        JLabel strong=new JLabel("Strong suspected"); strong.setForeground(new Color(70,100,180));''')
s=s.replace('p.add(known); p.add(new JLabel("|")); p.add(modified);',
'''p.add(known); p.add(new JLabel("|")); p.add(strong); p.add(new JLabel("|")); p.add(modified);''')
s=sub_once(s,
 r'    private boolean isDangerous\(BaseResearchProfiles\.Field f\)\{.*?\n    \}\n\n    private String nearbyContext',
'''    private boolean isDangerous(BaseResearchProfiles.Field f){\n        return FieldConfidenceAudit.isBaseSaveRisk(f);\n    }\n\n    private String nearbyContext''','Base isDangerous')
s=sub_once(s,
 r'    private String attributeName\(int row\)\{.*?\n    \}\n\n    private void autoSizeColumns',
'''    private String attributeName(int row){\n        BaseResearchProfiles.Field f=model.profile.fields.get(row);\n        FieldConfidenceAudit.Assessment a=FieldConfidenceAudit.assessBase(f,nearbySpecificName(row),nearbyContext(row));\n        if(a.saveRisk) return "WARNING — CAN BREAK SAVES: "+a.name;\n        return a.name;\n    }\n\n    private void autoSizeColumns''','Base attributeName')
s=sub_once(s,
 r'            boolean modified=f!=null&&!Arrays\.equals\(f\.current,f\.original\);\n            boolean known=f!=null&&f\.category!=null&&f\.category\.startsWith\("KNOWN"\);\n            boolean danger=isDangerous\(f\);\n            Color stateColor=modified\?Color\.RED:\(danger\?new Color\(180,90,0\):\(known\?new Color\(0,128,0\):null\)\);',
'''            boolean modified=f!=null&&!Arrays.equals(f.current,f.original);\n            FieldConfidenceAudit.Assessment assessment=f==null?null:FieldConfidenceAudit.assessBase(f,nearbySpecificName(row),nearbyContext(row));\n            boolean known=assessment!=null&&assessment.confidence==FieldConfidenceAudit.Confidence.CONFIRMED;\n            boolean strong=assessment!=null&&assessment.confidence==FieldConfidenceAudit.Confidence.STRONG_SUSPECTED;\n            boolean danger=assessment!=null&&assessment.saveRisk;\n            setToolTipText(assessment==null?null:assessment.evidence);\n            Color stateColor=modified?Color.RED:(danger?new Color(180,90,0):(known?new Color(0,128,0):(strong?new Color(70,100,180):null)));''','Base renderer confidence')
write(p,s)

# DLC Fields: exact confirmed whitelist; all generated stat-family rows are research hints.
p="src/main/java/DLCEditorPanel.java"
s=read(p)
s=s.replace('JLabel known=new JLabel("Known / identified field"); known.setForeground(new Color(0,128,0));',
'''JLabel known=new JLabel("Confirmed / isolated field"); known.setForeground(new Color(0,128,0));\n        JLabel strong=new JLabel("Strong suspected"); strong.setForeground(new Color(70,100,180));''')
s=s.replace('p.add(known); p.add(new JLabel("|")); p.add(modified);',
'''p.add(known); p.add(new JLabel("|")); p.add(strong); p.add(new JLabel("|")); p.add(modified);''')
s=sub_once(s,
 r'        boolean isDangerous\(DLCProfiles\.Field f\)\{.*?\}\n\n        String nearbyContext',
'''        boolean isDangerous(DLCProfiles.Field f){return FieldConfidenceAudit.isDlcSaveRisk(profileName,f);}\n\n        String nearbyContext''','DLC isDangerous')
s=sub_once(s,
 r'        String attributeFor\(DLCProfiles\.Field target\)\{.*?\n        \}\n\n        void autoSizeColumns',
'''        String attributeFor(DLCProfiles.Field target){\n            int row=profile.fields.indexOf(target);\n            String context=row>=0?nearbyContext(row):"";\n            FieldConfidenceAudit.Assessment a=FieldConfidenceAudit.assessDlc(profileName,target,context);\n            if(a.saveRisk) return "WARNING — CAN BREAK SAVES: "+a.name;\n            return a.name;\n        }\n\n        void autoSizeColumns''','DLC attributeFor')
s=sub_once(s,
 r'                boolean modified=!Arrays\.equals\(f\.current,f\.original\);boolean known=isConfirmedFieldLabel\(f\.label\);boolean danger=isDangerous\(f\);\n                Color stateColor=modified\?Color\.RED:\(danger\?new Color\(180,90,0\):\(known\?new Color\(0,128,0\):null\)\);',
'''                boolean modified=!Arrays.equals(f.current,f.original);\n                FieldConfidenceAudit.Assessment assessment=FieldConfidenceAudit.assessDlc(profileName,f,nearbyContext(mr));\n                boolean known=assessment.confidence==FieldConfidenceAudit.Confidence.CONFIRMED;\n                boolean strong=assessment.confidence==FieldConfidenceAudit.Confidence.STRONG_SUSPECTED;\n                boolean danger=assessment.saveRisk;\n                setToolTipText(assessment.evidence);\n                Color stateColor=modified?Color.RED:(danger?new Color(180,90,0):(known?new Color(0,128,0):(strong?new Color(70,100,180):null)));''','DLC renderer confidence')
# Remove the old label-only confirmation function so it cannot accidentally be reused.
s=sub_once(s,
 r'\n    static boolean isConfirmedFieldLabel\(String label\)\{.*?\n    \}\n\n    static boolean isInventoryDimensionLabel',
'''\n    static boolean isInventoryDimensionLabel''','remove old DLC confirmation')
write(p,s)

# DLC context resolver must use the strict profile+offset confidence policy and must not
# fall back to generated generic stat-family labels.
p="src/main/java/DLCArchiveContextResolver.java"
s=read(p)
s=s.replace('DLCEditorPanel.isConfirmedFieldLabel(field.label)', 'FieldConfidenceAudit.isDlcConfirmed(archive.getName(), field)')
s=s.replace('''        String base = legacyBase(field.label);\n        if (isGenericLegacy(base)) return "No nearby readable identifier";\n        return base;''',
'''        String base = legacyBase(field.label);\n        if (isGenericLegacy(base) || FieldConfidenceAudit.isGenericWeaponFamilyLabel(field.label)) return "No nearby readable identifier";\n        return base;''')
s=s.replace('''        String base = legacyBase(label);\n        if (isGenericLegacy(base)) return "No nearby readable identifier";\n        return base;''',
'''        String base = legacyBase(label);\n        if (isGenericLegacy(base) || FieldConfidenceAudit.isGenericWeaponFamilyLabel(label)) return "No nearby readable identifier";\n        return base;''')
write(p,s)

# Selective restore follows the exact same confidence policy.
p="src/main/java/ArchiveRestore.java"
s=read(p)
s=s.replace('if (field.category == null || !field.category.startsWith("KNOWN")) continue;',
            'if (!FieldConfidenceAudit.isBaseConfirmed(field)) continue;')
s=s.replace('''            for (DLCProfiles.Field field : profile.fields) {\n                file.seek(field.offset);''',
'''            for (DLCProfiles.Field field : profile.fields) {\n                if (!FieldConfidenceAudit.isDlcConfirmed(profile.name, field)) continue;\n                file.seek(field.offset);''')
write(p,s)

# Version bump and build outputs.
p="src/main/java/Launcher.java"; s=read(p).replace('DXMD Archive Editor Pro v0.6.13','DXMD Archive Editor Pro v0.6.14'); write(p,s)
for p in ("build.sh","build.bat"):
    s=read(p).replace('v0.6.13','v0.6.14'); write(p,s)

# README: version, build path, and audit notes.
p="README.md"; s=read(p)
s=s.replace('**Current version:** v0.6.13','**Current version:** v0.6.14')
s=s.replace('## v0.6.13 tab layout','## v0.6.14 tab layout')
s=s.replace('dist/DXMD-Archive-Editor-Pro-v0.6.13.jar','dist/DXMD-Archive-Editor-Pro-v0.6.14.jar')
marker='## DLC Fields\n\n'
insert='''## v0.6.14 field-confidence audit\n\nv0.6.14 audits the meaning of **Known / confirmed** across both Base Fields and DLC Fields. Generated labels such as `DAMAGE_01`, `ACCURACY_01`, `RATE_OF_FIRE_02`, `RECOIL_01`, and numbered variants are structural research hints, not proof that every nearby byte is that exact stat.\n\n- A repeated weapon-stat family is no longer promoted to confirmed simply because its generated label contains `Damage`, `Accuracy`, `Recoil`, etc.\n- Weapon-family rows are shown as suspected components with narrower candidate roles such as horizontal/vertical/spread, base-damage/falloff/range, recoil axis/recovery, fire-rate/burst timing, reload timing, scope bonus, and suppressor penalty.\n- Adam 3.0 rows that are short values changed to zero are called out as **strong suspected Weapon Parts upgrade-cost controls** when they match the documented removal of weapon-upgrade parts costs.\n- DLC confirmation now requires focused cross-mod isolation. Only three DLC rows currently meet that bar: Elite Combat Rifle inventory-grid control, Elite Tranquilizer inventory-grid control, and Elite Tranquilizer magazine capacity.\n- Focused comparisons from I Need The Edge and Master Inventory are used to identify inventory-grid/capacity controls; broad Hardcore Revival changes remain supporting evidence only because Hardcore changes many weapon properties together.\n- Evidence for each assessment is available as a table-cell tooltip. Strong-suspected rows use a separate visual state from confirmed green rows.\n- **Restore Editor Fields to Original** now follows the same strict confidence policy. DLC research/suspected rows are not silently rewritten by selective restore; use the exact `.bak` restore to undo experimental research edits.\n\n'''
if '## v0.6.14 field-confidence audit' not in s:
    s=s.replace(marker,insert+marker)
write(p,s)

# Release notes are replaced with the focused audit notes.
write("RELEASE_NOTES.md",'''# DXMD Archive Editor Pro v0.6.14\n\n## Base + DLC field-confidence audit\n\nThis release rebuilds the confidence model used by **Base Fields** and **DLC Fields**.\n\nThe previous research profiles inherited broad nearby labels from clean-vs-modded comparisons. A block named `DAMAGE_01`, for example, may contain the actual damage value, damage falloff/range pieces, upgrade metadata, Weapon Parts costs, display values, or neighboring record data. Seeing six changed bytes near `DAMAGE_01` is not evidence for six separate confirmed Damage fields.\n\n### New confidence rules\n\n- **Confirmed** now means a focused comparison or established editor mapping isolates the field's effect.\n- Repeated `Accuracy`, `Damage`, `Rate of Fire`, `Recoil`, `Reload Speed`, `Scope`, `Silencer`, etc. rows are treated as **Suspected Weapon Stat components**, not confirmed copies of the same stat.\n- Candidate descriptions expose plausible sub-roles without pretending they are decoded: horizontal/vertical/spread accuracy, base damage/damage falloff/range, recoil axes/recovery, fire-rate/burst timing, reload timing, scope bonuses, suppressor penalties, and similar components.\n- Adam 3.0 provides a useful structural discriminator: short weapon-family values that it zeros while removing Weapon Parts upgrade costs are shown as **Strong suspected upgrade-parts-cost** controls rather than as the apparent stat-family name.\n- Hardcore Revival remains important evidence, but because it changes damage, range, accuracy, reload time, recoil and attachment bonuses together, Hardcore-only changes are not enough to confirm one exact sub-stat.\n\n### DLC audit\n\nThe old label-based DLC classifier could mark well over half the DLC research rows green. v0.6.14 removes that behavior.\n\nOnly three DLC rows currently meet the strict confirmed threshold:\n\n- **Elite Combat Rifle Inventory Grid Control** — isolated by Master Inventory plus I Need The Edge/IPOAO comparisons.\n- **Elite Tranquilizer Rifle Inventory Grid Control** — isolated by Master Inventory plus I Need The Edge/IPOAO comparisons.\n- **Elite Tranquilizer Rifle Magazine Capacity** — I Need The Edge documents 10→4 and the Tactical DLC byte changes exactly `0A→04`.\n\nAdditional rows are retained as strong-suspected where the evidence is useful but not precise enough for confirmed status, including the Elite Battle Rifle grid-control candidate and suppressor damage-penalty controls.\n\n### Base audit\n\nExisting established Base editor mappings remain confirmed. Broad generated weapon-stat families in Base Fields remain research data and now receive the same component/candidate treatment as DLC. The audit also identifies 2-byte weapon-family rows that Adam 3.0 zeros as strong upgrade-parts-cost candidates.\n\n### Restore safety\n\nSelective **Restore Editor Fields to Original** now uses the same strict confirmation test. It restores confirmed mappings only. Suspected/unidentified research bytes are intentionally left alone; exact `.bak` restore remains the recovery path for experimental research edits.\n\n## Requirements\n\nJava 11 or newer. Keep game archives and saves backed up when testing research fields, especially inventory-dimension controls.\n''')

print('v0.6.14 confidence audit patch applied')
