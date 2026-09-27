# DXMD Archive Editor Pro

A Java/Swing archive-value editor and reverse-engineering tool for **Deus Ex: Mankind Divided**.

> **Current version:** v0.7.8  
> **Runtime:** Java 21 or newer  
> **Primary platform:** Windows

The project does **not** redistribute Deus Ex game archives.

See **[FIELD_AUDIT_v0.6.15.md](FIELD_AUDIT_v0.6.15.md)** for the current raw-archive evidence baseline. The v0.6.14 audit remains available as the previous baseline.

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

## Current tab layout

Confirmed base-game editing is organized by purpose instead of one large Base Game tab:

- **Weapon Stats** — confirmed weapon and ammunition controls, currently including mapped magazine capacities and ammunition stack sizes.
- **Player Stats** — energy regeneration, Biocell energy gain, takedown energy cost, and mapped experimental-augmentation behavior.
- **Inventory Stats** — confirmed item dimensions plus non-ammunition inventory stacks.
- **Economy & Crafting** — confirmed shop prices and Weapon Parts crafting costs.
- **XP Rewards** — all confirmed XP/reward editing in one place: hacking, passwords/keycodes, objectives, stealth, remote hacking, exploration, social/CASIE, combat and mechanical-target rewards.
- **Research Inspector** — unified Base + DLC reverse-engineering workspace with archive-resource/chunk mapping, raw byte context, multiple integer/Float16/Float32 interpretations, comparison evidence, editable candidate boundaries and User ID annotations.

The legacy **Base Fields** and **DLC Fields** research tabs were replaced in v0.7.1 by the unified Research Inspector. Confirmed editing stays in the five categorized tabs above.

The previous runtime **Weapon Reference** tab is also removed. DXMD weapon wiki pages, damage-testing material and supplied weapon-stat tables are used by the project as research evidence for identifying more archive fields, not as end-user application UI.

## v0.7.3 tab readability fix

v0.7.3 fixes dark/black text that could remain on top-level tabs under Nimbus. The theme now supplies explicit Nimbus text colors for enabled, selected, focused, hover and pressed tab states, and also forces per-tab foreground/background colors at runtime. Selected tabs use white text; inactive tabs use the light application text color.

## v0.7.8 compact Research Inspector list

The left side of the Research Inspector now keeps only **Status**, **Attribute**, and **User ID**. Archive/resource/HeaderLib/payload details remain in the selected-row inspector on the right, where they are useful without making the research list excessively wide. The list pane is also slightly narrower so the detailed inspector gets more room.

## v0.7.7 DLC HeaderLib completion

v0.7.7 adds the supplied DLC `pc_headerlib` set to the verified logical-resource catalog. All **350 / 350 DLC research rows** now resolve from archive offsets through their `.pc_resourcelib` to exact HeaderLib logical resources and payload-relative offsets. Combined with Base, logical-resource coverage is **2,896 / 2,896 current research rows**.

Tactical research is now split into its real structures, including the Tactical pack entity, MicroAssembler entity type, and separate player/NPC preorder tranquilizer-rifle templates. The mappings are reduced to a tiny immutable runtime catalog, so the application does not scan the uploaded HeaderLib archive at startup; the existing animated Loading Archive popup remains active during normal archive preparation.

## v0.7.6 HeaderLib logical-resource structure

v0.7.6 adds verified DXMD HeaderLib/BIN1 structure to the Research Inspector. The supplied `dxmd_dawn_extract.py` and pc_headerlib collection were used to map every current Base research row from flat archive offsets through the containing `.pc_resourcelib` into its exact logical resource payload. The inspector now shows logical assembly resource, HeaderLib, resource/owner IDs, flags, resource type/magic, payload length and offset within the logical resource. Candidate-boundary changes update the payload-relative offset as well.

All **2,546 / 2,546 Base research rows** and **350 / 350 DLC research rows** now resolve to verified logical resources: **2,896 / 2,896 current research rows total**. The DLC HeaderLib set added in v0.7.7 supplies the previously missing mappings for Assault, Classic, Enforcer, Intruder, and Tactical.

