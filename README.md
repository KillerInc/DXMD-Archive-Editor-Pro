# DXMD Archive Editor Pro

A Java/Swing archive-value editor and research tool for **Deus Ex: Mankind Divided**.

DXMD Archive Editor Pro is built around a simple goal: make known gameplay values easy to edit while also providing a safe research view for discovering and documenting additional values inside the game's `.archive` files.

> **Current version:** v0.6.9  
> **Runtime:** Java 11 or newer  
> **Platform:** Windows is the primary target. The editor itself is Java/Swing.

## What it edits

The editor currently works with the base-game archive:

```text
runtime/Game.layer.1.all.archive
```

and, when installed, these DLC archives:

```text
DLC/runtime/DLCPackAssault.layer.0.all.archive
DLC/runtime/DLCPackClassic.layer.0.all.archive
DLC/runtime/DLCPackEnforcer.layer.0.all.archive
DLC/runtime/DLCPackIntruder.layer.0.all.archive
DLC/runtime/DLCPackTactical.layer.0.all.archive
```

The program does **not** ship with any Deus Ex game archives.

## Main features

### Automatic game detection

You can select `DXMD.exe` and the editor resolves the proper game root and archive paths automatically.

For a normal Steam installation the structure looks like:

```text
Deus Ex Mankind Divided/
├─ retail/
│  └─ DXMD.exe
├─ runtime/
│  └─ Game.layer.1.all.archive
└─ DLC/
   └─ runtime/
      ├─ DLCPackAssault.layer.0.all.archive
      ├─ DLCPackClassic.layer.0.all.archive
      ├─ DLCPackEnforcer.layer.0.all.archive
      ├─ DLCPackIntruder.layer.0.all.archive
      └─ DLCPackTactical.layer.0.all.archive
```

If the JAR is placed somewhere inside the real game directory tree, the editor can also detect the installation at startup. Detection is deliberately structural: it walks upward through parent directories and validates the expected DXMD layout. It does **not** scan arbitrary nearby folders for archive copies.

Selecting the wrong archive from inside a valid DXMD installation is also tolerated. The selected file is treated as a location hint and the editor resolves the exact supported archive in the background.

### Base Game tab

The Base Game tab exposes established fields with simple controls. Current mappings include values such as:

- Hacking XP values
- Praxis and other shop prices
- Crafting costs
- Ammo and consumable stack sizes
- Inventory dimensions
- Weapon Parts stack size
- Tranquilizer Rifle magazine capacity
- Lancer Rifle magazine capacity
- Grenade Launcher ammo height
- Energy auto-regeneration limit
- Biocell energy gain
- Takedown energy cost
- Experimental augmentation behavior

The main tab is intended for fields that have enough evidence behind them to be presented as normal editor controls.

### Base Research tab

The Base Research tab is the diagnostic side of the project. It includes known fields plus thousands of candidate regions discovered by comparing clean and modified archive versions.

Each field can show:

- **Attribute Name** — confirmed, suspected, or unidentified purpose
- **User ID** — your own editable annotation for discoveries
- **Current Decimal** — the editable value
- **Current Hex** — read-only byte representation
- **Original Hex** — clean-game reference
- Up to **three comparison profiles** at once

`Current Decimal` is the only editable numeric field in the research tables.

### Comparison profiles

The research view can compare the current archive against selected reference profiles. Profiles currently include data derived from multiple community mods and controlled version-to-version comparisons, including:

- Hardcore Revival — Normal
- Hardcore Revival — Optional
- Adam 2.0
- Adam 3.0
- Tweaks
- Master Inventory
- No Health Regen
- No Health Regen — Variety
- No Health Regen — Variety B
- More Energy Regeneration — Half
- More Energy Regeneration — Full
- Inventory Stacking

Additional profiles can be added as useful reference archives are studied.

### DLC tabs

Installed DLC packs are shown as individual tabs instead of a single pack selector.

Missing DLC packs do not create errors and are not treated as required dependencies.

Each installed DLC tab has its own research table, comparison selectors, presets, backup/restore controls, and current/original values.

### Color legend

The research views use color to make testing easier:

- **Green text / known classification** — confirmed or strongly established field
- **Red** — current value differs from the original/default value
- **Dark green hex background** — displayed hex matches Original
- **Blue hex background** — displayed hex differs from Original
- **Orange / warning classification** — a field with a known save/game risk

If a field is both known and modified, the modified indication takes priority so changes are easy to spot.

### Risk warnings

Some fields are technically editable but can be unsafe in an existing save. Inventory dimensions are a good example: changing the dimensions of an item already present in a save can cause inventory problems or save instability.

The editor does not block these edits. Instead it:

