# DXMD Archive Editor Pro v0.7.5

## Animated archive loading

- The Loading Archive bar now uses a continuously animated indeterminate state while background archive preparation is running.
- A small `Working...` text animation provides a second obvious activity cue even if the platform's progress-bar animation is subtle.
- Status text still changes through resolving files, reading the archive directory, indexing resources, scanning identifiers and mapping research fields.
- At completion the bar switches to determinate mode, fills to 100% and briefly shows `Ready`.
- The dark theme now explicitly themes `JProgressBar`, with a faster repaint interval/cycle.

## Download packaging

- Preferred download: `DXMD-Archive-Editor-Pro-v0.7.5.zip`.
- ZIP contains the JAR, `DXMD-Archive-Editor-Pro-v0.7.5.jar.sha256`, and `VERIFY.txt`.
- The direct JAR remains attached to the GitHub release.
- Browser reputation warnings can still occur for unsigned/uncommon Java applications; a trusted code-signing certificate would be required for a stronger publisher-reputation signal.

Java 21 or newer remains required.
