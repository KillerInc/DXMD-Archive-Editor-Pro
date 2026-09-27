# DXMD Archive Editor Pro v0.6.15

## Full raw-archive audit

This release integrates the complete supplied clean/original + modded archive dataset directly into the Base and DLC research models instead of relying only on the older reconstructed comparison tables.

- 84 archive files audited across Base and all five DLC packs.
- 78 physical modded-vs-original comparisons.
- 48 deduplicated supplied comparison variants retained as derived evidence: 47 modified plus one exact-original control.
- 7,222 changed byte-runs available to the confidence/evidence layer.
- Raw comparison variants are now available in the Base/DLC research Compare lists.
- No game or mod archives are redistributed; the repository stores derived hashes, offsets and deltas only.

## Corrections and new confirmations

- Corrects the `4D C7 1C 10` interpretation: it is a sniper/standard-reticle-related function, not the built-in silencer ID. Silence To The Guns replaces it with `26 AC CD 27`, the silencer function. The affected controls are now named **Standard-Reticle Function Slot / Silencer Override**.
- Confirms the Elite Battle Rifle inventory-grid width from independent raw comparisons (`05→03`, with Favored Elites Plus `→02`).
- Adds focused Base confirmations for the Icarus Reflexes takedown-power control, regeneration/energy controls, Grenade Launcher ammo heights, Experimental Augmentation gates, bolt-action overrides, silencer-enable toggles and reticle/silencer override slots.
- Adds focused DLC confirmations from Silence To The Guns and the Micro-Assembler gate pattern while retaining contaminated Tactical bytes as suspected.
- Keeps suppressor damage-debuff pairs strong-suspected: their effect family is isolated, but their exact A/B internal sub-roles are not independently decoded.

## Audited confidence totals

- Base: 2,546 rows; 233 confirmed; 48 strong-suspected; 29 comparison references.
- DLC: 350 rows; 11 confirmed; 48 strong-suspected.
- Confirmed DLC by pack: Assault 2, Classic 1, Enforcer 2, Intruder 1, Tactical 5.

## Restore safety

Selective restore uses the same raw-audit confidence model: confirmed rows restore to embedded clean values, while strong-suspected, suspected and unidentified research bytes remain untouched. `.bak` remains the exact whole-file recovery option.

See `FIELD_AUDIT_v0.6.15.md` for the evidence rules and correction details.

## Requirements

Java 11 or newer. Keep game archives and saves backed up when testing research fields, especially inventory-dimension controls.
