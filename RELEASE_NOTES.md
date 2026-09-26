# DXMD Archive Editor Pro v0.6.12

## Base Fields continuity / context audit

This release rechecks how the **Nearby context / field** column is produced and how Base Fields rows are ordered.

- Base Fields rows are explicitly sorted by their physical archive offset and validated during profile loading.
- Column sorting is disabled in Base Fields so filtering cannot accidentally make the list look out of archive order.
- The profile validator rejects duplicate/overlapping or out-of-bounds field ranges and mismatched comparison-value lengths.
- Audited the current Base Fields dataset after the XP overlay: **2,553 field records**, strictly increasing archive order, **0 duplicate offsets** and **0 overlapping field ranges**.

## Nearby context improvements

The old context labels were largely inherited from a nearest-string heuristic. That produced misleading repeated suffixes such as `FIRE_PATTERN_01 #2`, `FIRE_PATTERN_01 #3`, long runs of `combat_xp_nonlethal #...`, and similar labels that looked like independently decoded fields when they were only nearby context.

v0.6.12 now derives the visible context conservatively from readable identifiers in the loaded archive:

- Numbered nearest-string suffixes are no longer presented as separate field identities.
- Repeated values around the same structure share a stable nearby identifier or identifier block instead of artificial `#2/#3/...` numbering.
- Example: the three research rows near `FIRE_PATTERN_01` show `FIRE_PATTERN_01` as their context rather than three fake numbered variants.
- For dense structures where many names occur together, the table can show a context span rather than claiming one exact name for every byte.
- Known DXMD XP/reward mappings use their verified internal archive names for context, including hacking, passwords, social/CASIE, remote hacking, exploration and combat reward families.
- When no trustworthy readable identifier is nearby, the table says so instead of displaying random printable bytes as a field name.

This changes the **context presentation**, not the underlying research offsets or comparison bytes.

## XP overlay / startup repair

During the v0.6.10 source reorganization, the v0.6.9 XP/reward overlay loader call was accidentally dropped. The first v0.6.11 build restored the call, which exposed that the source-tree copy of the compressed overlay resource was incomplete even though the known-good v0.6.9 JAR contained the complete data.

v0.6.12 restores the complete known-good overlay resource and keeps the decoded XP/reward records active in Base Fields. The release JAR is built from this corrected resource and is smoke-tested after packaging.

## Existing categorized editor

The normal editing layout remains:

- **Weapon Stats**
- **Player Stats**
- **Inventory Stats**
- **Economy & Crafting**
- **XP Rewards**
- **Base Fields**
- **DLC Fields**

The v0.6.8 DLC Compare initialization fix, backup/restore behavior, explicit-edit tracking and inventory-dimension save warnings remain included.

## Requirements

Java 11 or newer. Keep saves backed up when testing newly identified or inventory-dimension fields.