- Shows the warning in hover text where applicable
- Displays a warning before applying a risky change
- Explains the known reason for the warning
- Recommends a new game where appropriate
- Provides a **Don't show this warning again this session** option

The suppression lasts only for the current application session.

## Backups and restoration

The editor uses two different recovery systems because they solve different problems.

### `.bak` backup

Before the editor first writes an archive, it creates:

```text
<archive name>.bak
```

The backup is not overwritten on later edits. This preserves the exact file state that existed before DXMD Archive Editor Pro first modified it — including any other mods that were already installed.

**Restore `.bak`** returns the entire archive to that exact pre-editor state.

### Restore Editor Fields to Original

The editor also stores compact original-value profiles for mapped fields.

**Restore Editor Fields to Original** writes only the original bytes for fields the editor knows about. It does not replace the entire archive, so unrelated modifications from other mods are left alone.

This is intentionally different from restoring the `.bak` file.

## Archive identification

SHA-256 hashes are used as informational labels only. Known archives may be identified as Original or a known reference profile.

An unknown or custom hash is still editable. The editor does not reject a file merely because another mod has already changed it.

## User field identification files

Research discoveries can be exported with **Save Identifications...** and later restored with **Load Identifications...**.

The TSV export contains data such as:

```text
Scope
Archive
FieldID
Offset
Length
OriginalHex
CurrentHex
BuiltInGroup
BuiltInLabel
UserID
```

The internal Field ID and offset are intentionally stored in the export even though the Field ID is hidden in the UI. That gives each discovery a stable identity when reports are shared or later promoted into the built-in Known database.

A useful workflow is:

1. Change one candidate field.
2. Apply the archive.
3. Test the result in game.
4. Enter what the field did in **User ID**.
5. Save the identification TSV.
6. Share the TSV with the project so confirmed discoveries can be incorporated into future versions.

## Research status

DXMD Archive Editor Pro is partly an editor and partly an ongoing reverse-engineering project.

Some fields are fully confirmed. Others are intentionally labeled with names such as:

```text
Suspected: Weapon Stat — Unknown
Suspected: Health Regeneration Control
Unidentified
```

These labels are conservative by design. A suspected field should not be treated as confirmed until controlled archive comparisons or in-game testing establish what it does.

The project has already identified several repeated archive structures, including families related to:

- Item and ammo stacks
- Inventory dimensions
- Weapon magazine capacity
- Weapon upgrade costs
- Weapon upgrade records such as `AMMO_CAPACITY_01`, `DAMAGE_01`, and similar entries
- Hacking and other XP rewards
- Energy regeneration
- Biocell energy gain
- Takedown energy consumption
- Experimental augmentations
- Suppressor behavior
- Weapon core records

Long term, the goal is to replace as many fragile fixed offsets as practical with validated record/signature-based discovery.

### XP / reward database

v0.6.9 expands the decoded base-game XP/reward database and adds a dedicated **XP Rewards** tab for confirmed reward controls. The mappings use DXMD-specific reward documentation as high-level evidence and are cross-checked against the clean archive's internal record names, exact default values, repeated 24-byte record layout, and available mod comparison profiles. DXHR-only information is not used to identify DXMD fields.

Confirmed/strongly established families now include:

- Hacking tiers: **Script Kiddie**, **Grey Hat**, **Black Hat**, **Network Adept**, **Master Hacker**, and **First Try**
- Password/keycode tiers: **Access Granted**, **Free Admission**, **Open Sesame**, **Entering without Breaking**, and **Master Felonist**
- Stealth: **Ghost**, **Smooth Operator**, and **Reset**
- Remote hacking: **Paving the Way**, **Machina**, and **Flawless**
- Exploration: **Traveler**, **Explorer**, **Pathfinder**, **Trailblazer**, and **Scholar**
- Social rewards: **Silver Tongue**, **Split Decision**, **Life Lesson**, **Spin Doctor**, **On the Fence**, **Read the Room**, **Stop the Press**, and **Wait Your Turn**
- Enemy/combat tiers: **Trooper**, **Veteran**, **Elite**, and the Marchenko-specific **Sorry to disappoint you, Brother** reward
- Combat bonuses including **Merciful Soul**, **Marksman**, **Expedient**, **Multitasker**, **Shock Therapy**, **Surprise**, **Close Shave**, **Dust to Dust**, **Introvert**, **Juggernaut**, **Crash Landing**, **Piece by Piece**, **Sharpshooter**, **Chain Reaction**, **Master Blaster**, **Ring of Fire**, **Blown Away**, and **Collateral Damage**
- Mechanical targets: **Scrap Metal**, **Void Warranty**, and **Junk Yard**
- Objective-reward families associated with **Getting Things Done** and **Completionist**

