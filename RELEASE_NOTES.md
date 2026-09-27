# DXMD Archive Editor Pro v0.6.16

## Editable Float16 research values

This update adds a **Float16 (LE)** column beside **Current decimal** in both Base Fields and DLC Fields.

- The column is editable only for exact 2-byte fields.
- It interprets those bytes as little-endian IEEE-754 binary16 without changing the existing integer/hex views.
- Editing Float16 updates the underlying two bytes, so Current Decimal, Float16 and Current Hex remain synchronized.
- Example: `2.25` encodes to `80 40`; `-2.25` encodes to `80 C0`.
- Finite values outside binary16's range (-65504 to 65504) are rejected instead of silently overflowing.
- Infinity and NaN can still be represented explicitly if entered by name.
- Blank Float16 cells on non-2-byte fields are intentional.

The Float16 view is a research aid. A plausible half-float value does not by itself prove that a field's game semantics are floating-point.

All v0.6.15 raw-archive audit corrections and conservative confidence rules remain intact.

## Requirements

Java 11 or newer. Keep backups before testing research-field edits.
