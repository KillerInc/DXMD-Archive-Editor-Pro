import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentListener;

import options.BooleanOption;
import options.ByteOption;
import options.FloatOption;
import options.InventoryXOption;
import options.NumericOption;
import options.Option;
import options.ShortOption;

public class MainGUI extends JPanel 
{    
    private ArrayList<Option> optionData;
    private ArrayList<JComponent> entryValues;
    private ArrayList<JLabel> entryLabels;
    private ArrayList<JTextField> invalidValues;
    
    private int valueEntry_x, valueEntry_y;
    
    private JPanel scrollPanel;
    private JButton btn_Select, btn_Default, btn_File, btn_Apply;   
    private JLabel lbl_gameAddress;
    private JScrollPane scrollPane;
    private JTextField jtf_FileAddress;     
    private JTextArea descTextArea;
    
    private void prepareForRun() throws IOException
    {
        String gameFileLocation = jtf_FileAddress.getText();
        
        for (int i = 0; i < optionData.size(); i++)
        {
            Option option = optionData.get(i);
            if (option instanceof FloatOption)
                ((FloatOption) option).setNewValue(Float.parseFloat(((JTextField) entryValues.get(i)).getText()));
            else if (option instanceof NumericOption)
                ((NumericOption) option).setNewValue(Integer.parseInt(((JTextField) entryValues.get(i)).getText()));
            else
                ((BooleanOption) option).setNewValue(((JCheckBox) entryValues.get(i)).isSelected());
        }
        
        ArrayList<String> risky = new ArrayList<String>();
        for (Option option : optionData) {
            if (option instanceof InventoryXOption) {
                NumericOption n = (NumericOption) option;
                if (n.getSpecificValue(2) != n.getSpecificValue(0)) risky.add(option.getOptionName());
            }
        }
        if (!risky.isEmpty() && !RiskWarning.confirm(this, "Risky Inventory Dimension Edit",
                "These controls change weapon inventory dimensions. Existing saves may remember the previous item dimensions. If an affected weapon is already in inventory, changing its size can make that save unusable or crash the inventory screen.", risky)) return;

        File archive = new File(gameFileLocation);
        BackupManager.ensureBackup(archive);
        ExecuteChanges.run(archive, optionData);
        Launcher.refreshBaseViews(archive);
    }
    
