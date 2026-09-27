# DXMD Archive Editor Pro — Field Audit v0.6.15

## Authoritative input set

The audit used the complete supplied archive collection: clean Base + all five clean DLC archives and every supplied modded archive copy. Files were SHA-256 fingerprinted before comparison so duplicate copies did not inflate evidence.

- 84 archive files
- 6 clean OG archives
- 78 modded-vs-OG comparisons
- 48 deduplicated comparison variants
- 47 variants with actual byte changes
- 1 exact-OG control
- 7,222 changed byte-runs

The runtime editor stores the curated conclusions and clean archive fingerprints, not copyrighted game/mod archive payloads or the full raw diff matrix.

## Confidence rules

**Confirmed** requires a focused comparison, independent matching comparisons, explicit modding documentation tied to the changed value, or an already established editor mapping.

**Strong suspected** means the effect family is well isolated but the exact internal sub-role remains unresolved, or a strong structural pattern exists without direct isolation.

**Suspected** means useful contextual/comparison evidence exists but multiple interpretations remain plausible.

**Unidentified** means the current evidence does not justify a semantic label.

Nearby strings and generated families such as `DAMAGE_01`, `ACCURACY_01`, `RATE_OF_FIRE_02`, etc. never promote a row by themselves.

## Important corrections

### `4D C7 1C 10`

The supplied Silence To The Guns guide explicitly describes `4D C7 1C 10` as a sniper/standard-reticle-related function. The mod replaces that function slot with `26 AC CD 27`, which is the built-in silencer function. Therefore `4D C7 1C 10` must not itself be labeled as a built-in silencer ID.

### Takedown control

Base offset 7413173 (`22 C6 AB C4` -> zero in Icarus Reflexes) is confirmed as a takedown power-consumption control/identifier. It is distinct from the numeric takedown energy-cost float at offset 7413189.

### Elite Battle Rifle inventory width

Assault offset 54331 is confirmed as the Elite Battle Rifle inventory-grid width. Independent supplied archives make the same focused edit (OG byte 5 -> 3), while Favored Elites Plus changes the same byte to 2. A README describes vanilla width as 6; the clean archive byte is authoritative for the stored value.

### Micro-Assembler

Tactical offsets 112348, 113060 and 113820 are confirmed `0ACC4075` experimental/overclock gate markers. Other changes in the supplied Micro-Assembler archive are treated as contamination unless independently supported.

## v0.6.15 static baseline

Base: 2,546 rows — 233 confirmed / 48 strong-suspected / 1,370 suspected / 895 unidentified.

DLC:
- Assault: 52 rows — 2 confirmed / 8 strong-suspected
- Classic: 52 rows — 1 confirmed / 10 strong-suspected
- Enforcer: 72 rows — 2 confirmed / 12 strong-suspected
- Intruder: 90 rows — 1 confirmed / 11 strong-suspected
- Tactical: 84 rows — 5 confirmed / 7 strong-suspected

Selective restore is restricted to confirmed rows.
