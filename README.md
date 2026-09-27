# DXMD Archive Editor Pro

A Java/Swing archive-value editor and reverse-engineering tool for **Deus Ex: Mankind Divided**.

> **Current version:** v0.6.13  
> **Runtime:** Java 11 or newer  
> **Primary platform:** Windows

The project does **not** redistribute Deus Ex game archives.

## Supported archives

Base game:

```text
runtime/Game.layer.1.all.archive
```

Supported installed DLC packs:

```text
DLC/runtime/DLCPackAssault.layer.0.all.archive
DLC/runtime/DLCPackClassic.layer.0.all.archive
DLC/runtime/DLCPackEnforcer.layer.0.all.archive
DLC/runtime/DLCPackIntruder.layer.0.all.archive
DLC/runtime/DLCPackTactical.layer.0.all.archive
```

Select `DXMD.exe` and the editor resolves the expected base/DLC structure automatically. Startup detection only walks upward through the application path and validates the DXMD directory layout; it does not scan arbitrary nearby folders.

## v0.6.13 tab layout

Confirmed base-game editing is organized by purpose instead of one large Base Game tab:

- **Weapon Stats** — confirmed weapon and ammunition controls, currently including mapped magazine capacities and ammunition stack sizes.
- **Player Stats** — energy regeneration, Biocell energy gain, takedown energy cost, and mapped experimental-augmentation behavior.
- **Inventory Stats** — confirmed item dimensions plus non-ammunition inventory stacks.
- **Economy & Crafting** — confirmed shop prices and Weapon Parts crafting costs.
- **XP Rewards** — all confirmed XP/reward editing in one place: hacking, passwords/keycodes, objectives, stealth, remote hacking, exploration, social/CASIE, combat and mechanical-target rewards.
- **Base Fields** — the full base-game diagnostic/research table.
- **DLC Fields** — installed DLC research/edit tabs with archive-derived nearby context.

**Base Fields** and **DLC Fields** are intentionally the last two top-level tabs. The old **Base Game** tab was removed after its confirmed edits were assigned to normal categories.

The previous runtime **Weapon Reference** tab is also removed. DXMD weapon wiki pages, damage-testing material and supplied weapon-stat tables are used by the project as research evidence for identifying more archive fields, not as end-user application UI.

## Normal edit behavior

The categorized tabs use the same straightforward editing style as the original base editor: current values, clean/default presets, descriptions and Apply controls.

The editor tracks explicit user edits. Merely opening a tab or pressing Apply does not rewrite every repeated address behind a grouped logical control. Choosing **Default Values** intentionally marks that category for restoration to its clean values.

Before writes, the editor preserves a `.bak` file if one does not already exist.

## XP Rewards

All confirmed XP edits are consolidated into **XP Rewards** rather than being duplicated across other tabs. Current confirmed/strongly established families include:

- Script Kiddie, Grey Hat, Black Hat, Network Adept, Master Hacker and First Try
- Access Granted, Free Admission, Open Sesame, Entering without Breaking and Master Felonist
- Ghost, Smooth Operator and Reset
- Getting Things Done and Completionist objective-reward records
- Paving the Way, Machina and Flawless
- Traveler, Explorer, Pathfinder, Trailblazer and Scholar
- Silver Tongue, Split Decision, Life Lesson, Spin Doctor, On the Fence, Read the Room, Stop the Press and Wait Your Turn
- Trooper, Veteran, Elite and the Marchenko-specific reward
- Merciful Soul, Marksman, Expedient, Multitasker, Shock Therapy, Surprise, Close Shave, Dust to Dust, Introvert, Juggernaut, Crash Landing, Piece by Piece, Sharpshooter, Chain Reaction, Master Blaster, Ring of Fire, Blown Away and Collateral Damage
- Scrap Metal, Void Warranty and Junk Yard

DXMD-specific wiki information is treated as strong high-level evidence and is cross-checked against archive-side evidence such as internal names, clean default values, repeated record layouts and controlled mod comparisons. DXHR-only information is not used to identify DXMD fields.

## Base Fields

**Base Fields** contains confirmed mappings plus thousands of research candidates generated from clean/modded archive comparisons. It currently carries **2,553 field records** across **12 comparison profiles**.

The table includes:

- Attribute Name
- Nearby context / field
- editable User ID
- editable Current Decimal
- Current Hex
- Original Hex
- up to three selected comparison profiles

Known mappings remain visible in Base Fields for verification, but normal editing of those values is organized in the categorized tabs above.

### Archive-order and context audit

v0.6.12 reworked the context presentation after auditing the Base Fields list against the clean base archive.

