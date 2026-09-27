# DXMD Archive Editor Pro v0.6.14

## Base + DLC field-confidence audit

This release rebuilds the confidence model used by **Base Fields** and **DLC Fields**.

The previous research profiles inherited broad nearby labels from clean-vs-modded comparisons. A block named `DAMAGE_01`, for example, may contain the actual damage value, damage falloff/range pieces, upgrade metadata, Weapon Parts costs, display values, or neighboring record data. Seeing six changed bytes near `DAMAGE_01` is not evidence for six separate confirmed Damage fields.

### New confidence rules

- **Confirmed** now means a focused comparison or established editor mapping isolates the field's effect.
- Repeated `Accuracy`, `Damage`, `Rate of Fire`, `Recoil`, `Reload Speed`, `Scope`, `Silencer`, etc. rows are treated as **Suspected Weapon Stat components**, not confirmed copies of the same stat.
- Candidate descriptions expose plausible sub-roles without pretending they are decoded: horizontal/vertical/spread accuracy, base damage/damage falloff/range, recoil axes/recovery, fire-rate/burst timing, reload timing, scope bonuses, suppressor penalties, and similar components.
- Adam 3.0 provides a useful structural discriminator: short weapon-family values that it zeros while removing Weapon Parts upgrade costs are shown as **Strong suspected upgrade-parts-cost** controls rather than as the apparent stat-family name.
- Hardcore Revival remains important evidence, but because it changes damage, range, accuracy, reload time, recoil and attachment bonuses together, Hardcore-only changes are not enough to confirm one exact sub-stat.

### DLC audit

The old label-based DLC classifier could mark well over half the DLC research rows green. v0.6.14 removes that behavior.

Only three DLC rows currently meet the strict confirmed threshold:

- **Elite Combat Rifle Inventory Grid Control** — isolated by Master Inventory plus I Need The Edge/IPOAO comparisons.
- **Elite Tranquilizer Rifle Inventory Grid Control** — isolated by Master Inventory plus I Need The Edge/IPOAO comparisons.
- **Elite Tranquilizer Rifle Magazine Capacity** — I Need The Edge documents 10→4 and the Tactical DLC byte changes exactly `0A→04`.

Additional rows are retained as strong-suspected where the evidence is useful but not precise enough for confirmed status, including the Elite Battle Rifle grid-control candidate and suppressor damage-penalty controls.

### Base audit

Existing established Base editor mappings remain confirmed. Broad generated weapon-stat families in Base Fields remain research data and now receive the same component/candidate treatment as DLC. The audit also identifies 2-byte weapon-family rows that Adam 3.0 zeros as strong upgrade-parts-cost candidates.

### Restore safety

Selective **Restore Editor Fields to Original** now uses the same strict confirmation test. It restores confirmed mappings only. Suspected/unidentified research bytes are intentionally left alone; exact `.bak` restore remains the recovery path for experimental research edits.

## Requirements

Java 11 or newer. Keep game archives and saves backed up when testing research fields, especially inventory-dimension controls.
