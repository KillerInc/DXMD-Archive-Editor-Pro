# DXMD Archive Editor Pro v0.7.11

## Full code + redundant-text audit

- Audited the complete Java source tree and all current Base/DLC research presentation paths.
- Removed duplicated confidence wording from Attribute; Status remains the confidence column.
- Shortened Research Inspector headings/status text and known-stat tab wording without removing evidence.
- Streamed archive text scanning instead of loading entire archives into heap.
- Added animated background loading when switching Research Inspector archives.
- Added true unsigned 64-bit editing through 18446744073709551615.
- Stopped candidate inspection from inventing zero bytes when an archive is not loaded.
- Grouped normal controls now mark mixed underlying values.
- Boolean controls reject unexpected raw encodings instead of silently coercing them.
- ARCH indexing no longer guesses that a single linked archive is the current file.
- Preserved monospaced raw-byte/interpretation views.
- Hardened bundled DLC/profile and compressed-resource parsing.
- Java 21 compile, lint and profile/UI regressions run before release.

See CODE_AUDIT_v0.7.11.md for findings and remaining design debt.
