# DXMD Archive Editor Pro v0.7.1

## Unified Research Inspector

This release replaces the legacy Base Fields and DLC Fields research tabs with one archive-aware Research Inspector while keeping the confirmed Weapon Stats, Player Stats, Inventory Stats, Economy & Crafting and XP Rewards tabs.

### Research display
- Maps every research offset to its actual internal resource, chunk and resource-local offset by parsing the loaded archive directory.
- One selector covers Base and all five supported DLC archives.
- Displays raw context around the selected region instead of treating a changed-byte run as a guaranteed field boundary.
- Decodes candidate regions as little-endian integer, Float16, Float32 and Float64 values where applicable.
- Candidate start can move ±8 bytes and width can be 1, 2, 4 or 8 bytes.
- Automatically suggests common 4-byte Float32 boundary corrections for 2/3-byte diff fragments when surrounding bytes form a plausible float.
- Shows the existing confidence assessment, audit evidence and mod-comparison values beside the selected region.

### Editing and research notes
- Candidate regions can be edited as raw Hex, signed/unsigned integer, Float16 or Float32 values.
- Editing outside the original diff-run boundary produces an explicit warning.
- `.bak` behavior remains in place before writes.
- User ID annotations remain editable and can still be imported/exported as TSV.

### Validation
Using the supplied clean OG archives, all 2,546 Base research rows and all 350 DLC research rows mapped to an internal resource/chunk with zero unmapped rows. The original `80 3F` “Weird” case resolves to the full `00 00 80 3F` Float32 value (`1.0`) at the proper resource-local offset.

### Runtime
Java 21 or newer is required.
