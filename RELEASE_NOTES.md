# DXMD Archive Editor Pro v0.7.7

## DLC HeaderLib / logical-resource completion

- Adds verified HeaderLib/BIN1 mappings from the supplied DLC `pc_headerlib` collection.
- **Assault:** 52 / 52 research rows mapped.
- **Classic:** 52 / 52 research rows mapped.
- **Enforcer:** 72 / 72 research rows mapped.
- **Intruder:** 90 / 90 research rows mapped.
- **Tactical:** 84 / 84 research rows mapped.
- **DLC total:** 350 / 350.
- **Base + DLC total:** 2,896 / 2,896 current research rows.

The Research Inspector now shows logical resource, HeaderLib, resource/owner IDs, flags, payload offset/length, and resource type/magic for DLC rows just as it does for Base rows.

Tactical rows resolve across four useful structures: the Tactical pack entity, the MicroAssembler entity type, the preorder tranquilizer-rifle player template, and the preorder tranquilizer-rifle NPC template.

## Loading

The added DLC structure is stored as eight verified payload-range records in the immutable runtime catalog, so it adds negligible startup cost and does not scan the original HeaderLib 7z. The animated Loading Archive popup remains enabled and reports the HeaderLib logical-resource stage.

## Download packaging

Preferred download remains the ZIP containing the Java 21 JAR, SHA-256 checksum file and verification instructions. The direct JAR remains attached to the release.

Java 21 or newer remains required.