The animated Loading Archive popup remains active and now reports the HeaderLib logical-resource mapping stage.

## v0.7.5 animated loading / packaged download

v0.7.5 keeps the Loading Archive dialog visibly active during first-load work. The progress bar uses Swing's indeterminate animation while the background worker is running and the bar text also cycles `Working`, `Working.`, `Working..`, `Working...`; the stage label continues to report the current operation. When loading finishes, the bar switches to determinate mode and completes at 100%.

The preferred release download is now a ZIP containing the Java 21 JAR, a SHA-256 checksum file and verification instructions. The direct JAR remains available. This packaging can reduce browser false positives for uncommon standalone JAR downloads, but it is not a substitute for a trusted platform code-signing certificate.

## v0.7.4 loading/readability pass

v0.7.4 fixes the remaining dark-theme text that could still be painted black inside the confirmed-stat panels and adds a modal **Loading Archive** progress dialog for first-time archive preparation. Base archive directory parsing and readable-context scanning now happen in a `SwingWorker`, keeping the interface responsive while the progress dialog reports the current stage. Parsed archive-resource indexes are cached by path/size/modified-time and reused by the Research Inspector.

## v0.7.2 Dark research UI

v0.7.2 restyles the complete Swing application to match the Research Inspector mockup: dark navy work surfaces, teal/cyan selection accents, dark table headers and byte-inspector panes, subdued borders, and consistent controls across both the confirmed-stat tabs and Research Inspector. The application opens directly on Research Inspector while retaining Weapon Stats, Player Stats, Inventory Stats, Economy & Crafting and XP Rewards as normal confirmed-edit tabs.

The supplied Dawn/DXMD Python extractor was also tested against the clean archives as an independent structural cross-check. Its `ARCH` parser agrees with the Java archive index on the real container layout and supports linked archives, HeaderLib/BIN1 structures and resource-library reconstruction. It remains a research/reference tool; the released editor has no Python runtime dependency.

## v0.7.1 Research Inspector

v0.7.1 replaces the old flat Base Fields / DLC Fields research tables with a unified inspector. A changed-byte run is no longer assumed to define the real field boundary.

- Parses each loaded DXMD `ARCH` directory and maps archive offsets to the real internal `.pc_resourcelib`, chunk number and resource-local offset.
- Supports Base plus Assault, Classic, Enforcer, Intruder and Tactical DLC profiles from one archive selector.
- Shows raw bytes around the selected candidate region and decodes the same selection as little-endian unsigned/signed integers, Float16, Float32 and Float64 where width permits.
- Candidate start can be shifted ±8 bytes and width can be changed to 1/2/4/8 bytes. This exposes boundary mistakes such as a `80 3F` diff fragment that is actually the upper half of `00 00 80 3F` (`Float32 1.0`).
- Research edits are allowed through Hex, integer, Float16 or Float32 interpretations. Boundary-changing edits receive an explicit warning and preserve a `.bak` first.
- Existing confidence/evidence rules and comparison profiles remain visible. User ID annotations plus TSV import/export are retained.

The full supplied OG validation mapped **2,546 / 2,546 Base rows** and **350 / 350 DLC rows** to internal archive resources with zero unmapped research rows.

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

**Base Fields** contains confirmed mappings plus thousands of research candidates generated from clean/modded archive comparisons. It currently carries **2,546 non-overlapping field records** and **29 comparison references** after integrating the complete raw archive audit.

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

## v0.6.15 raw-archive audit

v0.6.15 compares the complete supplied clean/original archive set directly with every supplied modded archive. The derived evidence covers 84 archive files, 78 physical modded-vs-original comparisons, 48 SHA-256-deduplicated supplied variants and 7,222 changed byte-runs.

Current audited totals are **2,546 Base rows / 233 confirmed / 48 strong-suspected** and **350 DLC rows / 11 confirmed / 48 strong-suspected**. A key correction is that `4D C7 1C 10` is a sniper/standard-reticle-related function rather than the silencer ID; Silence To The Guns replaces it with `26 AC CD 27`, the built-in silencer function. See **[FIELD_AUDIT_v0.6.15.md](FIELD_AUDIT_v0.6.15.md)** for the evidence and promotion rules.

