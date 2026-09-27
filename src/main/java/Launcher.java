import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class Launcher {
    private static final ArrayList<BaseEditPanel> baseEditPanels = new ArrayList<BaseEditPanel>();
    private static DLCEditorPanel dlcPanel;
    private static BaseFieldsPanel baseFieldsPanel;
    private static JLabel installStatus;
    private static JButton restoreOriginalsButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("DXMD Archive Editor Pro v0.6.13");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout(6, 6));

                JPanel installBar = new JPanel(new BorderLayout(6, 6));
                JButton chooseExe = new JButton("Select DXMD.exe");
                restoreOriginalsButton = new JButton("Restore Editor Fields to Original");
                restoreOriginalsButton.setEnabled(false);
                JPanel installButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                installButtons.add(chooseExe);
                installButtons.add(restoreOriginalsButton);
                installStatus = new JLabel("Select DXMD.exe to auto-detect the base game and installed DLC archives.");
                installBar.add(installButtons, BorderLayout.WEST);
                installBar.add(installStatus, BorderLayout.CENTER);
                frame.add(installBar, BorderLayout.NORTH);

                BaseEditPanel weaponStats = new BaseEditPanel("Weapon Stats",
                        "Confirmed base-game weapon and ammunition controls. Published weapon-reference values are used for research only and are not shown as an application reference tab.",
                        BaseOptionCatalog.weaponStats());
                BaseEditPanel playerStats = new BaseEditPanel("Player Stats",
                        "Confirmed player-energy and augmentation controls.", BaseOptionCatalog.playerStats());
                BaseEditPanel inventoryStats = new BaseEditPanel("Inventory Stats",
                        "Confirmed inventory dimensions and non-ammunition stack controls. Dimension edits can be unsafe when affected items already exist in a save.",
                        BaseOptionCatalog.inventoryStats());
                BaseEditPanel economyCrafting = new BaseEditPanel("Economy & Crafting",
                        "Confirmed store-price and crafting-cost controls.", BaseOptionCatalog.economyCrafting());
                XPRewardPanel xpRewards = new XPRewardPanel();

                baseEditPanels.add(weaponStats);
                baseEditPanels.add(playerStats);
                baseEditPanels.add(inventoryStats);
                baseEditPanels.add(economyCrafting);
                baseEditPanels.add(xpRewards);

                baseFieldsPanel = new BaseFieldsPanel();
                dlcPanel = new DLCEditorPanel();

                JTabbedPane tabs = new JTabbedPane();
                tabs.addTab("Weapon Stats", weaponStats);
                tabs.addTab("Player Stats", playerStats);
                tabs.addTab("Inventory Stats", inventoryStats);
                tabs.addTab("Economy & Crafting", economyCrafting);
                tabs.addTab("XP Rewards", xpRewards);
                tabs.addTab("Base Fields", baseFieldsPanel);
                tabs.addTab("DLC Fields", dlcPanel);
                frame.add(tabs, BorderLayout.CENTER);

                chooseExe.addActionListener(e -> chooseGameExe(frame));
                restoreOriginalsButton.addActionListener(e -> restoreInstalledOriginalFields(frame));
                frame.setResizable(true);
                frame.setSize(1120, 840);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                SwingUtilities.invokeLater(() -> autoDetectFromApplication(frame));
            } catch (Throwable t) {
                t.printStackTrace();
                String msg = t.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = t.getClass().getName();
                JOptionPane.showMessageDialog(null, "DXMD Archive Editor Pro could not start:\n" + msg,
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void chooseGameExe(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Deus Ex: Mankind Divided - DXMD.exe");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("DXMD executable (*.exe)", "exe"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        openGameOrArchive(chooser.getSelectedFile(), parent);
    }

    public static void openGameOrArchive(File selected, Component parent) {
        if (selected == null || !selected.isFile()) {
            JOptionPane.showMessageDialog(parent, "Please select DXMD.exe or a .archive file.", "File selection error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String name = selected.getName();
        if (name.equalsIgnoreCase("DXMD.exe")) {
            try { applyLocatedGame(GameLocator.locate(selected), false); }
            catch (Exception ex) { JOptionPane.showMessageDialog(parent, ex.getMessage(), "Game detection error", JOptionPane.ERROR_MESSAGE); }
            return;
        }
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".archive")) {
            try { applyLocatedGame(GameLocator.locateFromHint(selected), false); return; }
            catch (Exception ignored) {
                if (name.equalsIgnoreCase("Game.layer.1.all.archive")) {
                    loadBaseArchiveEverywhere(selected);
                    installStatus.setText("Manual base archive: " + selected.getAbsolutePath());
                    return;
                }
            }
            JOptionPane.showMessageDialog(parent,
                    "That archive is not inside a recognized DXMD installation, so the editor will not guess which nearby copy to use.\nSelect DXMD.exe, or select the exact Game.layer.1.all.archive for manual testing.",
                    "Could not resolve game archive", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(parent, "Please select DXMD.exe or a .archive file.\nSelected: " + selected.getAbsolutePath(),
                "Unsupported file", JOptionPane.ERROR_MESSAGE);
    }

    private static void autoDetectFromApplication(Component parent) {
        GameLocator.Result result = GameLocator.locateFromApplication();
        if (result != null) applyLocatedGame(result, true);
    }

    private static void applyLocatedGame(GameLocator.Result result, boolean bootAutoDetected) {
        int installed = 0;
        for (File file : result.dlcArchives.values()) if (file != null) installed++;
        installStatus.setText((bootAutoDetected ? "Auto-detected game: " : "Game: ") + result.gameRoot.getAbsolutePath()
                + "   |   DLC packs found: " + installed + "/" + result.dlcArchives.size());
        if (result.baseArchive != null && result.baseArchive.isFile()) loadBaseArchiveEverywhere(result.baseArchive);
        else clearBaseArchiveEverywhere("Base archive not found in the validated game runtime folder.");
        dlcPanel.setDetectedArchives(result.dlcArchives, result.gameRoot);
        restoreOriginalsButton.setEnabled(result.baseArchive != null && result.baseArchive.isFile());
    }

    private static void loadBaseArchiveEverywhere(File archive) {
        for (BaseEditPanel panel : baseEditPanels) panel.loadArchive(archive);
        if (baseFieldsPanel != null) baseFieldsPanel.loadArchive(archive);
    }

    private static void clearBaseArchiveEverywhere(String message) {
        for (BaseEditPanel panel : baseEditPanels) panel.clearArchive(message);
        if (baseFieldsPanel != null) baseFieldsPanel.clearArchive(message);
    }

    private static void restoreInstalledOriginalFields(Component parent) {
        if (baseFieldsPanel == null) return;
        File base = baseFieldsPanel.getSelectedFile();
        if (base == null || !base.isFile()) {
            JOptionPane.showMessageDialog(parent, "No base archive is loaded.", "Restore", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(parent,
                "Restore every editor-supported field in the base archive and every detected DLC archive to the embedded clean-game values?\n\nThis does NOT replace whole archives and will leave unrelated modded bytes alone.\nA .bak of each file is created first if one does not already exist.",
                "Restore editor fields to original", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            ArchiveRestore.restoreBaseEditorFields(base);
            dlcPanel.restoreDetectedOriginalFields();
            refreshBaseViews(base);
            JOptionPane.showMessageDialog(parent, "Editor-supported fields were restored to their embedded original values.\nExisting .bak files were preserved.",
                    "Restore complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Restore error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void refreshBaseViews(File archive) {
        if (archive != null) loadBaseArchiveEverywhere(archive);
    }
}
