#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def rw(p): return (ROOT/p).read_text(encoding='utf-8')
def ww(p,s): (ROOT/p).write_text(s,encoding='utf-8')

p='src/main/java/ArchiveContextResolver.java'; s=rw(p)
old='''        String base = legacyBase(f.label);\n        if (base.isEmpty() || base.toLowerCase(Locale.ROOT).startsWith("raw field"))\n            return "No nearby readable identifier";\n        return base;'''
new='''        String base = legacyBase(f.label);\n        if (base.isEmpty() || base.toLowerCase(Locale.ROOT).startsWith("raw field") || FieldConfidenceAudit.isGenericWeaponFamilyLabel(f.label))\n            return "No nearby readable identifier";\n        return base;'''
if old not in s: raise SystemExit('Base context fallback block not found')
s=s.replace(old,new,1); ww(p,s)

p='README.md'; s=rw(p)
needle='The project does **not** redistribute Deus Ex game archives.\n'
addition='''The project does **not** redistribute Deus Ex game archives.\n\nSee **[FIELD_AUDIT_v0.6.14.md](FIELD_AUDIT_v0.6.14.md)** for the current confirmed/suspected field-confidence methodology and evidence baseline.\n'''
if 'FIELD_AUDIT_v0.6.14.md' not in s: s=s.replace(needle,addition,1)
ww(p,s)

p='RELEASE_NOTES.md'; s=rw(p)
if 'FIELD_AUDIT_v0.6.14.md' not in s:
    s=s.replace('## Requirements', '## Audit documentation\n\nThe repository now includes `FIELD_AUDIT_v0.6.14.md`, documenting the confirmation threshold, fixed audit counts, confirmed DLC offsets, strong-suspected candidates, and promotion rules for future discoveries.\n\n## Requirements')
ww(p,s)
print('final v0.6.14 release prep applied')
