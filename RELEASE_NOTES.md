# DXMD Archive Editor Pro v0.6.10

## Categorized base-game editing

- Replaced the old **Base Game** tab with dedicated edit categories: **Weapon Stats**, **Player Stats**, **Inventory Stats**, **Economy & Crafting**, and **XP Rewards**.
- Moved every confirmed normal base-game mapping into one of those categories.
- **Weapon Stats** includes confirmed ammunition controls as well as known weapon magazine fields.
- **Inventory Stats** contains confirmed item width/height controls and non-ammunition stack sizes.
- Consolidated all confirmed XP/reward controls in **XP Rewards** so they are no longer duplicated elsewhere.
- Renamed **Base Research** to **Base Fields** and moved it to the end beside **DLC Fields**.
- Renamed the DLC research area to **DLC Fields**.
- Removed the runtime **Weapon Reference** tab. DXMD weapon wiki/testing material remains research evidence for identifying fields, not application UI.

## Coverage audit

- Audited the categorized editor against the Base Fields database: all **222 confirmed base field records** are represented by a normal edit category.
- Current categorized controls resolve to **222 underlying archive addresses** with no cross-category overlap.
- Verified all categorized defaults against the clean 16,986,849-byte base archive: **0 mismatches**.
- The XP category contains **74 logical controls** backed by **134 archive addresses**; all clean defaults matched.
- Base Fields contains **2,553 research records** across the existing 12 comparison profiles.

## Code audit / safety fixes

- Normal edit tabs now track explicit user edits. Opening a tab or pressing Apply without changing anything does not rewrite grouped values in an already-modded archive.
- Selecting **Default Values** intentionally marks the category for restoration, including grouped controls whose first address already matched the default.
- Loading or choosing **Current File Values** clears the pending-write state.
- Inventory-dimension warnings cover width and height controls.
- Base archive name and size are validated before normal categorized controls are loaded.
- **Restore Editor Fields to Original** now restores only confirmed `KNOWN` base mappings; suspected/unidentified research bytes are deliberately left untouched.
- The v0.6.8 DLC Compare initialization fix remains included.

## Requirements

Java 11 or newer. Keep saves backed up when testing newly identified or inventory-dimension fields.
