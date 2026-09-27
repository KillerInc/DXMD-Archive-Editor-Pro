# DXMD Archive Editor Pro v0.7.4

## Readability

- Fixes remaining black text in the confirmed-stat tabs.
- Enabled option labels now use the light dark-theme text color.
- Valid numeric values use the light theme text; invalid values use the warning color.
- Disabled labels use the muted theme color.

## Loading Archive progress

- Adds a small dark-themed modal loading popup with a 0–100% progress bar.
- Shows stages for resolving game files, reading the ARCH directory, indexing resources, scanning readable identifiers, mapping research fields and populating editor views.
- Expensive Base research preparation runs in a `SwingWorker`, so the progress UI stays responsive.
- Startup auto-detection uses the same loading popup.
- Parsed `ArchiveResourceIndex` data is cached and reused when the Research Inspector opens the same unchanged archive.

Java 21 or newer remains required.