Records whose internal names are visible but whose DXMD reward-title mapping is not yet sufficiently specific remain marked **Suspected** rather than being promoted to Known. Examples include `social_interrupt_split`, `remotehacking_haywire`, `combat_xp_twister`, and the `collect_treasure` family.

DXMD-specific reference pages used during this mapping include the Deus Ex Wiki/Fandom **Praxis (DXMD)** and **Experience by area (DXMD)** pages. Archive bytes and controlled mod comparisons remain the confirmation layer used by the editor.

### Weapon research context

v0.6.9 also adds a read-only **Weapon Reference** tab to support archive-field identification. The table collects DXMD weapon inventory ratings and hidden maximum-range values so repeated weapon records can be compared against known game data without treating a number match as proof by itself.

The reference tab intentionally separates displayed inventory ratings from tested gameplay behavior. DXMD-specific wiki/testing material shows that the displayed **Damage**, **Accuracy**, and related ratings can be misleading: silencers apply weapon-specific real damage penalties, armor-piercing ammunition can change actual target damage independently of the visible base rating, and some variants with similar displayed values have substantially different real damage.

Useful archive-research fingerprints now documented in the application include:

- Hidden maximum range values such as **35 m 10mm Pistol**, **60 m Combat Rifle**, **80 m Battle Rifle**, **175 m Sniper Rifle**, and **300 m Lancer Rifle**
- Nominal base/max Damage, Ammo Capacity, and Rate of Fire values for the major base-game weapons
- Accuracy, Recoil, and Reload Speed ratings
- Silencer modifier context and measured weapon-specific damage-loss behavior
- The Combat Rifle AP-ammunition damage behavior against hard targets
- Elite Combat Rifle and Elite Battle Rifle test results that demonstrate why visible ratings should not be treated as the underlying damage formula
- Tactical Shotgun laser/accuracy behavior as evidence for hidden behavior fields beyond the displayed stat

The Weapon Reference tab is research context only. Existing `*_CORE`, `DAMAGE_*`, `AMMO_CAPACITY_*`, `RATE_OF_FIRE_*`, `ACCURACY_*`, `RECOIL_*`, attachment, and similar Base Research rows remain **Suspected/Research** unless their exact archive meaning has independent confirmation.

## Running

With Java 11 or newer installed:

```text
java -jar DXMD-Archive-Editor-Pro-v0.6.9.jar
```

On Windows you can normally double-click the JAR if `.jar` files are associated with Java.

## Building from source

### Windows

From a Developer Command Prompt or any shell where `javac` and `jar` are on `PATH`:

```bat
build.bat
```

### Linux / macOS

```bash
./build.sh
```

The build scripts compile with:

```text
javac --release 11
```

and place the finished JAR in:

```text
dist/DXMD-Archive-Editor-Pro-v0.6.9.jar
```

## Project layout

```text
src/main/java/          Java source
src/main/java/options/  Archive option/value types
src/main/resources/     Research and DLC profile data
dist/                   Built JAR
build.bat                Windows build script
build.sh                 Unix-like build script
```

## Important notes

- Always keep backups of saves when testing new or suspected fields.
- Some archive changes may only behave correctly on a new game.
- Do not assume a research-field name is confirmed unless it is marked as known.
- Absolute offsets can vary between game releases/store builds. Current work is primarily based on the archive layouts supplied during development.
- The editor intentionally allows already-modded archives; use the comparison and backup tools accordingly.

## Credits

**LightPower1** — creator of the original **DXMD Archive File Editor Tool** (Nexus Mods #18), the Java editor this project grew from. The original tool made editing `Game.layer.1.all.archive` easier and established the starting implementation used for this expanded editor.

Special thanks to **MohamedASalama** and **Grognougnou** for their work on Deus Ex: Mankind Divided archive-file editing. They were also explicitly credited by the original DXMD Archive File Editor Tool author for their archive-editing work.

## Community research references

A large part of the field mapping has been helped by controlled comparisons against community-created DXMD archive mods and their documentation. Reference material used during research has included mods such as Hardcore Revival, Adam 2.0/3.0, Tweaks, Master Inventory, I Need The Edge, IPOAO, Silence To The Guns, Micro Assembler Overheat Fix, Icarus Reflexes, Mankind Redefined, No Health Regen, More Energy Regeneration, and Inventory Stacking.

Those mods remain the work of their respective authors. DXMD Archive Editor Pro uses research observations and byte-level comparisons; it does not redistribute the original game archives.

## Disclaimer

DXMD Archive Editor Pro is a community tool and is not affiliated with or endorsed by Eidos-Montréal, Square Enix, or the authors of the referenced mods.

Editing game archives can break a game installation or save data. Keep backups and use research fields carefully.
