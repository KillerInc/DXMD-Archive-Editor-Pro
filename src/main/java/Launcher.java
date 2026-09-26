import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class Launcher {
    private static MainGUI mainWindow;
    private static DLCEditorPanel dlcPanel;
    private static BaseResearchPanel baseResearchPanel;
    private static JLabel installStatus;
    private static JButton restoreOriginalsButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
            JFrame jf=new JFrame("DXMD Archive Editor Pro v0.6.8");
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.setLayout(new BorderLayout(6,6));

            JPanel installBar = new JPanel(new BorderLayout(6,6));
            JButton chooseExe = new JButton("Select DXMD.exe");
            restoreOriginalsButton = new JButton("Restore Editor Fields to Original");
            restoreOriginalsButton.setEnabled(false);
            JPanel installButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            installButtons.add(chooseExe);
            installButtons.add(restoreOriginalsButton);
            installStatus = new JLabel("Select DXMD.exe to auto-detect the base game and installed DLC archives.");
            installBar.add(installButtons, BorderLayout.WEST);
            installBar.add(installStatus, BorderLayout.CENTER);
            jf.add(installBar, BorderLayout.NORTH);

            mainWindow=new MainGUI();
            dlcPanel=new DLCEditorPanel();
            baseResearchPanel=new BaseResearchPanel();
            JTabbedPane tabs=new JTabbedPane();
            tabs.addTab("Base Game", mainWindow);
            tabs.addTab("Base Research", baseResearchPanel);
            tabs.addTab("DLC Mod Fields", dlcPanel);
            jf.add(tabs, BorderLayout.CENTER);

            chooseExe.addActionListener(e -> chooseGameExe(jf));
            restoreOriginalsButton.addActionListener(e -> restoreInstalledOriginalFields(jf));

            jf.setResizable(true); jf.setSize(1080,820); jf.setLocationRelativeTo(null); jf.setVisible(true);

            // Safe boot auto-detection: only succeeds when the JAR/classes location is
            // actually inside a validated DXMD directory tree. No sibling/nearby file scan.
            SwingUtilities.invokeLater(() -> autoDetectFromApplication(jf));
            } catch (Throwable t) {
                t.printStackTrace();
                String msg = t.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = t.getClass().getName();
                JOptionPane.showMessageDialog(null,
                        "DXMD Archive Editor Pro could not start:\n" + msg,
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void chooseGameExe(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Deus Ex: Mankind Divided - DXMD.exe");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("DXMD executable (*.exe)", "exe"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        openGameOrArchive(fc.getSelectedFile(), parent);
    }

    /**
     * Handles a file selected from either the top DXMD.exe picker or the Base Game
     * picker. Selecting DXMD.exe performs full installation discovery and then loads
     * Game.layer.1.all.archive automatically. Selecting an archive loads it directly.
     */
    public static void openGameOrArchive(File selected, Component parent) {
        if (selected == null || !selected.isFile()) {
            JOptionPane.showMessageDialog(parent, "Please select DXMD.exe or a .archive file.",
                    "File selection error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = selected.getName();
        if (name.equalsIgnoreCase("DXMD.exe")) {
            try {
                applyLocatedGame(GameLocator.locate(selected), false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(),
                        "Game detection error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".archive")) {
            // A user may click any of the many archives in runtime/DLC/runtime. Treat the
            // selected file as a location hint, resolve the validated game root, and then
            // silently open the exact base archive the editor requires.
            try {
                GameLocator.Result r = GameLocator.locateFromHint(selected);
                applyLocatedGame(r, false);
                return;
            } catch (Exception ignored) {
                // Manual/testing fallback: an exact base archive outside an installed game
                // may still be opened directly. Never guess from a differently named copy.
                if (name.equalsIgnoreCase("Game.layer.1.all.archive")) {
                    mainWindow.loadArchive(selected);
                    baseResearchPanel.loadArchive(selected);
                    installStatus.setText("Manual base archive: " + selected.getAbsolutePath());
                    return;
                }
            }

            JOptionPane.showMessageDialog(parent,
                    "That archive is not inside a recognized DXMD installation, so the editor will not guess which nearby copy to use.\n"
                    + "Select DXMD.exe, or select the exact Game.layer.1.all.archive for manual testing.",
                    "Could not resolve game archive", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                "Please select DXMD.exe or a .archive file.\nSelected: " + selected.getAbsolutePath(),
                "Unsupported file", JOptionPane.ERROR_MESSAGE);
    }

    private static void autoDetectFromApplication(Component parent) {
        GameLocator.Result r = GameLocator.locateFromApplication();
        if (r == null) return; // Silent by design when the JAR is not inside the game tree.
        applyLocatedGame(r, true);
    }

    private static void applyLocatedGame(GameLocator.Result r, boolean bootAutoDetected) {
        int installed = 0;
        for (File f : r.dlcArchives.values()) if (f != null) installed++;
        installStatus.setText((bootAutoDetected ? "Auto-detected game: " : "Game: ")
                + r.gameRoot.getAbsolutePath() + "   |   DLC packs found: "
                + installed + "/" + r.dlcArchives.size());

        if (r.baseArchive != null) {
            mainWindow.loadArchive(r.baseArchive);
            baseResearchPanel.loadArchive(r.baseArchive);
        } else {
            mainWindow.clearArchive("Base archive not found in the validated game runtime folder.");
            baseResearchPanel.clearArchive("Base archive not found.");
        }
        dlcPanel.setDetectedArchives(r.dlcArchives, r.gameRoot);
        if (restoreOriginalsButton != null) restoreOriginalsButton.setEnabled(r.baseArchive != null && r.baseArchive.isFile());
    }


    private static void restoreInstalledOriginalFields(Component parent) {
        if (mainWindow == null || baseResearchPanel == null) return;
        File base = baseResearchPanel.getSelectedFile();
        if (base == null || !base.isFile()) {
            JOptionPane.showMessageDialog(parent, "No base archive is loaded.", "Restore", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(parent,
                "Restore every editor-supported field in the base archive and every detected DLC archive to the embedded clean-game values?\n\n"
                + "This does NOT replace whole archives and will leave unrelated modded bytes alone.\n"
                + "A .bak of each file is created first if one does not already exist.",
                "Restore editor fields to original", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            ArchiveRestore.restoreBaseEditorFields(base);
            dlcPanel.restoreDetectedOriginalFields();
            refreshBaseViews(base);
            JOptionPane.showMessageDialog(parent,
                    "Editor-supported fields were restored to their embedded original values.\nExisting .bak files were preserved.",
                    "Restore complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Restore error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void refreshBaseViews(File archive) {
        if (archive == null) return;
        if (mainWindow != null) mainWindow.loadArchive(archive);
        if (baseResearchPanel != null) baseResearchPanel.loadArchive(archive);
    }

    public static void showDoneMessage(){ if(mainWindow!=null) mainWindow.showDoneMessage(); }
}
