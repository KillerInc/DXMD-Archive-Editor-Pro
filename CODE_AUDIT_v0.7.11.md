# DXMD Archive Editor Pro v0.7.11 Code Audit

## Scope
Reviewed every Java source file currently in `src/main/java`, all option classes, Base/DLC research profile loaders, archive/context/index code, editing/backup/restore paths, Swing loading/theme code, and visible current UI strings.

## Fixed in v0.7.11
- Whole-archive context scans: Base/DLC context resolution now uses the streaming scanner.
- EDT stalls on Research Inspector source changes: first-time archive preparation runs in SwingWorker with an animated modal progress bar.
- U64 edit truncation: unsigned 8-byte values use BigInteger and allow the full 0..2^64-1 range.
- Fake candidate bytes: out-of-fragment inspection requires a loaded archive instead of returning zero-filled bytes.
- Grouped option ambiguity: differing mapped values are marked `[mixed]`.
- Boolean corruption/mod incompatibility: raw values must match an exact configured false or true encoding.
- ARCH self-link guess: removed the unsafe single-link fallback; local chunk ranges also require non-negative resource begins.
- Theme font regression: explicit monospaced views stay monospaced.
- DLC/profile parsers: added repeat-load clearing, row/order checks, duplicate-profile rejection and even-length hex validation.
- UI redundancy: shortened current headings/status text, source names, tab descriptions and Attribute labels while preserving full evidence.
- Stale terminology: current code/docs refer to Research Inspector rather than the removed Base/DLC Fields UI.

## Verified / retained
- Research profiles remain sorted and non-overlapping: Base 2,546 rows, DLC 350 rows.
- Logical resource catalog remains 10 verified payload ranges covering all 2,896 current research rows when archives are loaded.
- Selective restore still writes confirmed mappings only; research/suspected fields are excluded.
- `.bak` creation remains non-destructive: an existing backup is preserved.
- ArchiveResourceIndex caches by canonical path, size and modified timestamp.

## Remaining design debt
- Normal-editor writes are not transactionally journaled. A backup exists before writes, but a low-level I/O failure could leave a partially modified archive; automatic rollback is intentionally avoided because an old `.bak` may predate other user mods.
- Existing `.bak` files are preserved indefinitely and can become stale after game updates. A future backup-history/version UI would be safer than silently replacing them.
- Compressed profile resources are still Base64-decoded into memory before GZIP streaming. They are small compared with game archives, so this is low priority.
- LogicalResourceCatalog is a verified static catalog rather than a runtime HeaderLib parser. This keeps startup fast but requires a release to add newly decoded libraries.
- Context resolvers intentionally use heuristic nearby identifiers; they remain evidence only and never promote confidence by proximity.
