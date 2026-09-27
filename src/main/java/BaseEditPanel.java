import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import options.BooleanOption;
import options.FloatOption;
import options.InventoryXOption;
import options.NumericOption;
import options.Option;

/** Shared normal-edit UI for the categorized base-game tabs. */
@SuppressWarnings({"serial", "this-escape"})
public class BaseEditPanel extends JPanel {
    private final String sectionName;
    private final String sectionDescription;
    private final ArrayList<Option> options;
    private final ArrayList<JComponent> entryValues = new ArrayList<JComponent>();
    private final ArrayList<JLabel> entryLabels = new ArrayList<JLabel>();
    private final ArrayList<JTextField> invalidValues = new ArrayList<JTextField>();
    private final Set<Option> dirtyOptions = new HashSet<Option>();
    private boolean updatingProgrammatically;
    private final JTextField archiveField = new JTextField();
    private final JTextArea description = new JTextArea();
    private final JButton defaults = new JButton("Default Values");
    private final JButton current = new JButton("Current File Values");
    private final JButton apply = new JButton("Apply");
    private File archive;

    public BaseEditPanel(String sectionName, String sectionDescription, ArrayList<Option> options) {
        this.sectionName = sectionName;
        this.sectionDescription = sectionDescription;
        this.options = options;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildHeader();
        buildOptionGrid();
        buildFooter();
        updateApplyButton();
    }

    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 8));
        JPanel titleRow = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel(sectionName);
        title.setFont(new Font("Courier New", Font.BOLD, 18));
        titleRow.add(title, BorderLayout.CENTER);

        JButton select = new JButton("Select EXE / Archive");
        select.setFont(new Font("Courier New", Font.BOLD, 15));
        select.addActionListener(e -> selectArchive());
        titleRow.add(select, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        archiveField.setEditable(false);
        archiveField.setBackground(UiTheme.FIELD);
        archiveField.setForeground(UiTheme.TEXT);
        archiveField.setFont(new Font("Courier New", Font.PLAIN, 13));
        header.add(archiveField, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
    }

    private void buildOptionGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 5, 4, 5);
        gc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int pair = i % 2;
            int row = i / 2;
            int valueCol = pair * 2;
            int labelCol = valueCol + 1;

            JComponent valueComponent;
            if (option instanceof NumericOption || option instanceof FloatOption) {
                JTextField field = new JTextField();
                field.setFont(new Font("Courier New", Font.BOLD, 18));
                field.setPreferredSize(new Dimension(95, 34));
                field.setEnabled(false);
                field.setForeground(UiTheme.TEXT);
                field.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) { onTextEdited(field, option); }
                    public void removeUpdate(DocumentEvent e) { onTextEdited(field, option); }
                    public void insertUpdate(DocumentEvent e) { onTextEdited(field, option); }
                });
                valueComponent = field;
            } else {
                JCheckBox box = new JCheckBox();
                box.setEnabled(false);
                box.addActionListener(e -> {
                    if (!updatingProgrammatically) dirtyOptions.add(option);
                    updateApplyButton();
                });
                valueComponent = box;
            }

            java.awt.event.MouseAdapter hover = new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { description.setText(option.getOptionDesc()); }
                public void mouseExited(java.awt.event.MouseEvent e) { description.setText(sectionDescription); }
            };
            valueComponent.addMouseListener(hover);

            gc.gridy = row;
            gc.gridx = valueCol;
            gc.weightx = 0.0;
            gc.fill = GridBagConstraints.NONE;
            grid.add(valueComponent, gc);
            entryValues.add(valueComponent);

            JLabel label = new JLabel(option.getOptionName());
            label.setFont(new Font("Courier New", Font.PLAIN, 17));
            label.setForeground(UiTheme.MUTED);
            label.addMouseListener(hover);
            gc.gridx = labelCol;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            grid.add(label, gc);
            entryLabels.add(label);
        }

        gc.gridx = 0;
        gc.gridy = (options.size() + 1) / 2;
        gc.gridwidth = 4;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        grid.add(Box.createGlue(), gc);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);
    }

    private void buildFooter() {
        description.setFont(new Font("Courier New", Font.PLAIN, 15));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setRows(4);
        description.setText(sectionDescription);

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(new JScrollPane(description), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout(8, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        defaults.setEnabled(false);
        current.setEnabled(false);
        defaults.addActionListener(e -> showPreset(1, true));
        current.addActionListener(e -> showPreset(0, false));
        left.add(defaults);
        left.add(current);
        buttons.add(left, BorderLayout.WEST);

        apply.setFont(new Font("Courier New", Font.BOLD, 18));
        apply.setPreferredSize(new Dimension(150, 42));
        apply.addActionListener(e -> applyChanges());
        buttons.add(apply, BorderLayout.EAST);
        lower.add(buttons, BorderLayout.SOUTH);
        add(lower, BorderLayout.SOUTH);
    }

    private void selectArchive() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select DXMD.exe or Game.layer.1.all.archive");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("DXMD executable / archives (*.exe, *.archive)", "exe", "archive"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            Launcher.openGameOrArchive(chooser.getSelectedFile(), this);
    }

    public void loadArchive(File file) {
        if (file == null || !file.isFile()) {
            clearArchive("Base archive not found.");
            return;
        }
        try {
            BaseResearchProfiles.Profile profile = BaseResearchProfiles.get();
            if (!file.getName().equalsIgnoreCase(profile.name))
                throw new IOException("Expected " + profile.name + ".");
            if (file.length() != profile.size)
                throw new IOException("Unexpected file size. Expected " + profile.size + " bytes, got " + file.length() + ".");

            FileAnalyzer.analyze(file, options);
            archive = file;
            archiveField.setText(file.getAbsolutePath());
            invalidValues.clear();
            dirtyOptions.clear();
            ArrayList<String> mixed = new ArrayList<>();
            for (int i = 0; i < entryValues.size(); i++) {
                entryValues.get(i).setEnabled(true);
                Option option = options.get(i);
                boolean isMixed = option.hasMixedCurrentValues();
                entryLabels.get(i).setText(option.getOptionName() + (isMixed ? " [mixed]" : ""));
                entryLabels.get(i).setForeground(isMixed ? UiTheme.SUSPECTED : UiTheme.TEXT);
                if (isMixed) mixed.add(option.getOptionName());
            }
            defaults.setEnabled(true);
            current.setEnabled(true);
            showPreset(0, false);
            String mixedText = mixed.isEmpty() ? "" : "\nMixed values: " + String.join(", ", mixed)
                    + ". Editing synchronizes their mapped addresses.";
            description.setText(sectionDescription + mixedText
                    + (BackupManager.hasBackup(file) ? "\nBackup available." : ""));
        } catch (Exception ex) {
            clearArchive("Could not load base archive: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), sectionName + " load error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearArchive(String message) {
        archive = null;
        archiveField.setText("");
        invalidValues.clear();
        dirtyOptions.clear();
        for (JComponent c : entryValues) c.setEnabled(false);
        for (int i = 0; i < entryLabels.size(); i++) {
            entryLabels.get(i).setText(options.get(i).getOptionName());
            entryLabels.get(i).setForeground(UiTheme.MUTED);
        }
        defaults.setEnabled(false);
        current.setEnabled(false);
        description.setText(message);
        updateApplyButton();
    }

    private void showPreset(int index, boolean markDirty) {
        updatingProgrammatically = true;
        try {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                if (option instanceof FloatOption)
                    ((JTextField) entryValues.get(i)).setText(Float.toString(((FloatOption) option).getSpecificValue(index)));
                else if (option instanceof NumericOption)
                    ((JTextField) entryValues.get(i)).setText(Integer.toString(((NumericOption) option).getSpecificValue(index)));
                else
                    ((JCheckBox) entryValues.get(i)).setSelected(((BooleanOption) option).getSpecificValue(index));
            }
        } finally {
            updatingProgrammatically = false;
        }
        invalidValues.clear();
        dirtyOptions.clear();
        if (markDirty) dirtyOptions.addAll(options);
        updateApplyButton();
    }

    private void onTextEdited(JTextField field, Option option) {
        validateField(field, option);
        if (!updatingProgrammatically) dirtyOptions.add(option);
        updateApplyButton();
    }

    private void validateField(JTextField field, Option option) {
        boolean valid = false;
        try {
            if (option instanceof FloatOption) {
                float value = Float.parseFloat(field.getText().trim());
                FloatOption f = (FloatOption) option;
                valid = Float.isFinite(value) && value >= f.getMinValue() && value <= f.getMaxFloatValue();
            } else {
                int value = Integer.parseInt(field.getText().trim());
                valid = value >= 0 && value <= ((NumericOption) option).getMaxValue();
            }
        } catch (Exception ignored) {}

        if (valid) {
            invalidValues.remove(field);
            field.setForeground(UiTheme.TEXT);
        } else {
            if (!invalidValues.contains(field)) invalidValues.add(field);
            field.setForeground(UiTheme.DANGER);
        }
        updateApplyButton();
    }

    private void applyChanges() {
        if (archive == null) return;
        ArrayList<Option> changed = new ArrayList<Option>();
        try {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                if (option instanceof FloatOption)
                    ((FloatOption) option).setNewValue(Float.parseFloat(((JTextField) entryValues.get(i)).getText().trim()));
                else if (option instanceof NumericOption)
                    ((NumericOption) option).setNewValue(Integer.parseInt(((JTextField) entryValues.get(i)).getText().trim()));
                else
                    ((BooleanOption) option).setNewValue(((JCheckBox) entryValues.get(i)).isSelected());
                if (dirtyOptions.contains(option)) changed.add(option);
            }

            if (changed.isEmpty()) {
                description.setText("No changes to apply.");
                return;
            }

            ArrayList<String> risky = new ArrayList<String>();
            for (Option option : changed) if (isInventoryDimension(option)) risky.add(option.getOptionName());
            if (!risky.isEmpty() && !RiskWarning.confirm(this, "Risky Inventory Dimension Edit",
                    "These controls change inventory item dimensions. Existing saves can retain previous dimensions; changing the size of an item already present can make that save unusable or crash the inventory screen. Drop affected items and save before changing dimensions.", risky))
                return;

            File backup = BackupManager.ensureBackup(archive);
            ExecuteChanges.run(archive, changed);
            Launcher.refreshBaseViews(archive);
            description.setText(sectionName + " changes applied. Backup: " + backup.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), sectionName + " apply error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isInventoryDimension(Option option) {
        String name = option.getOptionName().toUpperCase(java.util.Locale.ROOT);
        return option instanceof InventoryXOption || name.contains(" WIDTH") || name.contains(" HEIGHT");
    }

    private void updateApplyButton() {
        apply.setEnabled(archive != null && invalidValues.isEmpty() && !dirtyOptions.isEmpty());
    }
}
