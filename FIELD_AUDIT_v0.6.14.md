# DXMD Archive Editor Pro — Field Confidence Audit v0.6.14

This document records the confidence rules used by v0.6.14 for **Base Fields** and **DLC Fields**.

The main correction is simple: a nearby or generated label such as `DAMAGE_01`, `ACCURACY_01`, `RATE_OF_FIRE_02`, or `RECOIL_01` identifies a **record family / research region**, not every individual byte inside that region. Multiple changed bytes around one readable name can represent the actual stat, horizontal/vertical components, falloff/range, timing values, upgrade metadata, Weapon Parts costs, display values, flags, or unrelated neighboring data.

## Confidence levels

### Confirmed

Used only when a focused comparison or an already-established editor mapping isolates the field's behavior well enough to expose it as fact.

### Strong suspected

There is specific comparison or structural evidence for a narrow role, but not enough independent isolation to call the exact byte semantics confirmed.

### Suspected

The row is clearly associated with a weapon/stat/augmentation family, but available comparisons change too many things at once to isolate one exact sub-stat.

### Unidentified

No defensible role is currently supported.

## Dataset counts

### Base archive

- Total research rows: **2,553**
- Confirmed mappings: **217**
- Strong-suspected weapon-upgrade cost candidates: **23**
- The confirmed Base mappings are primarily established XP/reward, inventory, economy/crafting and player controls.
- Only **2 Base rows are confirmed direct weapon-stat mappings**:
  - `4937661` — Tranquilizer Rifle Magazine Capacity
  - `4981373` — Lancer Rifle Magazine Capacity

Repeated Base `DAMAGE_*`, `ACCURACY_*`, `RATE_OF_FIRE_*`, `RECOIL_*`, `RELOAD_SPEED_*`, `SCOPE_*`, `SILENCER_*`, attachment and similar research rows are **not** promoted to Confirmed by their generated labels.

### DLC archives

- Total research rows: **342** across five DLC packs
- Confirmed mappings: **3**
- Strong-suspected mappings/candidates before runtime context promotion: **47**
- Remaining rows stay Suspected or Unidentified. Archive-derived nearby context can improve a description but can never promote a row to Confirmed by proximity alone.

## Confirmed DLC mappings

### Enforcer — Elite Combat Rifle

`31578472` — **Elite Combat Rifle Inventory Grid Control**

Evidence:

- clean value `05`
- Master Inventory changes the relevant Elite Combat Rifle grid from 5-wide to 3-wide and changes this byte `05 -> 03`
- I Need The Edge comparison changes it `05 -> 03`
- Favored+ changes it `05 -> 02`
- IPOAO changes it `05 -> 01`

The generated profile label at this position was `AMMO_CAPACITY_03 #3`, demonstrating why generated semantic labels cannot be trusted as exact identities.

### Tactical — Elite Tranquilizer Rifle

`106868` — **Elite Tranquilizer Rifle Inventory Grid Control**

Evidence:

- Master Inventory, I Need The Edge variants and IPOAO all alter this same byte while changing the Elite Tranquilizer inventory grid.
- The effect is isolated as an inventory-grid control even though the generated profile called the row only `Raw field #8`.

`107948` — **Elite Tranquilizer Rifle Magazine Capacity**

Evidence:

- clean value `0A` = 10
- I Need The Edge v1.1/v1.2 explicitly documents changing the Elite Tranquilizer Rifle magazine from **10 to 4**
- the comparison changes this byte exactly `0A -> 04`

## Strong-suspected DLC examples

### Assault — Elite Battle Rifle

`54331` — **Strong suspected: Elite Battle Rifle Inventory Grid Control**

I Need The Edge variants change this byte `05 -> 03` and `05 -> 02` while optional modules resize Battle/Elite weapon inventory grids. It remains Strong suspected because a second independent focused comparison does not isolate this specific Elite Battle Rifle row.

### Enforcer / Intruder suppressor controls

- Enforcer `31593122`
- Intruder `14844588`

Both are **Strong suspected suppressor damage-penalty controls**. I Need The Edge v1.2 explicitly removes suppressor damage debuffs and changes these `Silencer0` bytes `40 -> A6`. They remain Strong suspected because the exact internal byte semantics have not been independently decoded.

Enforcer `31593408` changes beside the suppressor row and remains a **Suspected suppressor companion field** rather than being assigned the same identity.

### Tactical Micro-Assembler candidates

- `107324`
- `108070`

These are retained as **Suspected Micro-Assembler / augmentation controls**. The Micro-Assembler comparison changes them, but that reference archive also changes known weapon inventory/capacity bytes. The archive is therefore not clean enough to confirm these two fields by itself.

## Weapon Parts upgrade-cost candidates

Adam 3.0 explicitly removes the Weapon Parts required to upgrade weapons. In Base Fields, a recurring pattern appears inside Accuracy, Damage, Rate of Fire, Recoil, Fire Pattern, Ammo Capacity and Burst families:

- the row is a short **2-byte** value
- Hardcore changes it along with the surrounding weapon family
- Adam 3.0 changes that short value to **zero**

These rows are now labeled **Strong suspected: ... Upgrade Parts Cost**, rather than being displayed as another confirmed Accuracy/Damage/etc. value.

The same two-byte family/value patterns recur in DLC weapon records. Matching DLC rows inherit only the **Strong suspected upgrade-cost candidate** classification; they are not promoted to Confirmed.

## Why Hardcore Revival is supporting evidence, not proof

Hardcore Revival changes multiple weapon properties together, including damage, range, accuracy, reload time, recoil and weapon-mod bonuses/maluses. A byte changed by Hardcore therefore proves that the byte belongs to a modified region, but does **not** by itself prove that the byte is specifically Damage, Accuracy, Recoil, etc.

v0.6.14 still uses Hardcore to discover and group research regions, but exact Attribute names remain Suspected unless a narrower comparison isolates them.

## Candidate sub-stat descriptions

When exact semantics are unknown, v0.6.14 describes plausible roles without presenting them as fact:

- Accuracy — horizontal / vertical / spread candidate
- Damage — base damage / damage falloff / range component candidate
- Rate of Fire — fire-rate / burst-delay / timing candidate
- Recoil — horizontal / vertical / recovery candidate
- Reload Speed — reload timing / multiplier candidate
- Fire Pattern — firing-mode / burst-pattern candidate
- Ammo Capacity — magazine / capacity-upgrade component candidate
- Scope — zoom / accuracy-bonus component candidate
- Silencer — noise / damage-penalty component candidate
- Laser — accuracy / spread-bonus component candidate
- Holo / Reflex — sight modifier / accuracy / zoom candidate
- Burst — burst-count / burst-delay candidate

These are deliberately phrased as candidates until further controlled comparisons identify the individual values.

## Restore behavior

The confidence audit also controls selective restore:

- **Restore Editor Fields to Original** restores Confirmed mappings only.
- Strong-suspected, Suspected and Unidentified research bytes are deliberately left untouched.
- **Restore .bak** remains the exact whole-file recovery path for experimental research edits.

This prevents a research label from becoming write authority simply because it happened to look like a known stat.

## Future promotion rule

A field should only move to Confirmed when at least one of the following is available:

1. a focused mod changes one documented gameplay property and isolates the same byte/value;
2. two independent focused comparisons converge on the same field behavior;
3. direct in-game controlled testing verifies the field while neighboring candidates are held constant;
4. a trustworthy archive structure / code reference directly establishes the field semantics.

Until then, keeping a field Suspected is preferable to publishing a confident but incorrect mapping.