    public MainGUI()
    {
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        optionData = new ArrayList<Option>();
        entryValues = new ArrayList<JComponent>();
        entryLabels = new ArrayList<JLabel>();
        invalidValues = new ArrayList<JTextField>();
        fillOptionData();

        // Header / archive selector. BorderLayout lets the path field grow with the window.
        JPanel header = new JPanel(new BorderLayout(8, 8));
        JPanel titleRow = new JPanel(new BorderLayout(8, 8));
        lbl_gameAddress = new JLabel("Game.layer.1.all.archive (auto-detected from DXMD.exe or selected manually):");
        lbl_gameAddress.setFont(new Font("Courier New", Font.BOLD, 18));
        titleRow.add(lbl_gameAddress, BorderLayout.CENTER);

        btn_Select = new JButton("Select Archive");
        btn_Select.setFont(new Font("Courier New", Font.BOLD, 16));
        btn_Select.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select DXMD.exe or Game.layer.1.all.archive");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "DXMD executable / archives (*.exe, *.archive)", "exe", "archive"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION)
                Launcher.openGameOrArchive(fileChooser.getSelectedFile(), this);
        });
        titleRow.add(btn_Select, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        jtf_FileAddress = new JTextField();
        jtf_FileAddress.setEditable(false);
        jtf_FileAddress.setBackground(Color.WHITE);
        jtf_FileAddress.setFont(new Font("Courier New", Font.PLAIN, 13));
        header.add(jtf_FileAddress, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Option grid: two responsive columns. Labels take any extra horizontal space.
        scrollPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 5, 4, 5);
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        descTextArea = new JTextArea();
        descTextArea.setFont(new Font("Courier New", Font.PLAIN, 16));
        descTextArea.setLineWrap(true);
        descTextArea.setWrapStyleWord(true);
        descTextArea.setFocusable(false);
        descTextArea.setRows(3);

        for (int i = 0; i < optionData.size(); i++) {
            Option currentOption = optionData.get(i);
            int pair = i % 2;
            int row = i / 2;
            int valueCol = pair * 2;
            int labelCol = valueCol + 1;

            JComponent valueComponent;
            if (currentOption instanceof NumericOption || currentOption instanceof FloatOption) {
                JTextField tempTF = new JTextField();
                tempTF.setFont(new Font("Courier New", Font.BOLD, 18));
                tempTF.setPreferredSize(new Dimension(95, 34));
                tempTF.setEnabled(false);
                tempTF.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {}
                    public void removeUpdate(DocumentEvent e) { checkOptionInputValidity(tempTF, currentOption); }
                    public void insertUpdate(DocumentEvent e) { checkOptionInputValidity(tempTF, currentOption); }
                });
                valueComponent = tempTF;
            } else {
                JCheckBox tempCB = new JCheckBox();
                tempCB.setEnabled(false);
                valueComponent = tempCB;
            }

            java.awt.event.MouseAdapter hover = new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) { descTextArea.setText(currentOption.getOptionDesc()); }
                public void mouseExited(java.awt.event.MouseEvent evt) { descTextArea.setText(""); }
            };
            valueComponent.addMouseListener(hover);

            gc.gridy = row;
            gc.gridx = valueCol;
            gc.weightx = 0.0;
            gc.fill = GridBagConstraints.NONE;
            scrollPanel.add(valueComponent, gc);
            entryValues.add(valueComponent);

            JLabel tempLbl = new JLabel(currentOption.getOptionName());
            tempLbl.setFont(new Font("Courier New", Font.PLAIN, 17));
            tempLbl.setForeground(Color.GRAY);
            tempLbl.addMouseListener(hover);
            gc.gridx = labelCol;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            scrollPanel.add(tempLbl, gc);
            entryLabels.add(tempLbl);
        }

        // Keep rows pinned to the top when the window is taller than the option list.
        gc.gridx = 0;
        gc.gridy = (optionData.size() + 1) / 2;
        gc.gridwidth = 4;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        scrollPanel.add(javax.swing.Box.createGlue(), gc);

        scrollPane = new JScrollPane(scrollPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        add(scrollPane, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(new JScrollPane(descTextArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout(8, 8));
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btn_Default = new JButton("Default Values");
        btn_Default.setFont(new Font("Courier New", Font.BOLD, 16));
        btn_Default.setEnabled(false);
        btn_Default.addActionListener(actionEvent -> applyPreset("Default Values"));
        leftButtons.add(btn_Default);

        btn_File = new JButton("Current File Values");
        btn_File.setFont(new Font("Courier New", Font.BOLD, 16));
        btn_File.setEnabled(false);
        btn_File.addActionListener(actionEvent -> applyPreset("Current Values"));
        leftButtons.add(btn_File);
        buttons.add(leftButtons, BorderLayout.WEST);

        btn_Apply = new JButton("Apply");
        btn_Apply.setFont(new Font("Courier New", Font.BOLD, 20));
        btn_Apply.setPreferredSize(new Dimension(150, 44));
        btn_Apply.addActionListener(actionEvent -> {
            try { prepareForRun(); }
            catch (IOException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Apply error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(btn_Apply, BorderLayout.EAST);
        lower.add(buttons, BorderLayout.SOUTH);
        add(lower, BorderLayout.SOUTH);
        handleApplyButtonEnabled();
    }

    public void loadArchive(File archive)
    {
        if (archive == null || !archive.isFile()) { clearArchive("Base archive not found."); return; }
        if (!archive.getName().equalsIgnoreCase("Game.layer.1.all.archive")) {
            clearArchive("Wrong base archive selected: " + archive.getName());
            javax.swing.JOptionPane.showMessageDialog(this,
                "The Base Game tab only accepts Game.layer.1.all.archive.\nSelected: " + archive.getAbsolutePath(),
                "Wrong archive", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        try
        {
            jtf_FileAddress.setText(archive.getAbsolutePath());
            FileAnalyzer.analyze(archive, optionData);
            enableOptions();
            applyPreset("Current Values");
            descTextArea.setText("Loaded base archive: " + archive.getAbsolutePath()
                + "\nSize: " + archive.length() + " bytes"
                + "\nFile identity: " + BackupManager.identify(archive)
                + (BackupManager.hasBackup(archive) ? "\nBackup: " + BackupManager.backupFile(archive).getAbsolutePath() : ""));
            handleApplyButtonEnabled();
        }
        catch (IOException e)
        {
            clearArchive("Could not load base archive: " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Load error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearArchive(String message)
    {
        jtf_FileAddress.setText("");
        for (JComponent c : entryValues) c.setEnabled(false);
        for (JLabel l : entryLabels) l.setForeground(Color.GRAY);
        btn_Default.setEnabled(false);
        btn_File.setEnabled(false);
        descTextArea.setText(message);
        handleApplyButtonEnabled();
    }

    private void checkOptionInputValidity(JTextField tempTF, Option option)
    {
        if (option instanceof FloatOption)
        {
            boolean valid = false;
            try
            {
                float v = Float.parseFloat(tempTF.getText());
                FloatOption fo = (FloatOption) option;
                valid = Float.isFinite(v) && v >= fo.getMinValue() && v <= fo.getMaxFloatValue();
            }
            catch (Exception ignored) {}
            setFieldValidity(tempTF, valid);
        }
        else
            checkInputValidity(tempTF, ((NumericOption) option).getMaxValue());
    }

    private void setFieldValidity(JTextField tempTF, boolean valid)
    {
        if (!valid) { if (!invalidValues.contains(tempTF)) invalidValues.add(tempTF); tempTF.setForeground(Color.RED); }
        else { invalidValues.remove(tempTF); tempTF.setForeground(Color.BLACK); }
        handleApplyButtonEnabled();
    }

    private void checkInputValidity(JTextField tempTF, int optionMax)
    {        
        if (!isEntryValid(tempTF.getText(), optionMax))
        {
            if (!invalidValues.contains(tempTF))
            {
                tempTF.setForeground(Color.RED);
                invalidValues.add(tempTF);
            }
        }
        else
        {
            if (invalidValues.contains(tempTF))
            {
                invalidValues.remove(tempTF);
                tempTF.setForeground(Color.BLACK);
            }                    
        } 
        handleApplyButtonEnabled();
    }
    
    public void showDoneMessage()
    {
        descTextArea.setText("Done");
    }
    
    private void incrementCheckBoxLocation()
    {        
        if (valueEntry_x == 10)
            valueEntry_x = 390;
        else
        {
            valueEntry_x = 10;
            valueEntry_y += 40;        
        }
    }
    
    private void enableOptions()
    {
        for (JComponent tempField : entryValues)
        {
            tempField.setEnabled(true);
        }
        
        for (JLabel tempLabel : entryLabels)
        {
            tempLabel.setForeground(Color.BLACK);
        }
        
        btn_Default.setEnabled(true);
        btn_File.setEnabled(true);
    }    
    
    private void applyPreset(String preset)
    { 
        int value;
        if (preset.equals("Current Values"))
            value = 0;
        else
            value = 1;
        
        for (int i = 0; i < entryValues.size(); i++)
        {
            if (optionData.get(i) instanceof FloatOption)
                ((JTextField) entryValues.get(i)).setText(Float.toString(((FloatOption) optionData.get(i)).getSpecificValue(value)));
            else if (optionData.get(i) instanceof NumericOption)
                ((JTextField) entryValues.get(i)).setText(Integer.toString(((NumericOption) optionData.get(i)).getSpecificValue(value)));
            else // BooleanOption then
                ((JCheckBox) entryValues.get(i)).setSelected(((BooleanOption) optionData.get(i)).getSpecificValue(value));
        }        
    }
    
    // Used to know whether a specific entry is valid for the specific option it is trying to change
    private boolean isEntryValid(String entry, int optionMax)
    {
        try
        {
            int intVal = Integer.parseInt(entry);
            if (intVal >= 0 && intVal <= optionMax)
                return true;
            else
                return false;
        }
        catch (Exception e)
        {
            return false;
        }
    }
    
    private void handleApplyButtonEnabled()
    {
        if (invalidValues.size() > 0 || jtf_FileAddress.getText().isEmpty())
            btn_Apply.setEnabled(false);
        else
            btn_Apply.setEnabled(true);
    }
    
    private void fillOptionData()
    {
        // Note: final parameter given should be the default value
        optionData.add(new ShortOption(setUpLongAL(5401405), "XP for lvl. 1 Hack", "Sets the amount of XP received from hacking a level 1 device. Range:0-65535", 25));
        optionData.add(new ShortOption(setUpLongAL(5401429), "XP for lvl. 2 Hack", "Sets the amount of XP received from hacking a level 2 device. Range:0-65535", 50));
        optionData.add(new ShortOption(setUpLongAL(5401453), "XP for lvl. 3 Hack", "Sets the amount of XP received from hacking a level 3 device. Range:0-65535", 75));
        optionData.add(new ShortOption(setUpLongAL(5401477), "XP for lvl. 4 Hack", "Sets the amount of XP received from hacking a level 4 device. Range:0-65535", 100));
        optionData.add(new ShortOption(setUpLongAL(5401501), "XP for lvl. 5 Hack", "Sets the amount of XP received from hacking a level 5 device. Range:0-65535", 125));
        optionData.add(new ShortOption(setUpLongAL(5401525), "XP for First Try Hack", "Sets the amount of XP received from hacking a device on your first try. Range:0-65535", 5));
        optionData.add(new FloatOption(setUpLongAL(6577693), "Energy Auto-Regen Limit", "Sets the energy value that automatic regeneration can recover to. Clean game value is 35.0; independently isolated by multiple mod comparisons. Range:0-1000", 35.0f, 0.0f, 1000.0f));
        optionData.add(new FloatOption(setUpLongAL(4570013), "Biocell Energy Gain", "Sets the energy restored by a Biocell. Clean game value is 85.0; No Health Regen Variety isolates 42.5 and Variety B isolates 28.0. Range:0-1000", 85.0f, 0.0f, 1000.0f));
        
        optionData.add(new ShortOption(setUpLongAL(5399013, 5403213, 5404333, 5405805), "XP for Headshot", "Sets the amount of XP received from a headshot kill. Range:0-65535", 10));
        optionData.add(new ShortOption(setUpLongAL(5398989, 5403189, 5404309, 5405781), "XP for Non-Lethal", "Sets the amount of XP received from a non-lethal takedown (merciful soul). Range:0-65535", 20));
        
        optionData.add(new ShortOption(setUpLongAL(7736149), "Praxis Shop Cost", "Sets the amount credits a Praxis kit will cost in a store. Range:0-65535", 10000));
        optionData.add(new ShortOption(setUpLongAL(4570325), "Biocell Shop Cost", "Sets the amount credits a biocell will cost in a store. Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(5796261), "Hypostim Shop Cost", "Sets the amount credits a hypostim will cost in a store. Range:0-65535", 150));
        optionData.add(new ShortOption(setUpLongAL(4913021), "Painkiller Shop Cost", "Sets the amount credits a painkiller bottle will cost in a store. Range:0-65535", 50));
        optionData.add(new ShortOption(setUpLongAL(6119021), "Multitool Shop Cost", "Sets the amount credits a Multitool will cost in a store. Range:0-65535", 800));
        optionData.add(new ShortOption(setUpLongAL(7558933), "Typhoon Ammo Shop Cost", "Sets the amount credits a single Typhoon ammo will cost in a store. This is multiplied by 3 or 5 for the packs. Range:0-65535", 500));
        optionData.add(new ShortOption(setUpLongAL(5852637), "Tesla Ammo Shop Cost", "Sets the amount credits a single Tesla ammo will cost in a store. This is multiplied by 8 for the pack. Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(5853429), "Nanoblade Ammo Shop Cost", "Sets the amount credits a single Nanoblade ammo will cost in a store. This is multiplied by 8 for the pack. Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(5865149), "Weapon Part Shop Cost", "Sets the amount credits a single weapon part will cost in a store. This is multiplied for the pack. Range:0-65535", 5));
        
        optionData.add(new ShortOption(setUpLongAL(6789165), "Reveal Shop Cost", "Sets the amount credits a Reveal software will cost in a store. Range:0-65535", 250));
        optionData.add(new ShortOption(setUpLongAL(6789837), "Stealth Shop Cost", "Sets the amount credits a Stealth software will cost in a store. Range:0-65535", 250));
        optionData.add(new ShortOption(setUpLongAL(6790509), "Nuke Shop Cost", "Sets the amount credits a Nuke software will cost in a store. Range:0-65535", 150));
        optionData.add(new ShortOption(setUpLongAL(6791181), "Datascan Shop Cost", "Sets the amount credits a Datascan software will cost in a store. Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(6791853), "Stop! Shop Cost", "Sets the amount credits a Stop! software will cost in a store. Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(6792525), "Overclock Shop Cost", "Sets the amount credits an Overclock software will cost in a store. Range:0-65535", 200));
        
        optionData.add(new InventoryXOption(setUpLongAL(6784805), "Sniper Width", "Sets the width of the sniper rifle in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 7));
        optionData.add(new InventoryXOption(setUpLongAL(4936557), "Tranquilizer Rifle Width", "Sets the width of the tranquilizer rifle in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 7));
        optionData.add(new InventoryXOption(setUpLongAL(4987389, 6754613), "Shotgun Width", "Sets the width of the shotgun in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 5));
        optionData.add(new InventoryXOption(setUpLongAL(4967181), "Grenade Launcher Width", "Sets the width of the grenade launcher in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 4));
        optionData.add(new InventoryXOption(setUpLongAL(4940389), "Machine Pistol Width", "Sets the width of the machine pistol in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 4));
        optionData.add(new InventoryXOption(setUpLongAL(4951797), "Battle Rifle Width", "Sets the width of the battle rifle in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 6));
        optionData.add(new InventoryXOption(setUpLongAL(4886637), "Combat Rifle Width", "Sets the width of the combat rifle in the inventory (number of tiles). Range:0-16 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 5));
        optionData.add(new ByteOption(setUpLongAL(4281877, 4282621, 4283365, 4284109), "Grenade Launcher Ammo Height", "Sets the inventory height for the four grenade-launcher ammo types. Clean game value is 2 tiles. Range:0-255 WARNING: changing an item dimension while that item exists in a save can make the save unusable or crash the inventory. Drop affected items and save before changing dimensions.", 2));
        optionData.add(new ByteOption(setUpLongAL(4937661), "Tranquilizer Rifle Magazine", "Sets the base tranquilizer rifle magazine capacity. Clean game value is 6. Range:0-255", 6));
        optionData.add(new ByteOption(setUpLongAL(4981373), "Lancer Rifle Magazine", "Sets the base Lancer rifle magazine capacity. Clean game value is 3. Range:0-255", 3));
        
        optionData.add(new ShortOption(setUpLongAL(7562693), "Typhoon Ammo Crafting Cost", "Sets the amount weapons parts needed to craft a 3-pack of Typhoon ammo. Range:0-65535", 75));
        optionData.add(new ShortOption(setUpLongAL(7557853), "Mine Template Crafting Cost", "Sets the amount weapons parts needed to craft a mine template. Range:0-65535", 75));
        optionData.add(new ShortOption(setUpLongAL(5865517), "Biocell Crafting Cost", "Sets the amount weapons parts needed to craft a biocell. Range:0-65535", 120));
        optionData.add(new ShortOption(setUpLongAL(6119341), "Multi-Tool Crafting Cost", "Sets the amount weapons parts needed to craft a Multi-Tool. Range:0-65535", 120));
        optionData.add(new ShortOption(setUpLongAL(6172733), "Nanoblade Crafting Cost", "Sets the amount weapons parts needed to craft a Nanoblade ammo pack. Range:0-65535", 75));
        optionData.add(new ShortOption(setUpLongAL(7441693), "Tesla Ammo Crafting Cost", "Sets the amount weapons parts needed to craft a Tesla ammo pack. Range:0-65535", 75));
        
        optionData.add(new ShortOption(setUpLongAL(4264429, 4265549, 4267085, 4268245, 4269245, 4270501, 4285101, 4286237, 4287013, 4288053, 4288885, 4290013, 6615853, 6616941, 6966957, 7525853), "Ammo Stack", "Sets the max inventory stack size of weapon ammo (grenade launcher excluded). Range:0-65535", 200));
        optionData.add(new ShortOption(setUpLongAL(4282117, 4282861, 4283605, 4284349), "Grenade Ammo Stack", "Sets the max inventory stack size of grenade launcher ammo. Range:0-65535", 10));
        optionData.add(new ShortOption(setUpLongAL(4907021, 4908021, 5202949, 5204013, 5709093, 5710741, 5711741, 5723317, 5862917, 6783125, 6784101), "Grenade Stack", "Sets the max inventory stack size of thrown grenades and mines. Range:0-65535", 10));
        optionData.add(new ShortOption(setUpLongAL(4570277), "Biocell Stack", "Sets the max inventory stack size of Biocells. Range:0-65535", 25));
        optionData.add(new ShortOption(setUpLongAL(4912973), "Painkiller Stack", "Sets the max inventory stack size of painkillers. Range:0-65535", 25));
        optionData.add(new ShortOption(setUpLongAL(5796213), "Hypostim Stack", "Sets the max inventory stack size of hypostims. Range:0-65535", 25));
        optionData.add(new ShortOption(setUpLongAL(5865197), "Weapon Parts Stack", "Sets the max inventory stack size of Weapon Parts. Clean game value is 999. Range:0-65535", 999));
        
        optionData.add(new FloatOption(setUpLongAL(7413189), "Takedown Energy Cost", "Sets the energy consumed by a takedown. Clean game value is 33.0; set to 0 for no takedown energy cost. Variety Pack isolates this field at 80.0. Range:0-1000", 33.0f, 0.0f, 1000.0f));
        optionData.add(new BooleanOption(setUpLongAL(6611045, 7589029, 7589797, 7704685, 7718621, 7719573, 7721117, 7722581, 7723565, 7727101),  setUpShortAL(10, 10, 10, 10, 10, 10, 10, 10, 10, 10), setUpShortAL(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 
                "Augs Non-Experimental", "Makes all augmentations non-experimental, removing need for neuroplasticity calibrator.", false));
    }
    
    private ArrayList<Long> setUpLongAL(long ... input)
    {
        ArrayList<Long> tempList = new ArrayList<Long>();
        for (Long l : input)
            tempList.add(l);
        return tempList;
    }
    
    private ArrayList<Short> setUpShortAL(int ... input)
    {
        ArrayList<Short> tempList = new ArrayList<Short>();
        for (int l : input)
            tempList.add((short) l);
        return tempList;
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
    }
    
}
