# DXMD Archive Editor Pro v0.7.13

## Controlled archive research + functional weapon controls

- Extracted and compared the original Hardcore Revival 1.024 Normal and Optional 7z packages from the project Library.
- Verified that all five DLC archives are byte-identical between Normal and Optional.
- Base Game.layer.1 differs by exactly **83 bytes across 60 stack-value runs**.
- Confirmed every changed run at a verified stack marker +16:
  - 53 × `8D0137A2`
  - 7 × `B4254725`
- Corrected 26 previously stored controlled-stack originals from Hardcore-Normal bytes to their true clean-OG values.
- Replaced generic controlled stack labels with real item names where the clean archive exposes them.
- Added normal controls for:
  - EMP ammunition stacks
  - grenade-launcher payload stacks
  - PEPS / TESLA / Nanoblade / Typhoon ammo stacks
  - alcohol stacks
  - Neuropozyne stack
  - Gyroscopic Regulator / Hydraulic Micropump / Stem Processor Chip stacks
- Translated the workbook/tutorial functional hashes into clean Steam Game.layer.1 offsets.
- Added verified base **Magazine**, **Range**, and **Reload Timing** controls for 15 named weapons.
- Validated all 71 newly added/corrected offsets directly against the clean OG archive.
- Kept display-only Accuracy/Damage/ROF/Recoil/Reload values research-only.
- Preserved the existing Confirmed / Strong Suspected / Suspected / Unidentified methodology.

The release ZIP contains only the JAR.
