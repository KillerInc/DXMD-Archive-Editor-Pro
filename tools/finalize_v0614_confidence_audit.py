#!/usr/bin/env python3
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
def read(p): return (ROOT/p).read_text(encoding='utf-8')
def write(p,s): (ROOT/p).write_text(s,encoding='utf-8')
def sub_once(s,pattern,repl,label,flags=re.S):
    out,n=re.subn(pattern,repl,s,count=1,flags=flags)
    if n!=1: raise SystemExit(f'{label}: expected one match, got {n}')
    return out

# Treat Chaff/Core/Preorder generated labels as untrusted context hints too.
p='src/main/java/FieldConfidenceAudit.java'; s=read(p)
s=s.replace('static boolean isGenericWeaponFamilyLabel(String label) { return weaponFamily(label)!=null; }', '''static boolean isGenericWeaponFamilyLabel(String label) {\n        if (weaponFamily(label)!=null) return true;\n        String u=cleanGeneratedLabel(label).toUpperCase(Locale.ROOT);\n        return u.startsWith("CHAFF") || u.startsWith("WEAPON_CORE") || u.startsWith("PREORDER");\n    }''')
write(p,s)

# Base archive context must use the strict confirmation rule, not raw category text.
p='src/main/java/ArchiveContextResolver.java'; s=read(p)
s=s.replace('if (field.category != null && field.category.startsWith("KNOWN")) {', 'if (FieldConfidenceAudit.isBaseConfirmed(field)) {')
s=s.replace('if (field.category != null && field.category.startsWith("KNOWN")) score += overlap * 20.0;', 'if (FieldConfidenceAudit.isBaseConfirmed(field)) score += overlap * 20.0;')
write(p,s)

# Identification exports now carry audited confidence/assessment instead of old semantic guesses.
p='src/main/java/FieldIdentificationIO.java'; s=read(p)
s=s.replace('''            for(BaseResearchProfiles.Field x:p.fields) w.println(String.join("\\t","BASE",safe(p.name),safe(baseFieldId(x)),Long.toString(x.offset),Integer.toString(x.original.length),BaseFieldsPanel.hex(x.original),BaseFieldsPanel.hex(x.current),safe(x.category),safe(x.label),safe(x.identifiedAs)));''', '''            for(BaseResearchProfiles.Field x:p.fields) {\n                FieldConfidenceAudit.Assessment a=FieldConfidenceAudit.assessBase(x,null,"");\n                w.println(String.join("\\t","BASE",safe(p.name),safe(baseFieldId(x)),Long.toString(x.offset),Integer.toString(x.original.length),BaseFieldsPanel.hex(x.original),BaseFieldsPanel.hex(x.current),safe(a.confidence.name()),safe(a.name+" | raw="+x.label),safe(x.identifiedAs)));\n            }''')
s=s.replace('''            for(DLCProfiles.Field x:p.fields) w.println(String.join("\\t","DLC",safe(profileName),safe(dlcFieldId(profileName,x)),Long.toString(x.offset),Integer.toString(x.original.length),DLCEditorPanel.hex(x.original),DLCEditorPanel.hex(x.current),safe(DLCEditorPanel.semanticGroup(x.label)),safe(x.label),safe(x.identifiedAs)));''', '''            for(DLCProfiles.Field x:p.fields) {\n                FieldConfidenceAudit.Assessment a=FieldConfidenceAudit.assessDlc(profileName,x,"");\n                w.println(String.join("\\t","DLC",safe(profileName),safe(dlcFieldId(profileName,x)),Long.toString(x.offset),Integer.toString(x.original.length),DLCEditorPanel.hex(x.original),DLCEditorPanel.hex(x.current),safe(a.confidence.name()),safe(a.name+" | raw="+x.label),safe(x.identifiedAs)));\n            }''')
s=s.replace('# DXMD Archive Editor Pro field-identification export v1','# DXMD Archive Editor Pro field-identification export v2')
s=s.replace('BuiltInGroup\\tBuiltInLabel','Confidence\\tBuiltInAssessment')
write(p,s)

# Clarify selective DLC restore and remove obsolete label-only semantic helpers.
p='src/main/java/DLCEditorPanel.java'; s=read(p)
s=s.replace('''void restoreOriginalFields(){int a=JOptionPane.showConfirmDialog(DLCEditorPanel.this,"Restore editor-supported fields in "+shortName(profileName)+" to clean-game values?","Restore editor fields",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);''', '''void restoreOriginalFields(){int a=JOptionPane.showConfirmDialog(DLCEditorPanel.this,"Restore confirmed editor fields in "+shortName(profileName)+" to clean-game values?\\n\\nSuspected/unidentified research bytes are left unchanged. Use Restore .bak to undo experimental research edits.","Restore editor fields",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);''')
s=re.sub(r'\n        String weaponName\(\)\{[^\n]*\}\n', '\n', s, count=1)
s=sub_once(s, r'\n    static boolean isInventoryDimensionLabel\(String label\)\{.*?\n    static String hex\(byte\[] b\)', '\n    static String hex(byte[] b)', 'remove obsolete DLC semantic helpers')
write(p,s)

# DLC profiles receive the same range/length validation discipline as Base Fields.
p='src/main/java/DLCProfiles.java'; s=read(p)
s=s.replace('''        }\n    }\n    static byte[] fromHex''', '''        }\n        validateAndSort();\n    }\n    private static void validateAndSort() throws IOException {\n        for(Profile p:profiles.values()){\n            p.fields.sort(Comparator.comparingLong(f->f.offset));\n            long previousEnd=-1;\n            for(Field f:p.fields){\n                if(f.original==null||f.original.length==0)throw new IOException("Zero-length DLC field in "+p.name+" at "+f.offset);\n                if(f.modded==null||f.modded.length!=f.original.length)throw new IOException("DLC comparison length mismatch in "+p.name+" at "+f.offset);\n                long end=f.offset+f.original.length;\n                if(f.offset<0||end>p.size)throw new IOException("DLC field outside archive bounds in "+p.name+" at "+f.offset);\n                if(f.offset<previousEnd)throw new IOException("Overlapping/out-of-order DLC fields in "+p.name+" near "+f.offset);\n                previousEnd=end;\n            }\n        }\n    }\n    static byte[] fromHex''')
write(p,s)

# Add static audit counts to README. Archive-derived context can promote some unknown rows
# to suspected at runtime, but confirmed/strong-suspected counts are fixed by evidence.
p='README.md'; s=read(p)
needle='## DLC Fields\n\n'
block='''### v0.6.14 confidence counts\n\nThe static evidence audit contains **2,553 Base research rows** and **342 DLC research rows**. Base has **217 confirmed mappings** (primarily established XP, economy, inventory and player controls) and **23 strong-suspected weapon-upgrade cost candidates**. Only **2 Base weapon-stat rows** are confirmed as direct weapon stats: the Tranquilizer Rifle and Lancer Rifle magazine capacities.\n\nAcross all five DLC packs, only **3 / 342 rows** currently meet the confirmed threshold. Another **47** are strong-suspected from focused or structural comparison evidence. Remaining rows stay suspected or unidentified; archive-derived context may improve their description but never promotes them to confirmed by proximity alone.\n\n'''
if '### v0.6.14 confidence counts' not in s: s=s.replace(needle,block+needle)
write(p,s)

print('v0.6.14 confidence audit finalization applied')
