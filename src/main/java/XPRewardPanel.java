import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import options.NumericOption;
import options.Option;
import options.ShortOption;

/**
 * DXMD base-game XP/reward editor.
 *
 * Mappings are cross-checked against DXMD-specific reward documentation and
 * confirmed against the clean archive's internal record names/default values.
 * Records that do not yet have a sufficiently specific DXMD title match stay
 * in Base Research instead of being exposed as normal controls here.
 */
public class XPRewardPanel extends JPanel {
    private final ArrayList<Option> options = new ArrayList<>();
    private final ArrayList<JTextField> fields = new ArrayList<>();
    private final ArrayList<JTextField> invalid = new ArrayList<>();
    private final JTextField archiveField = new JTextField();
    private final JTextArea description = new JTextArea();
    private final JButton defaults = new JButton("Default Values");
    private final JButton current = new JButton("Current File Values");
    private final JButton apply = new JButton("Apply XP Changes");
    private File archive;

    public XPRewardPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        fillOptions();

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(new JLabel("Base archive:"), BorderLayout.WEST);
        archiveField.setEditable(false);
        archiveField.setBackground(Color.WHITE);
        top.add(archiveField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 5, 3, 5);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int pair = i % 2;
            int row = i / 2;
            int valueCol = pair * 2;
            int labelCol = valueCol + 1;

