# DXMD Archive Editor Pro v0.7.12

## Research integration release

- Added verified runtime hash/function recognition to Research Inspector.
- Added published value-layout overlap detection so the inspector can distinguish a nearby marker from a selected run that actually intersects a documented value field.
- Added verified paragraph/weapon ownership context for published Damage, Rate of Fire, Reload, Recoil, Ammo Capacity, Fire Pattern and Silencer records without changing confidence automatically.
- Applied the recovered `deusex_md_invetory.xlsx` workbook research:
  - `578E035D` = displayed Accuracy
  - `0519EE4D` = displayed Damage/Lethality
  - `652633EC` = displayed Rate of Fire
  - `4801D357` = displayed Recoil
  - `3EE6E864` = displayed Reload Speed
  - `79D898E8` = base magazine capacity
- Added the workbook-confirmed alternate inventory-height layout for `5FA99754`.
- Preserved archive-version safety: Game.layer.0 workbook offsets are not copied into Steam/Game.layer.1 normal controls.
- Named confirmed Base suppressor display mappings by actual weapon owner.
- Expanded persistent research documentation and known-bad/contradictory research notes.
- Removed the obsolete broken v0.7.11 retry workflow.
- Java 21 build/lint and verified-layout regression checks pass before release.

See **RESEARCH_SOURCES.txt** for the full research trail and **RESEARCH_RECONCILIATION.txt** for paragraph/row reconciliation details.
