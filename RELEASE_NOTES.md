# DXMD Archive Editor Pro v0.6.13

## DLC Fields context audit

This release applies the Base Fields context-audit approach to all supported DLC research tabs.

Previously, the DLC **Context / field** column largely displayed generated profile labels directly. That could make research hints look more authoritative than they really were and could produce misleading identities such as numbered duplicate labels or nearby strings that were never confirmed as exact field names.

v0.6.13 changes DLC context handling so the visible context is derived conservatively from the actual loaded DLC archive.

### What changed

- Added a dedicated `DLCArchiveContextResolver` that scans each loaded DLC archive for readable nearby identifiers.
- **Nearby context / field** now comes from the archive itself rather than simply echoing the generated profile label.
- Artificial duplicate suffixes such as `FIRE_PATTERN_01 #2` / `#3` are removed from visible context.
- Hex-like garbage and weak printable strings are rejected as field context.
- Arbitrary non-raw generated labels are no longer automatically treated as confirmed/green mappings.
- The old nearest-semantic-field inference was removed. A nearby known weapon-stat label is no longer allowed to spread a suspected identity across unrelated rows within a large byte window.
- Only explicitly mapped DLC field families receive confirmed treatment.
- Unknown rows remain **Unidentified** unless the archive provides defensible nearby context; weapon-like context may be shown only as **Suspected**.
- When no trustworthy identifier exists, the UI reports **No nearby readable identifier** rather than inventing a precise identity.
- DLC context is recalculated whenever the archive is reloaded.
- DLC rows remain in physical archive order and column sorting stays disabled.

Generated DLC profile labels are still retained internally as research hints and for comparison/profile compatibility. This update changes how confidence is presented to the user, not the underlying DLC research offsets or comparison bytes.

## Existing Base Fields audit

The v0.6.12 Base Fields context and continuity audit remains intact. Base Fields continues to derive nearby context conservatively from the loaded base archive, keep research rows in physical archive order, and avoid artificial numbered nearest-string identities.

## Existing editor features retained

- Weapon Stats, Player Stats, Inventory Stats, Economy & Crafting and XP Rewards categorized editing
- Base Fields and DLC Fields research tables
- up to three comparison profiles
- User ID import/export for sharing field identifications
- automatic `.bak` preservation before writes
- exact `.bak` restore
- selective **Restore Editor Fields to Original** behavior that leaves unrelated mod bytes alone
- inventory-dimension save-risk warnings
- automatic base-game and installed-DLC detection from `DXMD.exe`

## Requirements

Java 11 or newer. Keep saves and game archives backed up when testing newly identified or inventory-dimension fields.
