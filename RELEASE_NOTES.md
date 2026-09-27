# DXMD Archive Editor Pro v0.7.9

## Comparison Evidence alignment fix

- Replaces the right-side Comparison Evidence pseudo-table with a real `JTable`.
- Columns are now **Source**, **Hex**, and **Decoded** with stable column boundaries.
- Fixes alignment drift caused by padded text under Windows font/DPI scaling.
- Keeps all existing Base/DLC comparison rows and decoded values.
- The compact left Research Inspector table from v0.7.8 remains unchanged.

Java 21 or newer remains required.
