# DXMD Archive Editor Pro — Field Confidence Audit v0.6.15

v0.6.15 replaces the reconstructed comparison baseline with an audit made directly from the complete supplied clean/original and modded archive set.

## Authoritative dataset

- 84 archive files inspected.
- 6 clean originals: Base plus Assault, Classic, Enforcer, Intruder and Tactical DLC.
- 78 physical modded-vs-original comparisons.
- 48 SHA-256-deduplicated supplied variants in the retained evidence set; 47 are modified and one is an exact-original control.
- 7,222 distinct changed byte-runs retained as derived evidence.
- No game or mod archive is redistributed by this repository; only hashes, offsets and changed-byte evidence are retained.

## Confidence rules

**Confirmed** requires a focused comparison, independent agreement between focused mods, or an already established editor mapping. **Strong suspected** means the effect family is isolated but the exact internal sub-role is not. **Suspected** means useful structural or contextual evidence exists but is not sufficient to identify the exact control. **Unidentified** remains the default when evidence does not isolate a role.

Generated nearby families such as `DAMAGE_01`, `ACCURACY_01`, `RATE_OF_FIRE_02` and `RECOIL_01` are never proof by themselves. Hardcore Revival remains valuable coverage but changes too many weapon properties simultaneously to isolate most sub-fields.

## v0.6.15 audited counts

- Base: 2,546 non-overlapping field records, 233 confirmed, 48 strong-suspected.
- Base comparison references: 29 after integrating all raw variants with the existing research references.
- DLC: 350 non-overlapping field records, 11 confirmed, 48 strong-suspected.
- Confirmed DLC by pack: Assault 2, Classic 1, Enforcer 2, Intruder 1, Tactical 5.

The row totals changed because exact multi-byte mappings now replace older overlapping fragments instead of being layered on top of them.

## Corrections from v0.6.14

### `4D C7 1C 10` is not the silencer-function ID

The supplied Silence To The Guns guide identifies `4D C7 1C 10` as a sniper/standard-reticle-related function. The mod replaces it with `26 AC CD 27`, which is the built-in silencer function ID. v0.6.15 therefore labels these rows **Standard-Reticle Function Slot / Silencer Override**. The previous interpretation that `4D C7 1C 10` itself was a silencer ID was incorrect.

### Elite Battle Rifle inventory width

The clean Assault archive byte at offset `54331` is `05`. Independent inventory-focused archives change the same byte `05→03`, while Favored Elites Plus changes it to `02`. This is now confirmed as the Elite Battle Rifle inventory-grid width. One supplied README describes the vanilla width as 6; the clean archive byte is authoritative for the stored value and the documentation discrepancy is retained rather than forcing the archive to match the prose.

## Newly strengthened Base mappings

Focused comparisons now confirm the Takedown Power Consumption control, Energy Auto-Regeneration Limit, Biocell Energy Gain, Takedown Energy Cost, four Grenade Launcher ammo-height controls, ten Experimental Augmentation gates, documented bolt-action override toggles, built-in-silencer enable toggles, and the reticle-function slots used by Silence To The Guns for silencer overrides.

The Takedown Power Consumption field is deliberately called a **control**, not a numeric cost: Icarus Reflexes changes the isolated four-byte value to zero, but its raw value behaves like an internal identifier/control. The separate float at `7413189` remains the confirmed Takedown Energy Cost.

## Suppressor debuff fields

I Need The Edge v1.2 isolates five Base two-component pairs and corresponding Enforcer/Intruder DLC pairs while its documented change removes suppressor damage debuffs. These are strong evidence for **Suppressor Damage-Debuff Components A/B**, but the internal distinction between the two components is not independently decoded, so they remain strong-suspected rather than receiving invented sub-stat names.

## Micro-Assembler contamination handling

Three Tactical `0ACC4075` markers changed to zero are confirmed as Micro-Assembler Experimental/Overclock gates because the supplied mod documentation identifies that behavior and the same marker structure is independently established by Adam 2.0. Tactical offsets `107324` and `108070` remain suspected: the supplied Micro-Assembler archive also carries unrelated Elite Tranquilizer inventory/magazine edits, so those bytes are not attributed to Micro-Assembler without independent evidence.

## Restore behavior

Selective **Restore Editor Fields to Original** follows this same confidence policy. Confirmed mappings are restored; strong-suspected, suspected and unidentified research bytes are left untouched. Tests against copies of the real clean Base and Assault archives verified that newly confirmed mappings restore correctly while research bytes remain unchanged. Exact `.bak` restoration remains the whole-file recovery path.

## Promotion rule

Future fields should move to Confirmed only when another focused archive, documentation plus an isolated byte change, or equivalent independent evidence removes the ambiguity. Proximity to a readable name or membership in a generated weapon family is not enough.
