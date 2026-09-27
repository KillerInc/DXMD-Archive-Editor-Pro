# DXMD Archive Editor Pro v0.6.15

## Full raw-archive audit

v0.6.15 rebuilds the field-confidence baseline from the complete supplied clean + modded archive set instead of relying only on the earlier embedded comparison extracts.

- 84 archive files inspected: 6 clean OG archives and 78 modded archive copies.
- Exact SHA-256 deduplication produces 48 comparison variants: 47 modified variants plus one exact-OG control.
- 7,222 changed byte-runs were compared across Base and all five DLC packs.
- Broad or duplicated weapon-family labels remain suspected unless a focused comparison isolates the effect.

## Corrections and newly confirmed mappings

- Corrects `4D C7 1C 10`: the Silence To The Guns guide identifies it as a sniper/standard-reticle-related function. The mod replaces that slot with `26 AC CD 27`, the built-in silencer function. The editor no longer labels the original value itself as a silencer ID.
- Clarifies the Icarus Reflexes field at Base offset 7413173 as a **Takedown Power Consumption Control**, not a numeric energy-cost value.
- Confirms Elite Battle Rifle inventory width at Assault offset 54331 from independent inventory-focused comparisons.
- Confirms focused Silence To The Guns function/toggle mappings in Base and DLC weapon records.
- Confirms the three Tactical Micro-Assembler `0ACC4075` experimental/overclock gate markers while leaving contaminated neighboring Tactical changes suspected.
- Keeps suppressor damage-debuff pairs strong-suspected because the effect family is isolated but the exact A/B sub-role is not.

## Confidence baseline

Base Fields: 2,546 rows — 233 confirmed, 48 strong-suspected, 1,370 suspected, 895 unidentified.

DLC confirmed / strong-suspected:
- Assault: 2 / 8
- Classic: 1 / 10
- Enforcer: 2 / 12
- Intruder: 1 / 11
- Tactical: 5 / 7

## Restore safety

Selective **Restore Editor Fields to Original** uses the same confidence policy. Confirmed raw-audit mappings restore to clean bytes; strong-suspected, suspected and unidentified research bytes are left untouched. `.bak` remains the exact whole-file recovery path.

## Requirements

Java 11 or newer. Back up game archives and saves before testing archive edits, especially inventory-dimension changes.
