# DXMD Archive Editor Pro v0.6.9

## XP / reward database expansion

This release substantially expands the decoded XP/reward fields in the base-game archive.

Mappings are based on DXMD-specific reward documentation and cross-checked against the clean archive's internal record names, exact default values, repeating record layout, and available comparison-mod archives. DXHR-only information is not used to identify DXMD fields.

### New XP Rewards controls

- Hacking reward titles: Script Kiddie, Grey Hat, Black Hat, Network Adept, Master Hacker, First Try.
- Password/keycode rewards: Access Granted, Free Admission, Open Sesame, Entering without Breaking, Master Felonist.
- Stealth rewards: Ghost, Smooth Operator, Reset.
- Remote hacking: Paving the Way, Machina, Flawless.
- Exploration: Traveler, Explorer, Pathfinder, Trailblazer, Scholar.
- Social/CASIE rewards: Silver Tongue, Split Decision, Life Lesson, Spin Doctor, On the Fence, Read the Room, Stop the Press, Wait Your Turn.
- Combat tiers: Trooper, Veteran, Elite, and the Marchenko-specific reward.
- Combat bonuses: Merciful Soul, Marksman, Expedient, Multitasker, Shock Therapy, Surprise, Close Shave, Dust to Dust, Introvert, Juggernaut, Crash Landing, Piece by Piece, Sharpshooter, Chain Reaction, Master Blaster, Ring of Fire, Blown Away, Collateral Damage.
- Mechanical targets: Scrap Metal, Void Warranty, Junk Yard.

### Base Research improvements

- Promoted 156 XP/reward records to explicit two-byte editable reward fields.
- Base Research now contains 2,553 fields across the existing 12 comparison profiles.
- Added named reward categories for combat, stealth, passwords, remote hacking, exploration, social interactions, and objectives.
- Objective reward families are identified as Getting Things Done and Completionist where their archive structure supports it.
- Internal records without a sufficiently specific DXMD title match remain marked Suspected instead of being promoted to Known. This includes `social_interrupt_split`, `remotehacking_haywire`, `combat_xp_twister`, and `collect_treasure` records.
- The special Marchenko takedown record remains separate in Base Research because its clean XP differs from the standard Expedient records.

## Weapon research reference

- Added a read-only **Weapon Reference** tab using DXMD-specific weapon documentation, Aeratus' DXMD weapon-damage testing context, and the supplied nominal-stat transcription.
- Added base/max inventory ratings for Damage, Magazine Capacity and Rate of Fire, plus Accuracy, Recoil and Reload Speed for major weapons.
- Added hidden maximum-range fingerprints, including the 35 m Pistol, 60 m Combat Rifle, 80 m Battle Rifle, 175 m Sniper Rifle and 300 m Lancer Rifle.
- Documented that displayed weapon ratings are research fingerprints rather than literal damage values.
- Added silencer context, AP-ammunition behavior, Elite Combat Rifle/Battle Rifle comparison notes, and Tactical Shotgun accuracy/pellet-spread context to help decode `*_CORE`, attachment and weapon-stat records without over-promoting them.
- No weapon archive field was promoted to Known solely from a wiki/IGN number match; archive-side confirmation is still required.

## Existing functionality

The v0.6.8 DLC Compare initialization fix remains included, along with backup/restore, archive detection, comparison profiles, User ID import/export, save-risk warnings, and all previously mapped base/DLC controls.

## Requirements

Java 11 or newer.

Keep saves backed up when testing newly identified or inventory-dimension fields.