- Field ranges are sorted by physical archive offset and validated as unique/non-overlapping before the table is shown.
- Base Fields column sorting is disabled so the research list stays in physical archive order. Filtering hides rows but does not reorder them.
- The **Nearby context / field** column is derived conservatively from readable archive identifiers instead of treating every nearest string as an exact field name.
- Artificial duplicate suffixes such as `FIRE_PATTERN_01 #2` / `#3` are no longer presented as separate identities. Rows in the same nearby structure can share `FIRE_PATTERN_01` or a broader identifier-block span.
- Dense structures such as XP/reward records use verified internal archive names where mappings are known; uncertain rows remain contextual rather than being promoted to Known.
- If the archive does not provide a trustworthy readable identifier near a research row, the UI says so rather than displaying random printable data as a name.

The v0.6.9 decoded XP/reward overlay remains active in Base Fields. v0.6.12 restores both the overlay loader and the complete known-good compressed overlay resource after the v0.6.10 reorganization exposed an incomplete source-tree copy.

Comparison profiles currently include Hardcore Revival Normal/Optional, Adam 2.0, Adam 3.0, Tweaks, Master Inventory, No Health Regen variants, More Energy Regeneration Half/Full and Inventory Stacking.

## DLC Fields

Installed DLC packs are displayed as individual tabs. Missing packs are normal and are not created by the tool. DLC research views support original/current values, comparison profiles, presets, User ID import/export, backups and restore functions.

### DLC context audit in v0.6.13

v0.6.13 brings the DLC research table onto the same conservative context model used by Base Fields:

- **Nearby context / field** is resolved from readable identifiers in the actual loaded DLC archive instead of simply displaying the generated profile label.
- Artificial numbered identities such as `FIRE_PATTERN_01 #2` / `#3` are stripped from the visible context.
- Hex-like garbage and other weak printable strings are rejected as context.
- Arbitrary non-raw labels are no longer automatically treated as confirmed/green fields.
- The old nearest-semantic-field inference, which could spread one weapon-stat identity across unrelated rows within a large byte window, has been removed.
- Only explicitly mapped DLC field families receive confirmed treatment; uncertain rows remain suspected or unidentified.
- When no trustworthy nearby identifier exists, the table reports **No nearby readable identifier** rather than inventing an exact field identity.
- DLC rows remain in physical archive order and context is recalculated whenever the archive is reloaded.

The generated DLC labels are still retained internally as research hints and for compatibility with existing comparison/profile data; they are no longer treated as authoritative display identities.

The v0.6.8 fix that initializes DLC Compare selections before building comparison columns remains included.

## Safety and restore behavior

Inventory-dimension changes can be unsafe when an affected item already exists in a save. The editor warns before applying those changes. A new game, or dropping affected items and saving before changing dimensions, is the safer testing path.

Two restore mechanisms serve different purposes:

- **Restore `.bak`** replaces the whole archive with the exact pre-editor backup.
- **Restore Editor Fields to Original** writes only confirmed editor-supported mappings back to clean values. Suspected/unidentified research records are deliberately left untouched, as are unrelated mod bytes.

SHA-256 identities are informational only; custom/modded archives are not rejected solely because their hash is unknown.

## User field identifications

Base Fields and DLC Fields can export/import User ID annotations as TSV. Stable field IDs, offsets, original/current hex, built-in labels and the User ID are included so discoveries can be shared and promoted later when confirmed.

## Research status

The project deliberately distinguishes confirmed mappings from research candidates. Labels such as `Suspected: Weapon Stat — Unknown`, suspected health/consumable controls and `Unidentified` are not treated as established simply because a nearby published value happens to match.

Community archive comparisons used during research have included Hardcore Revival, Adam 2.0/3.0, Tweaks, Master Inventory, I Need The Edge, IPOAO, Silence To The Guns, Micro Assembler Overheat Fix, Icarus Reflexes, Mankind Redefined, No Health Regen, More Energy Regeneration and Inventory Stacking.

## Building

Windows:

```bat
build.bat
```

Linux/macOS:

```bash
./build.sh
```

The source targets Java 11 with `javac --release 11`. The resulting JAR is written to:

```text
dist/DXMD-Archive-Editor-Pro-v0.6.13.jar
```

## Credits

**LightPower1** — creator of the original **DXMD Archive File Editor Tool** (Nexus Mods #18), whose Java editor established the starting implementation this project grew from.

Special thanks to **MohamedASalama** and **Grognougnou** for their Deus Ex: Mankind Divided archive-editing work; they were also credited by the original DXMD Archive File Editor Tool author.

Referenced community mods remain the work of their respective authors. DXMD Archive Editor Pro uses research observations and byte-level comparisons and does not redistribute original game archives.

## Disclaimer

DXMD Archive Editor Pro is a community tool and is not affiliated with or endorsed by Eidos-Montréal or Square Enix. Editing game archives can break an installation or save data. Keep backups when testing changes, especially newly identified or inventory-dimension fields.
