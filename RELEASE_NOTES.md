# DXMD Archive Editor Pro v0.7.6

## HeaderLib / logical-resource research view

- Adds **Logical Resource** and **Payload Offset** columns to the Research Inspector.
- Adds a Logical Resource / HeaderLib Structure panel with:
  - logical assembly resource path
  - HeaderLib filename and library index
  - resource ID
  - owner ID
  - flags
  - payload-relative offset
  - payload length
  - resource type and header magic
- Candidate boundary changes recalculate the logical payload-relative offset.
- Evidence text explicitly distinguishes structural HeaderLib evidence from gameplay-semantic proof.
- Verified against the supplied DXMD v002 HeaderLib data: **2,546 / 2,546 Base research rows map successfully**.
- The current supplied HeaderLib set has no matching records for the five DLC research resource libraries; DLC logical-resource cells remain blank instead of being guessed.

## Loading

- The animated Loading Archive popup remains active.
- First-load status now includes `Loading HeaderLib logical-resource structure...` and `Mapping research fields to logical resources...`.
- The current verified mappings are bundled and immutable, so this adds negligible runtime cost; no 1.2 GB HeaderLib scan is performed at application startup.

## Download packaging

- Preferred download remains the ZIP containing the Java 21 JAR, SHA-256 checksum file and verification instructions.
- Direct JAR remains attached to the release.

Java 21 or newer remains required.
