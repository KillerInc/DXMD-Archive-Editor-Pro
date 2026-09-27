import javax.swing.*;
import javax.swing.Timer;
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

    private record PendingLoad(GameLocator.Result located, File manualBase, String manualStatus, boolean bootAutoDetected) {}
    private record LoadingUi(JDialog dialog, JLabel message, JProgressBar bar, Timer animation) {}
    @FunctionalInterface private interface ProgressSink { void update(int value,String message); }
    @FunctionalInterface private interface LoadResolver { PendingLoad resolve(ProgressSink progress) throws Exception; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UiTheme.install();
                JFrame frame = new JFrame("DXMD Archive Editor Pro v0.7.6");
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
        if(!name.equalsIgnoreCase("DXMD.exe")&&!name.toLowerCase(Locale.ROOT).endsWith(".archive")){
            JOptionPane.showMessageDialog(parent,"Please select DXMD.exe or a .archive file.\nSelected: "+selected.getAbsolutePath(),"Unsupported file",JOptionPane.ERROR_MESSAGE);return;
        }

        beginLoad(parent, progress -> {
            progress.update(8,"Resolving game files...");
            PendingLoad pending;
            if(name.equalsIgnoreCase("DXMD.exe")){
                GameLocator.Result result=GameLocator.locate(selected);
                pending=new PendingLoad(result,null,null,false);
            }else{
                try{
                    GameLocator.Result result=GameLocator.locateFromHint(selected);
                    pending=new PendingLoad(result,null,null,false);
                }catch(Exception ignored){
                    if(name.equalsIgnoreCase("Game.layer.1.all.archive"))
                        pending=new PendingLoad(null,selected,"Manual base archive: "+selected.getAbsolutePath(),false);
                    else throw new java.io.IOException("That archive is not inside a recognized DXMD installation. Select DXMD.exe, or select the exact Game.layer.1.all.archive for manual testing.");
                }
            }
            File base=pending.located!=null?pending.located.baseArchive:pending.manualBase;
            prewarmBaseResearch(base,progress);
            progress.update(94,"Preparing editor views...");
            return pending;
        });
    }

    private static void autoDetectFromApplication(Component parent){
        GameLocator.Result result=GameLocator.locateFromApplication();
        if(result==null)return;
        beginLoad(parent,progress->{
            progress.update(12,"Auto-detected DXMD. Preparing archives...");
            prewarmBaseResearch(result.baseArchive,progress);
            progress.update(94,"Preparing editor views...");
            return new PendingLoad(result,null,null,true);
        });
    }

    private static void prewarmBaseResearch(File base, ProgressSink progress) throws Exception {
        if(base==null||!base.isFile())return;
        progress.update(28,"Reading archive directory...");
        ArchiveResourceIndex.load(base);
        progress.update(48,"Indexing internal resources...");
        progress.update(58,"Scanning readable archive identifiers...");
        ArchiveContextResolver.resolve(base,BaseResearchProfiles.get().fields);
        progress.update(76,"Loading HeaderLib logical-resource structure...");
        LogicalResourceCatalog.prewarm();
        progress.update(88,"Mapping research fields to logical resources...");
    }

    private static void beginLoad(Component parent, LoadResolver resolver){
        LoadingUi ui=createLoadingUi(parent);
        SwingWorker<PendingLoad,String> worker=new SwingWorker<>(){
            @Override protected PendingLoad doInBackground() throws Exception {
                return resolver.resolve((value,message)->{setProgress(Math.max(0,Math.min(100,value)));publish(message);});
            }
            @Override protected void process(java.util.List<String> chunks){
                if(!chunks.isEmpty())ui.message.setText(chunks.get(chunks.size()-1));
            }
            @Override protected void done(){
                boolean success=false;
                try{
                    PendingLoad pending=get();
                    ui.animation.stop();
                    ui.bar.setIndeterminate(false);
                    ui.bar.setValue(96);
                    ui.bar.setString("Finalizing...");
                    ui.message.setText("Populating editor...");
                    if(pending.located!=null)applyLocatedGame(pending.located,pending.bootAutoDetected);
                    else if(pending.manualBase!=null){loadBaseArchiveEverywhere(pending.manualBase);researchInspector.setDetectedArchives(pending.manualBase,loadedDlcArchives);installStatus.setText(pending.manualStatus);}
                    ui.bar.setValue(100);
                    ui.bar.setString("Ready");
                    ui.message.setText("Archive ready");
                    success=true;
                }catch(Exception ex){
                    Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    JOptionPane.showMessageDialog(parent,cause.getMessage()==null?cause.toString():cause.getMessage(),"Archive load error",JOptionPane.ERROR_MESSAGE);
                }finally{
                    ui.animation.stop();
                    if(success){
                        Timer close=new Timer(180,e->ui.dialog.dispose());
                        close.setRepeats(false);
                        close.start();
                    }else ui.dialog.dispose();
                }
            }
        };
        worker.execute();
        ui.dialog.setVisible(true);
    }

    private static LoadingUi createLoadingUi(Component parent){
        Window owner=parent instanceof Window?(Window)parent:SwingUtilities.getWindowAncestor(parent);
        JDialog dialog=new JDialog(owner,"Loading Archive",Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel panel=new JPanel(new BorderLayout(10,10));panel.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));
        JLabel title=new JLabel("Loading Archive");title.setFont(new Font("Segoe UI",Font.BOLD,16));title.setForeground(UiTheme.TEXT);
        JLabel message=new JLabel("Preparing...");message.setForeground(UiTheme.MUTED);
        JProgressBar bar=new JProgressBar(0,100);
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("Working");
        bar.setForeground(UiTheme.ACCENT);
        bar.setBackground(UiTheme.FIELD);
        bar.setPreferredSize(new Dimension(390,22));
        final int[] dots={0};
        Timer animation=new Timer(260,e->{
            dots[0]=(dots[0]+1)%4;
            bar.setString("Working"+".".repeat(dots[0]));
        });
        animation.setCoalesce(true);
        animation.start();
        JPanel labels=new JPanel(new BorderLayout(4,4));labels.add(title,BorderLayout.NORTH);labels.add(message,BorderLayout.SOUTH);
        panel.add(labels,BorderLayout.NORTH);panel.add(bar,BorderLayout.CENTER);
        dialog.setContentPane(panel);dialog.pack();dialog.setResizable(false);dialog.setLocationRelativeTo(parent);UiTheme.apply(dialog);bar.setForeground(UiTheme.ACCENT);bar.setBackground(UiTheme.FIELD);
        return new LoadingUi(dialog,message,bar,animation);
    }

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