## v0.6.15 raw-archive field-confidence audit

v0.6.15 extends the confidence audit across every supplied raw OG/modded archive and applies the resulting focused mappings to both Base Fields and DLC Fields. Generated labels such as `DAMAGE_01`, `ACCURACY_01`, `RATE_OF_FIRE_02`, `RECOIL_01`, and numbered variants are structural research hints, not proof that every nearby byte is that exact stat.

- A repeated weapon-stat family is no longer promoted to confirmed simply because its generated label contains `Damage`, `Accuracy`, `Recoil`, etc.
- Weapon-family rows are shown as suspected components with narrower candidate roles such as horizontal/vertical/spread, base-damage/falloff/range, recoil axis/recovery, fire-rate/burst timing, reload timing, scope bonus, and suppressor penalty.
- Adam 3.0 rows that are short values changed to zero are called out as **strong suspected Weapon Parts upgrade-cost controls** when they match the documented removal of weapon-upgrade parts costs.
- DLC confirmation now requires focused cross-mod isolation. The full raw audit raises 11 DLC rows to confirmed where independent or focused comparisons isolate the effect, including the Elite Battle Rifle grid width, silencer/reticle function slots, and three Micro-Assembler experimental/overclock markers.
- Focused comparisons from I Need The Edge and Master Inventory are used to identify inventory-grid/capacity controls; broad Hardcore Revival changes remain supporting evidence only because Hardcore changes many weapon properties together.
- Evidence for each assessment is available as a table-cell tooltip. Strong-suspected rows use a separate visual state from confirmed green rows.
- **Restore Editor Fields to Original** now follows the same strict confidence policy. DLC research/suspected rows are not silently rewritten by selective restore; use the exact `.bak` restore to undo experimental research edits.

### v0.6.14 confidence counts (historical baseline)

The static evidence audit contains **2,553 Base research rows** and **342 DLC research rows**. Base has **217 confirmed mappings** (primarily established XP, economy, inventory and player controls) and **23 strong-suspected weapon-upgrade cost candidates**. Only **2 Base weapon-stat rows** are confirmed as direct weapon stats: the Tranquilizer Rifle and Lancer Rifle magazine capacities.

Across all five DLC packs, only **3 / 342 rows** currently meet the confirmed threshold. Another **47** are strong-suspected from focused or structural comparison evidence. Remaining rows stay suspected or unidentified; archive-derived context may improve their description but never promotes them to confirmed by proximity alone.

## v0.6.16 editable Float16 research view

Base Fields and DLC Fields now include an editable **Float16 (LE)** column immediately beside **Current decimal**. It is enabled only for exact 2-byte fields. The column interprets the two archive bytes as IEEE-754 binary16 in little-endian order; editing it writes the corresponding two-byte half-float representation and immediately updates the decimal/hex views. For example, `2.25` encodes to `80 40`. This is an alternate numeric interpretation for research, not a claim that every 2-byte field is a float.

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

The Research Inspector can export/import Base and DLC User ID annotations as TSV. Stable field IDs, offsets, original/current hex, built-in labels and the User ID are included so discoveries can be shared and promoted later when confirmed.

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

The source targets Java 21 with `javac --release 21`. The resulting JAR is written to:

```text
dist/DXMD-Archive-Editor-Pro-v0.7.8.jar
```

## Credits

**LightPower1** — creator of the original **DXMD Archive File Editor Tool** (Nexus Mods #18), whose Java editor established the starting implementation this project grew from.

Special thanks to **MohamedASalama** and **Grognougnou** for their Deus Ex: Mankind Divided archive-editing work; they were also credited by the original DXMD Archive File Editor Tool author.

Referenced community mods remain the work of their respective authors. DXMD Archive Editor Pro uses research observations and byte-level comparisons and does not redistribute original game archives.

## Disclaimer

DXMD Archive Editor Pro is a community tool and is not affiliated with or endorsed by Eidos-Montréal or Square Enix. Editing game archives can break an installation or save data. Keep backups when testing changes, especially newly identified or inventory-dimension fields.
