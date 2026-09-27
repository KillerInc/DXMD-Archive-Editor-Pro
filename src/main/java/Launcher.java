import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.*;

public class Launcher {
    private static final ArrayList<BaseEditPanel> baseEditPanels = new ArrayList<>();
    private static ResearchInspectorPanel researchInspector;
    private static JLabel installStatus;
    private static JButton restoreOriginalsButton;
    private static File loadedBaseArchive;
    private static final LinkedHashMap<String,File> loadedDlcArchives=new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UiTheme.install();
                JFrame frame = new JFrame("DXMD Archive Editor Pro v0.7.3");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout(6, 6));

                JPanel installBar = new JPanel(new BorderLayout(6, 6));
                installBar.setBorder(BorderFactory.createEmptyBorder(7,8,3,8));
                JButton chooseExe = new JButton("Select DXMD.exe");
                restoreOriginalsButton = new JButton("Restore Confirmed Fields to Original");
                restoreOriginalsButton.setEnabled(false);
                JPanel installButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                installButtons.add(chooseExe); installButtons.add(restoreOriginalsButton);
                installStatus = new JLabel("Select DXMD.exe to auto-detect the base game and installed DLC archives.");
                installStatus.setForeground(UiTheme.MUTED);
                installBar.add(installButtons, BorderLayout.WEST); installBar.add(installStatus, BorderLayout.CENTER);
                frame.add(installBar, BorderLayout.NORTH);

                BaseEditPanel weaponStats = new BaseEditPanel("Weapon Stats","Confirmed base-game weapon and ammunition controls.",BaseOptionCatalog.weaponStats());
                BaseEditPanel playerStats = new BaseEditPanel("Player Stats","Confirmed player-energy and augmentation controls.", BaseOptionCatalog.playerStats());
                BaseEditPanel inventoryStats = new BaseEditPanel("Inventory Stats","Confirmed inventory dimensions and non-ammunition stack controls. Dimension edits can be unsafe when affected items already exist in a save.",BaseOptionCatalog.inventoryStats());
                BaseEditPanel economyCrafting = new BaseEditPanel("Economy & Crafting","Confirmed store-price and crafting-cost controls.", BaseOptionCatalog.economyCrafting());
                XPRewardPanel xpRewards = new XPRewardPanel();
                Collections.addAll(baseEditPanels,weaponStats,playerStats,inventoryStats,economyCrafting,xpRewards);
                researchInspector=new ResearchInspectorPanel();

                JTabbedPane tabs = new JTabbedPane();
                tabs.addTab("Weapon Stats", weaponStats); tabs.addTab("Player Stats", playerStats); tabs.addTab("Inventory Stats", inventoryStats);
                tabs.addTab("Economy & Crafting", economyCrafting); tabs.addTab("XP Rewards", xpRewards); tabs.addTab("Research Inspector", researchInspector);
                tabs.setSelectedComponent(researchInspector);
                frame.add(tabs, BorderLayout.CENTER);

                chooseExe.addActionListener(e -> chooseGameExe(frame)); restoreOriginalsButton.addActionListener(e -> restoreInstalledOriginalFields(frame));
                frame.setResizable(true); frame.setMinimumSize(new Dimension(1280,760)); frame.setSize(1500,900); frame.setLocationRelativeTo(null);
                UiTheme.apply(frame);
                installStatus.setForeground(UiTheme.MUTED);
                frame.setVisible(true);
                SwingUtilities.invokeLater(() -> autoDetectFromApplication(frame));
            } catch (Throwable t) {
                t.printStackTrace(); String msg=t.getMessage(); if(msg==null||msg.isBlank())msg=t.getClass().getName();
                JOptionPane.showMessageDialog(null,"DXMD Archive Editor Pro could not start:\n"+msg,"Startup error",JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void chooseGameExe(Component parent) {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Select Deus Ex: Mankind Divided - DXMD.exe");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); chooser.setAcceptAllFileFilterUsed(false); chooser.setFileFilter(new FileNameExtensionFilter("DXMD executable (*.exe)", "exe"));
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) openGameOrArchive(chooser.getSelectedFile(), parent);
    }

    public static void openGameOrArchive(File selected, Component parent) {
        if (selected == null || !selected.isFile()) {JOptionPane.showMessageDialog(parent,"Please select DXMD.exe or a .archive file.","File selection error",JOptionPane.ERROR_MESSAGE);return;}
        String name=selected.getName();
        if(name.equalsIgnoreCase("DXMD.exe")){try{applyLocatedGame(GameLocator.locate(selected),false);}catch(Exception ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Game detection error",JOptionPane.ERROR_MESSAGE);}return;}
        if(name.toLowerCase(Locale.ROOT).endsWith(".archive")){
            try{applyLocatedGame(GameLocator.locateFromHint(selected),false);return;}catch(Exception ignored){
                if(name.equalsIgnoreCase("Game.layer.1.all.archive")){loadBaseArchiveEverywhere(selected);researchInspector.setDetectedArchives(selected,loadedDlcArchives);installStatus.setText("Manual base archive: "+selected.getAbsolutePath());return;}
            }
            JOptionPane.showMessageDialog(parent,"That archive is not inside a recognized DXMD installation. Select DXMD.exe, or select the exact Game.layer.1.all.archive for manual testing.","Could not resolve game archive",JOptionPane.ERROR_MESSAGE);return;
        }
        JOptionPane.showMessageDialog(parent,"Please select DXMD.exe or a .archive file.\nSelected: "+selected.getAbsolutePath(),"Unsupported file",JOptionPane.ERROR_MESSAGE);
    }

    private static void autoDetectFromApplication(Component parent){GameLocator.Result result=GameLocator.locateFromApplication();if(result!=null)applyLocatedGame(result,true);}
    private static void applyLocatedGame(GameLocator.Result result, boolean bootAutoDetected){
        int installed=0;loadedDlcArchives.clear();loadedDlcArchives.putAll(result.dlcArchives);for(File f:result.dlcArchives.values())if(f!=null)installed++;
        installStatus.setText((bootAutoDetected?"Auto-detected game: ":"Game: ")+result.gameRoot.getAbsolutePath()+"   |   DLC packs found: "+installed+"/"+result.dlcArchives.size());
        if(result.baseArchive!=null&&result.baseArchive.isFile())loadBaseArchiveEverywhere(result.baseArchive);else clearBaseArchiveEverywhere("Base archive not found in the validated game runtime folder.");
        researchInspector.setDetectedArchives(result.baseArchive,result.dlcArchives);restoreOriginalsButton.setEnabled(result.baseArchive!=null&&result.baseArchive.isFile());
    }
    private static void loadBaseArchiveEverywhere(File archive){loadedBaseArchive=archive;for(BaseEditPanel p:baseEditPanels)p.loadArchive(archive);if(researchInspector!=null)researchInspector.setBaseArchive(archive);}
    private static void clearBaseArchiveEverywhere(String message){loadedBaseArchive=null;for(BaseEditPanel p:baseEditPanels)p.clearArchive(message);if(researchInspector!=null)researchInspector.setBaseArchive(null);}

    private static void restoreInstalledOriginalFields(Component parent){
        File base=loadedBaseArchive;if(base==null||!base.isFile()){JOptionPane.showMessageDialog(parent,"No base archive is loaded.","Restore",JOptionPane.WARNING_MESSAGE);return;}
        int answer=JOptionPane.showConfirmDialog(parent,"Restore every confirmed editor-supported field in the base archive and detected DLC archives to clean-game values?\n\nSuspected/research bytes and unrelated mod bytes are left unchanged. A .bak is created first if needed.","Restore confirmed fields",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(answer!=JOptionPane.YES_OPTION)return;
        try{ArchiveRestore.restoreBaseEditorFields(base);for(Map.Entry<String,File>e:loadedDlcArchives.entrySet())if(e.getValue()!=null&&e.getValue().isFile())ArchiveRestore.restoreDlcEditorFields(e.getValue(),DLCProfiles.get(e.getKey()));refreshBaseViews(base);if(researchInspector!=null)researchInspector.reloadCurrent();JOptionPane.showMessageDialog(parent,"Confirmed fields restored. Existing .bak files were preserved.","Restore complete",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Restore error",JOptionPane.ERROR_MESSAGE);}
    }
    public static void refreshBaseViews(File archive){if(archive!=null)loadBaseArchiveEverywhere(archive);if(researchInspector!=null)researchInspector.reloadCurrent();}
}
