# DXMD Archive Editor Pro v0.7.2

## Dark Research UI

- Restyles the full application to match the approved dark Research Inspector mockup.
- Dark navy application/panel backgrounds with teal/cyan accents.
- Dark research tables and headers, teal row selection, subdued grid/border treatment and high-contrast text.
- Raw byte and interpretation panes use a dark code-view presentation.
- Buttons, text fields, combo boxes, spinners, tabs, scroll panes and titled panels use one consistent palette.
- Research Inspector is the default selected tab at startup.
- The five confirmed-stat tabs remain available and receive the same visual theme.
- Java 21 remains the minimum runtime.

## Dawn/DXMD parser cross-check

The supplied pure-Python Dawn/DXMD extractor was tested against the clean DLC archives and independently confirmed the `ARCH` directory/chunk model used by the Java Research Inspector. The Python implementation also documents HeaderLib (`BILH`/`BIN1`) parsing and `pc_resourcelib` reconstruction, which will be useful for deeper resource-level research. It is not required to run the Java editor.
