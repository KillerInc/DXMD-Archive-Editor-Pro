import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Read-only DXMD weapon research reference.
 *
 * Displayed inventory ratings are useful fingerprints for archive research but
 * are not assumed to be literal gameplay damage. Range and tested-behavior
 * notes are kept separate so research fields are not promoted without archive
 * confirmation.
 */
public class WeaponReferencePanel extends JPanel {
    private static final String[] COLUMNS = {
            "Weapon", "Damage", "Magazine", "Rate of Fire", "Accuracy", "Recoil", "Reload", "Max range (m)"
    };

    private static final Object[][] ROWS = {
            {"Stun Gun", "15", "8 -> 14", "11", "20", "30", "50", "8"},
            {"Tranquilizer Rifle", "50", "5 -> 12", "20", "75", "40", "35", "55"},
            {"10mm Pistol", "15 -> 30", "15 -> 38", "25 -> 53", "20", "30", "50", "35"},
            {"Tactical Shotgun", "70 -> 85", "6 -> 12", "33 -> 51", "15", "70", "20", "25"},
            {"Machine Pistol", "20 -> 35", "30 -> 60", "80 -> 89", "30", "60", "45", "45"},
            {"Combat Rifle", "35 -> 50", "30 -> 48", "60 -> 72", "45", "60", "30", "60"},
            {"Revolver", "50 -> 65", "6 -> 12", "25 -> 43", "55", "60", "30", "55"},
            {"Battle Rifle", "60 -> 75", "10", "10 -> 25", "50", "70", "40", "80"},
            {"Sniper Rifle", "70 -> 85", "5 -> 11", "12 -> 21", "65", "75", "30", "175"},
            {"Grenade Launcher", "80", "6", "40", "50", "55", "10", "n/a"},
            {"Lancer Rifle", "85 -> 100", "3", "5", "85", "70", "40", "300"},
            {"Devastator Shotgun", "70 -> 85", "32", "50", "30", "60", "20", "35"}
    };

    public WeaponReferencePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextArea intro = new JTextArea(
                "DXMD weapon-reference values for archive research. The inventory Damage/Rate/Accuracy/etc. ratings are useful fingerprints, " +
                "but the DXMD wiki and controlled damage testing show that some displayed ratings are not literal gameplay values. " +
                "Do not promote an archive field to Known from a number match alone; confirm with the internal record name/layout or a controlled archive comparison."
        );
        intro.setEditable(false);
        intro.setLineWrap(true);
        intro.setWrapStyleWord(true);
        intro.setBackground(getBackground());
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        add(intro, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(ROWS, COLUMNS) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(Math.max(table.getRowHeight(), 22));
        int[] widths = {170, 95, 105, 105, 85, 75, 75, 105};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JTextArea notes = new JTextArea();
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setRows(9);
        notes.setText(
                "Research context:\n" +
                "• The wiki explicitly warns that displayed weapon attributes can be inaccurate or misleading, especially Damage.\n" +
                "• Maximum range is a hidden gameplay attribute. Useful fingerprints include Pistol 35 m, Combat Rifle 60 m, Battle Rifle 80 m, Sniper 175 m, and Lancer 300 m.\n" +
                "• Silencers change several values at once. Wiki/test data reports nominal damage penalties of -5 Pistol, -10 Machine Pistol, -25 Combat Rifle, -30 Tactical Shotgun, and -40 Lancer, while measured real damage loss is roughly 25%, 47%, 25%, 25%, and 55% respectively.\n" +
                "• Combat Rifle AP ammunition was reported at about three times regular-ammo damage against turrets, so ammo modifiers should be treated separately from the base weapon Damage rating.\n" +
                "• Elite Combat Rifle testing reports substantially higher real damage than the standard rifle despite similar displayed ratings, which is evidence that hidden/core multipliers exist.\n" +
                "• Standard and Elite Battle Rifles were tested as having identical real damage, another warning not to equate visible ratings with the underlying damage calculation.\n" +
                "• Tactical Shotgun laser-sight Accuracy changes do not change pellet spread, suggesting additional hidden weapon-behavior fields.\n\n" +
                "Sources used as high-level DXMD evidence: Deus Ex Wiki/Fandom Weapons (DXMD), DXMD Weapon Damage Testing and weapon-specific DXMD pages; nominal stat transcription supplied with the project research."
        );
        add(new JScrollPane(notes), BorderLayout.SOUTH);
    }
}