            JTextField value = new JTextField();
            value.setPreferredSize(new Dimension(90, 30));
            value.setEnabled(false);
            value.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { validateField(value); }
                public void insertUpdate(DocumentEvent e) { validateField(value); }
                public void removeUpdate(DocumentEvent e) { validateField(value); }
            });
            java.awt.event.MouseAdapter hover = new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { description.setText(option.getOptionDesc()); }
                public void mouseExited(java.awt.event.MouseEvent e) { description.setText(""); }
            };
            value.addMouseListener(hover);

            JLabel label = new JLabel(option.getOptionName());
            label.addMouseListener(hover);

            gc.gridy = row; gc.gridx = valueCol; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            grid.add(value, gc);
            gc.gridx = labelCol; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            grid.add(label, gc);
            fields.add(value);
        }
        gc.gridx = 0; gc.gridy = (options.size() + 1) / 2; gc.gridwidth = 4;
        gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        grid.add(Box.createGlue(), gc);
        JScrollPane scroll = new JScrollPane(grid);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setRows(3);
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.add(new JScrollPane(description), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        defaults.setEnabled(false); current.setEnabled(false); apply.setEnabled(false);
        defaults.addActionListener(e -> showPreset(1));
        current.addActionListener(e -> showPreset(0));
        apply.addActionListener(e -> applyChanges());
        buttons.add(defaults); buttons.add(current); buttons.add(apply);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    public void loadArchive(File f) {
        if (f == null || !f.isFile()) { clearArchive("Base archive not found."); return; }
        try {
            FileAnalyzer.analyze(f, options);
            archive = f;
            archiveField.setText(f.getAbsolutePath());
            for (JTextField field : fields) field.setEnabled(true);
            defaults.setEnabled(true); current.setEnabled(true);
            showPreset(0);
            description.setText("Loaded " + options.size() + " confirmed DXMD XP/reward controls. " +
                    "Unmatched internal reward records remain in Base Research as Suspected.");
        } catch (IOException ex) {
            clearArchive("Could not load base archive: " + ex.getMessage());
        }
    }

    public void clearArchive(String message) {
        archive = null;
        archiveField.setText("");
        for (JTextField field : fields) field.setEnabled(false);
        defaults.setEnabled(false); current.setEnabled(false); apply.setEnabled(false);
        description.setText(message);
    }

    private void showPreset(int index) {
        for (int i = 0; i < options.size(); i++) {
            fields.get(i).setText(Integer.toString(((NumericOption) options.get(i)).getSpecificValue(index)));
        }
        updateApply();
    }

    private void validateField(JTextField field) {
        boolean ok = false;
        try {
            int v = Integer.parseInt(field.getText().trim());
            ok = v >= 0 && v <= 65535;
        } catch (Exception ignored) {}
        if (ok) { invalid.remove(field); field.setForeground(Color.BLACK); }
        else { if (!invalid.contains(field)) invalid.add(field); field.setForeground(Color.RED); }
        updateApply();
    }

    private void updateApply() {
        apply.setEnabled(archive != null && invalid.isEmpty());
    }

    private void applyChanges() {
        if (archive == null) return;
        try {
            for (int i = 0; i < options.size(); i++) {
                ((NumericOption) options.get(i)).setNewValue(Integer.parseInt(fields.get(i).getText().trim()));
            }
            BackupManager.ensureBackup(archive);
            ExecuteChanges.run(archive, options);
            Launcher.refreshBaseViews(archive);
            loadArchive(archive);
            description.setText("XP/reward changes applied and all base views reloaded.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Apply XP error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ArrayList<Long> a(long... values) {
        ArrayList<Long> out = new ArrayList<>();
        for (long v : values) out.add(v);
        return out;
    }

    private void add(String name, String desc, int def, long... offsets) {
        options.add(new ShortOption(a(offsets), name, desc + " Range:0-65535", def));
    }

    private void fillOptions() {
        add("Script Kiddie (Hack L1)", "Hacking level 1 reward. Clean DXMD: 25 XP; internal hacking_lvl_1.", 25, 5401405);
        add("Grey Hat (Hack L2)", "Hacking level 2 reward. Clean DXMD: 50 XP; internal hacking_lvl_2.", 50, 5401429);
        add("Black Hat (Hack L3)", "Hacking level 3 reward. Clean DXMD: 75 XP; internal hacking_lvl_3.", 75, 5401453);
        add("Network Adept (Hack L4)", "Hacking level 4 reward. Clean DXMD: 100 XP; internal hacking_lvl_4.", 100, 5401477);
        add("Master Hacker (Hack L5)", "Hacking level 5 reward. Clean DXMD: 125 XP; internal hacking_lvl_5.", 125, 5401501);
        add("First Try", "Complete a hack on the first attempt. Clean DXMD: 5 XP; internal hacking_firsttry.", 5, 5401525);

        add("Access Granted (Code L1)", "Password/keycode level 1 reward; internal pw_lvl_1.", 25, 5402277);
        add("Free Admission (Code L2)", "Password/keycode level 2 reward; internal pw_lvl_2.", 50, 5402301);
        add("Open Sesame (Code L3)", "Password/keycode level 3 reward; internal pw_lvl_3.", 75, 5402325);
        add("Entering without Breaking (Code L4)", "Password/keycode level 4 reward; internal pw_lvl_4.", 100, 5402349);
        add("Master Felonist (Code L5)", "Password/keycode level 5 reward; internal pw_lvl_5.", 125, 5402373);

        add("Ghost", "Objective completed unseen; internal obj_notseen.", 200, 5400197);
        add("Smooth Operator", "Objective completed without triggering an alarm; internal obj_noalarm.", 200, 5400221);
        add("Reset", "Alarm state subsides/returns to cautious; internal moodswing_cautiousreturn.", 10, 5402165);

        add("Paving the Way", "Remote-hack environmental device; internal remotehacking_environment.", 5, 5402477);
        add("Machina", "Remote-hack security device/vehicle; internal remotehacking_success.", 10, 5401949);
        add("Flawless", "Remote hack without a mistimed input; internal remotehacking_nomiss_alt.", 5, 5401973);

        add("Traveler", "Exploration reward; internal secretarea_traveler.", 100, 5402861);
        add("Explorer", "Exploration reward; internal secretarea_explorer.", 200, 5402885);
        add("Pathfinder", "Exploration reward; internal secretarea_pathfinder.", 300, 5402909);
        add("Trailblazer", "Exploration reward; internal secretarea_trailblazer.", 400, 5402933);
        add("Scholar", "Read a unique eBook; internal collect_scholar.", 100, 5405541);

        add("Wait Your Turn", "Failed CASIE/QTE interrupt; internal social_interrupt_fail.", 50, 5401677);
        add("Stop the Press", "Successful CASIE/QTE interrupt; internal social_interrupt_win.", 200, 5401789);
        add("Life Lesson", "Major persuasion failure; internal social_debate_lose.", 250, 5401901);
        add("Split Decision", "Major persuasion partial success; internal social_debate_neutral.", 500, 5405365);
        add("Silver Tongue", "Major persuasion success; internal social_debate_win.", 1000, 5403117);
        add("Read the Room", "Minor persuasion failure; internal social_persuade_fail.", 100, 5402533);
        add("On the Fence", "Minor persuasion partial success; internal social_persuade_split.", 250, 5402589);
        add("Spin Doctor", "Minor persuasion success; internal social_persuade_win.", 500, 5402645);

        add("Trooper XP", "Base Trooper-tier neutralization XP; internal combat_incap_smallfry.", 10, 5403165);
        add("Veteran XP", "Base Veteran-tier neutralization XP; internal combat_incap_veteran.", 20, 5405757);
        add("Elite XP", "Base Elite-tier neutralization XP; internal combat_incap_bigdawg.", 30, 5398965);
        add("Marchenko XP", "Viktor Marchenko base reward ('Sorry to disappoint you, Brother'); internal combat_incap_sorrytodisappoint.", 100, 5404285);
        add("Merciful Soul", "Non-lethal neutralization bonus; internal combat_xp_nonlethal.", 20, 5398989, 5403189, 5404309, 5405781);
        add("Marksman", "Headshot neutralization bonus; internal combat_xp_headshot.", 10, 5399013, 5403213, 5404333, 5405805);
        add("Expedient", "Standard melee-takedown bonus. Marchenko's separate 20-XP record remains research-only.", 10, 5399037, 5403237, 5405829);
        add("Multitasker", "Double-takedown bonus; internal combat_xp_takedownmulti.", 45, 5399061, 5402221, 5403261, 5404381, 5405853);
        add("Shock Therapy", "TESLA neutralization bonus.", 10, 5399085, 5403285, 5404405, 5405877);
        add("Surprise", "Punch-through-wall neutralization bonus.", 10, 5399109, 5403309, 5404429, 5405901);
        add("Close Shave", "Nanoblade neutralization bonus.", 10, 5399133, 5403333, 5404453, 5405925);
        add("Dust to Dust", "P.E.P.S. focused-blast neutralization bonus.", 10, 5399157, 5403357, 5404477, 5405949);
        add("Introvert", "Typhoon neutralization bonus.", 10, 5399181, 5403381, 5404501, 5405973);
        add("Juggernaut", "Charged Icarus Dash neutralization bonus.", 10, 5399205, 5403405, 5404525, 5405997);
        add("Crash Landing", "Icarus Strike neutralization bonus.", 10, 5399229, 5403429, 5404549, 5406021);
        add("Piece by Piece", "Armor-destruction XP chunk; internal combat_xp_piecebypiece.", 5, 5399253, 5402693, 5403453, 5404573, 5406045);
        add("Sharpshooter", "Focus Enhancement streak bonus.", 5, 5399277, 5403061, 5403477, 5404597, 5406069);
        add("Chain Reaction", "Multi-target TESLA bonus.", 5, 5399301, 5400085, 5403501, 5404621, 5406093);
        add("Master Blaster", "Explosive Nanoblade multi-kill bonus.", 5, 5399325, 5402109, 5403525, 5404645, 5406117);
        add("Ring of Fire", "Multi-target Typhoon bonus.", 5, 5399373, 5402749, 5403573, 5404693, 5406165);
        add("Blown Away", "Fragmentation grenade/mine neutralization bonus.", 5, 5399397, 5403597, 5404717, 5406189);
        add("Collateral Damage", "Multi-target fragmentation explosion bonus.", 10, 5399421, 5400141, 5403621, 5404741, 5406213);

        add("Scrap Metal", "Destroy a turret; internal combat_disable_turret.", 15, 5402805);
        add("Void Warranty", "Destroy a flying drone; internal combat_disable_drone.", 20, 5406877);
        add("Junk Yard", "Destroy a walker/sentry robot; internal combat_disable_sentry.", 40, 5401845);
    }
}
