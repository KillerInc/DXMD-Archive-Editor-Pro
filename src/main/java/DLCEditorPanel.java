import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;

public class DLCEditorPanel extends JPanel {
    private final JLabel installSummary=new JLabel("DLC detection not run yet.");
    private final JLabel status=new JLabel("Select DXMD.exe above to detect installed DLCs.");
    private final JTabbedPane packTabs=new JTabbedPane();
    private final LinkedHashMap<String,File> detected=new LinkedHashMap<>();
    private final LinkedHashMap<String,PackPanel> panels=new LinkedHashMap<>();
    private File gameRoot;

    public DLCEditorPanel(){
        setLayout(new BorderLayout(8,8));
        JPanel top=new JPanel(new BorderLayout(6,6));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        left.add(new JLabel("Detected DLCs:")); left.add(installSummary); top.add(left,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,2)); JButton manual=new JButton("Select Archive Manually"); JButton rescan=new JButton("Rescan"); buttons.add(manual);buttons.add(rescan);top.add(buttons,BorderLayout.EAST);
        JPanel north=new JPanel(); north.setLayout(new BoxLayout(north,BoxLayout.Y_AXIS)); north.add(top); north.add(makeLegend());
        add(north,BorderLayout.NORTH); add(packTabs,BorderLayout.CENTER); add(status,BorderLayout.SOUTH);
        manual.addActionListener(e->selectFile()); rescan.addActionListener(e->rescan());
    }

    private JPanel makeLegend(){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,1));
        p.add(new JLabel("Legend:"));
        JLabel known=new JLabel("Known / identified field"); known.setForeground(new Color(0,128,0));
        JLabel modified=new JLabel("Modified from Original"); modified.setForeground(Color.RED);
        JLabel research=new JLabel("Raw / inferred research field");
        JLabel danger=new JLabel("Warning / save-risk field"); danger.setForeground(new Color(180,90,0));
        JLabel match=new JLabel("Hex matches Original"); match.setOpaque(true); match.setBackground(new Color(0,80,0)); match.setForeground(Color.WHITE); match.setBorder(BorderFactory.createEmptyBorder(1,5,1,5));
        JLabel differs=new JLabel("Hex differs from Original"); differs.setOpaque(true); differs.setBackground(new Color(173,216,230)); differs.setForeground(Color.BLACK); differs.setBorder(BorderFactory.createEmptyBorder(1,5,1,5));
        p.add(known); p.add(new JLabel("|")); p.add(modified); p.add(new JLabel("|")); p.add(research); p.add(new JLabel("|")); p.add(danger); p.add(new JLabel("|")); p.add(match); p.add(new JLabel("|")); p.add(differs);
        return p;
    }

    public void setDetectedArchives(Map<String,File> files,File root){detected.clear();gameRoot=root;for(String n:DLCProfiles.names())detected.put(n,files.get(n));rebuildTabs();}
    private String shortName(String n){return n.replace("DLCPack","").replace(".layer.0.all.archive","");}
    private void rebuildTabs(){
        packTabs.removeAll(); panels.clear(); int count=0; StringBuilder s=new StringBuilder("<html>"); boolean first=true;
        for(String n:DLCProfiles.names()){
            File f=detected.get(n); boolean installed=f!=null&&f.isFile(); if(!first)s.append(" &nbsp; | &nbsp; ");first=false;s.append(shortName(n)).append(": <b>").append(installed?"Installed":"Missing").append("</b>");
            if(installed){PackPanel p=new PackPanel(n,f);panels.put(n,p);packTabs.addTab(shortName(n),p);count++;}
        }
        s.append("</html>");installSummary.setText(s.toString());status.setText(count==0?"No supported DLC pack archives detected.":"Loaded "+count+" installed DLC pack tab(s). Missing packs are not shown as editable tabs.");
    }
    private void rescan(){if(gameRoot==null){status.setText("Select DXMD.exe first; no game root is known yet.");return;}try{GameLocator.Result r=GameLocator.locate(new File(new File(gameRoot,"retail"),"DXMD.exe"));setDetectedArchives(r.dlcArchives,r.gameRoot);status.setText("Rescan complete.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Rescan error",JOptionPane.ERROR_MESSAGE);}}
    private void selectFile(){
        JFileChooser fc=new JFileChooser();fc.setDialogTitle("Select DLC archive");fc.setFileSelectionMode(JFileChooser.FILES_ONLY);fc.setAcceptAllFileFilterUsed(false);fc.setFileFilter(new FileNameExtensionFilter("DXMD archives (*.archive)","archive"));
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;File picked=fc.getSelectedFile();
        try{GameLocator.Result r=GameLocator.locateFromHint(picked);setDetectedArchives(r.dlcArchives,r.gameRoot);String profile=DLCProfiles.detect(picked.getName());if(profile!=null&&panels.containsKey(profile))packTabs.setSelectedComponent(panels.get(profile));return;}catch(Exception ignored){}
        String profile=DLCProfiles.detect(picked.getName());if(profile==null){status.setText("Selected archive is not a supported DLC archive and is not inside a recognized DXMD install.");return;}
        detected.put(profile,picked);rebuildTabs();if(panels.containsKey(profile))packTabs.setSelectedComponent(panels.get(profile));status.setText("Loaded standalone "+shortName(profile)+" archive for testing.");
    }
    public void restoreDetectedOriginalFields() throws IOException {for(String n:DLCProfiles.names()){File f=detected.get(n);if(f!=null&&f.isFile())ArchiveRestore.restoreDlcEditorFields(f,DLCProfiles.get(n));}for(PackPanel p:panels.values())p.loadSelected();}

    private class PackPanel extends JPanel {
        final String profileName; final File selectedFile; final DLCProfiles.Profile profile; final PatchTableModel model;
        final JComboBox<String>[] compareBoxes=new JComboBox[3];
        final JTable table; final JTextField fileField=new JTextField(); final JLabel packStatus=new JLabel();

        PackPanel(String name,File file){
            profileName=name;selectedFile=file;profile=DLCProfiles.get(name);model=new PatchTableModel(profile);table=new JTable(model);setLayout(new BorderLayout(6,6));
            JPanel top=new JPanel();top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
            JPanel r1=new JPanel(new BorderLayout(6,6));r1.add(new JLabel("Archive:"),BorderLayout.WEST);fileField.setEditable(false);fileField.setText(file.getAbsolutePath());r1.add(fileField,BorderLayout.CENTER);top.add(r1);

            JPanel compareRow=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
            compareRow.add(new JLabel("Compare Original against:"));
            java.util.List<String> refs=DLCReferenceProfiles.namesFor(profileName);
            for(int i=0;i<3;i++){
                compareBoxes[i]=new JComboBox<>();compareBoxes[i].addItem("(None)");for(String n:refs)compareBoxes[i].addItem(n);
                if(i<refs.size())compareBoxes[i].setSelectedItem(refs.get(i));
                compareBoxes[i].addActionListener(e->refreshComparisonColumns());
                compareRow.add(compareBoxes[i]);
            }
            // JTable is created before the comparison boxes are populated, so its initial
            // TableColumnModel only contains the six fixed columns. Rebuild the structure
            // once the default comparison selections exist so comparison cells are visible
            // immediately instead of requiring the user to re-select a profile.
            refreshComparisonColumns();
            top.add(compareRow);

            JPanel r2=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
            JButton original=new JButton("Original preset"),useCompare=new JButton("Use Compare 1 as preset"),reload=new JButton("Reload current"),saveIds=new JButton("Save Identifications..."),loadIds=new JButton("Load Identifications..."),restore=new JButton("Restore editor fields"),bak=new JButton("Restore .bak"),apply=new JButton("Apply");
            r2.add(original);r2.add(useCompare);r2.add(reload);r2.add(saveIds);r2.add(loadIds);r2.add(restore);r2.add(bak);r2.add(apply);top.add(r2);add(top,BorderLayout.NORTH);
            table.setAutoCreateRowSorter(false);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setRowHeight(Math.max(table.getRowHeight(),22));table.setDefaultRenderer(Object.class,new GroupAwareRenderer());add(new JScrollPane(table),BorderLayout.CENTER);add(packStatus,BorderLayout.SOUTH);
            original.addActionListener(e->{model.useOriginal();autoSizeColumns();});
            useCompare.addActionListener(e->{java.util.List<String> x=selectedComparisons();if(!x.isEmpty()){model.useReference(x.get(0));autoSizeColumns();}});
            reload.addActionListener(e->loadSelected());
            saveIds.addActionListener(e->FieldIdentificationIO.exportDlc(DLCEditorPanel.this,profileName,profile));
            loadIds.addActionListener(e->{FieldIdentificationIO.importDlc(DLCEditorPanel.this,profileName,profile);model.fireTableDataChanged();autoSizeColumns();});
            restore.addActionListener(e->restoreOriginalFields());bak.addActionListener(e->restoreBackup());apply.addActionListener(e->applyChanges());loadSelected();
        }
        java.util.List<String> selectedComparisons(){ArrayList<String> out=new ArrayList<>();for(JComboBox<String> b:compareBoxes){if(b==null)continue;String n=(String)b.getSelectedItem();if(n!=null&&!n.equals("(None)")&&!out.contains(n))out.add(n);}return out;}
        byte[] referenceBytes(DLCProfiles.Field f,String n){return DLCReferenceProfiles.get(profileName,n,f.offset,f);}
        void refreshComparisonColumns(){
            model.fireTableStructureChanged();
            configureTable();
        }
        void configureTable(){table.setDefaultRenderer(Object.class,new GroupAwareRenderer());autoSizeColumns();}
        void loadSelected(){try{if(selectedFile.length()!=profile.size)throw new IOException("Unexpected file size. Expected "+profile.size+" bytes, got "+selectedFile.length());try(RandomAccessFile raf=new RandomAccessFile(selectedFile,"r")){for(DLCProfiles.Field f:profile.fields){raf.seek(f.offset);byte[] b=new byte[f.original.length];raf.readFully(b);f.current=b;}}model.fireTableDataChanged();configureTable();packStatus.setText("Loaded "+profile.fields.size()+" editable regions. Identity: "+BackupManager.identify(selectedFile)+(BackupManager.hasBackup(selectedFile)?" | .bak available":""));}catch(Exception ex){JOptionPane.showMessageDialog(DLCEditorPanel.this,ex.getMessage(),"Load error",JOptionPane.ERROR_MESSAGE);}}
        void applyChanges(){
            if(table.isEditing())table.getCellEditor().stopCellEditing();
            ArrayList<String> risky=new ArrayList<>();
            for(DLCProfiles.Field f:profile.fields) if(isDangerous(f)&&!Arrays.equals(f.current,f.original)) risky.add(attributeFor(f));
            if(!risky.isEmpty()&&!RiskWarning.confirm(DLCEditorPanel.this,"Risky DLC Archive Edit",
                    "These fields change inventory item dimensions. DXMD saves can retain the old dimensions; if an affected item is already present in inventory, changing its size can make the save unusable or crash the inventory screen.",risky)) return;
            try{File backup=BackupManager.ensureBackup(selectedFile);try(RandomAccessFile raf=new RandomAccessFile(selectedFile,"rw")){for(DLCProfiles.Field f:profile.fields){raf.seek(f.offset);raf.write(f.current);}}loadSelected();packStatus.setText("Applied and reloaded current values. Backup: "+backup.getName());JOptionPane.showMessageDialog(DLCEditorPanel.this,"Changes applied to "+shortName(profileName)+" and reloaded.\nBackup kept at:\n"+backup.getAbsolutePath());}catch(Exception ex){JOptionPane.showMessageDialog(DLCEditorPanel.this,ex.getMessage(),"Apply error",JOptionPane.ERROR_MESSAGE);}
        }
        void restoreOriginalFields(){int a=JOptionPane.showConfirmDialog(DLCEditorPanel.this,"Restore editor-supported fields in "+shortName(profileName)+" to clean-game values?","Restore editor fields",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(a!=JOptionPane.YES_OPTION)return;try{ArchiveRestore.restoreDlcEditorFields(selectedFile,profile);loadSelected();}catch(Exception ex){JOptionPane.showMessageDialog(DLCEditorPanel.this,ex.getMessage(),"Restore error",JOptionPane.ERROR_MESSAGE);}}
        void restoreBackup(){if(!BackupManager.hasBackup(selectedFile)){JOptionPane.showMessageDialog(DLCEditorPanel.this,"No .bak exists for this archive yet.","Restore .bak",JOptionPane.INFORMATION_MESSAGE);return;}int a=JOptionPane.showConfirmDialog(DLCEditorPanel.this,"Replace this DLC archive with its exact .bak copy?","Restore exact backup",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(a!=JOptionPane.YES_OPTION)return;try{BackupManager.restoreBackup(selectedFile);loadSelected();}catch(Exception ex){JOptionPane.showMessageDialog(DLCEditorPanel.this,ex.getMessage(),"Restore .bak error",JOptionPane.ERROR_MESSAGE);}}
        String weaponName(){String n=profileName.toLowerCase(Locale.ROOT);if(n.contains("assault"))return "Battle Rifle";if(n.contains("classic"))return "Revolver";if(n.contains("enforcer"))return "Combat Rifle";if(n.contains("intruder"))return "Pistol";if(n.contains("tactical"))return "Tranquilizer Rifle";return "Weapon";}
        boolean isDangerous(DLCProfiles.Field f){String u=f.label==null?"":f.label.toUpperCase(Locale.ROOT);return u.contains("INVENTORY WIDTH")||u.contains("INVENTORY HEIGHT");}
        String attributeFor(DLCProfiles.Field target){
            if(isDangerous(target))return "WARNING — CAN BREAK SAVES: "+target.label;
            String g=semanticGroup(target.label); String w=weaponName();
            if(g!=null){
                if(g.equals("Accuracy"))return w+" Accuracy"; if(g.equals("Ammo Capacity"))return w+" Ammo Capacity"; if(g.equals("Damage"))return w+" Damage";
                if(g.equals("Fire Pattern"))return w+" Fire Pattern"; if(g.equals("Rate of Fire"))return w+" Rate of Fire"; if(g.equals("Recoil"))return w+" Recoil";
                if(g.equals("Reload Speed"))return w+" Reload Speed"; if(g.equals("Scope"))return w+" Scope"; if(g.equals("Chaff"))return "Chaff Augmentation Attribute";
                if(g.equals("Weapon Core"))return "Suspected: "+w+" Weapon Stat — Unknown"; return g;
            }
            DLCProfiles.Field nearest=null;long best=Long.MAX_VALUE;for(DLCProfiles.Field f:profile.fields){String sg=semanticGroup(f.label);if(sg==null)continue;long dist=Math.abs(f.offset-target.offset);if(dist<best){best=dist;nearest=f;}}
            if(nearest!=null&&best<=900){String ng=semanticGroup(nearest.label);if(ng!=null&&(ng.equals("Accuracy")||ng.equals("Ammo Capacity")||ng.equals("Damage")||ng.equals("Fire Pattern")||ng.equals("Rate of Fire")||ng.equals("Recoil")||ng.equals("Reload Speed")||ng.equals("Scope")||ng.equals("Weapon Core")))return "Suspected: "+w+" Weapon Stat — Unknown";return "Suspected: "+ng+" Attribute — Unknown";}
            return "Unidentified";
        }
        void autoSizeColumns(){FontMetrics fm=table.getFontMetrics(table.getFont());for(int c=0;c<table.getColumnCount();c++){TableColumn col=table.getColumnModel().getColumn(c);int w=fm.stringWidth(table.getColumnName(c))+28;for(int r=0;r<table.getRowCount();r++){Object v=table.getValueAt(r,c);if(v!=null)w=Math.max(w,fm.stringWidth(String.valueOf(v))+24);}int max=(c<=2)?360:190;col.setPreferredWidth(Math.min(max,Math.max(w,80)));}}
        class GroupAwareRenderer extends JLabel implements TableCellRenderer{
            private final Color matchBg=new Color(0,80,0);
            private final Color differentBg=new Color(173,216,230);
            GroupAwareRenderer(){setOpaque(true);setBorder(BorderFactory.createEmptyBorder(2,5,2,5));}
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                setText(value==null?"":String.valueOf(value));int mr=t.convertRowIndexToModel(row);DLCProfiles.Field f=profile.fields.get(mr);
                boolean modified=!Arrays.equals(f.current,f.original);String direct=semanticGroup(f.label);boolean known=direct!=null&&!f.label.toUpperCase(Locale.ROOT).startsWith("RAW FIELD");boolean danger=isDangerous(f);Color stateColor=modified?Color.RED:(danger?new Color(180,90,0):(known?new Color(0,128,0):null));
                boolean hexColumn=col>=4;
                if(hexColumn){byte[] shown;if(col==4)shown=f.current;else if(col==5)shown=f.original;else shown=referenceBytes(f,selectedComparisons().get(col-6));boolean matches=shown!=null&&Arrays.equals(shown,f.original);setBackground(matches?matchBg:differentBg);setForeground(matches?Color.WHITE:Color.BLACK);}
                else if(selected){setBackground(t.getSelectionBackground());setForeground(stateColor!=null?stateColor:t.getSelectionForeground());}
                else{setBackground(t.getBackground());setForeground(stateColor!=null?stateColor:t.getForeground());}
                boolean start=mr==0;if(!start)start=!attributeFor(profile.fields.get(mr)).equals(attributeFor(profile.fields.get(mr-1)));setBorder(start?BorderFactory.createCompoundBorder(new MatteBorder(2,0,0,0,t.getGridColor()),BorderFactory.createEmptyBorder(3,5,2,5)):BorderFactory.createEmptyBorder(2,5,2,5));return this;
            }
        }
        class PatchTableModel extends AbstractTableModel{
            final DLCProfiles.Profile p;PatchTableModel(DLCProfiles.Profile x){p=x;}
            public int getRowCount(){return p.fields.size();}
            public int getColumnCount(){return 6+selectedComparisons().size();}
            public String getColumnName(int c){if(c==0)return "Attribute Name";if(c==1)return "Context / field";if(c==2)return "User ID";if(c==3)return "Current decimal";if(c==4)return "Current hex";if(c==5)return "Original hex";return selectedComparisons().get(c-6)+" hex";}
            public Object getValueAt(int r,int c){DLCProfiles.Field f=p.fields.get(r);if(c==0)return attributeFor(f);if(c==1)return f.label;if(c==2)return f.identifiedAs;if(c==3)return decimal(f.current);if(c==4)return hex(f.current);if(c==5)return hex(f.original);return hex(referenceBytes(f,selectedComparisons().get(c-6)));}
            public boolean isCellEditable(int r,int c){return c==2||c==3;}
            public void setValueAt(Object v,int r,int c){DLCProfiles.Field f=p.fields.get(r);try{if(c==2)f.identifiedAs=String.valueOf(v).trim();else if(c==3)f.current=parseDecimal(String.valueOf(v),f.original.length);else return;}catch(Exception ex){JOptionPane.showMessageDialog(DLCEditorPanel.this,ex.getMessage(),"Invalid value",JOptionPane.ERROR_MESSAGE);}fireTableRowsUpdated(r,r);}
            void useOriginal(){for(DLCProfiles.Field f:p.fields)f.current=f.original.clone();fireTableDataChanged();}
            void useReference(String n){for(DLCProfiles.Field f:p.fields){byte[] b=referenceBytes(f,n);if(b!=null)f.current=b.clone();}fireTableDataChanged();}
        }
    }

    static String semanticGroup(String label){if(label==null)return null;String u=label.toUpperCase(Locale.ROOT);if(u.startsWith("RAW FIELD"))return null;if(u.startsWith("ACCURACY"))return "Accuracy";if(u.startsWith("AMMO_CAPACITY"))return "Ammo Capacity";if(u.startsWith("DAMAGE"))return "Damage";if(u.startsWith("FIRE_PATTERN"))return "Fire Pattern";if(u.startsWith("RATE_OF_F"))return "Rate of Fire";if(u.startsWith("RECOIL"))return "Recoil";if(u.startsWith("RELOAD_SPEED"))return "Reload Speed";if(u.startsWith("SCOPE"))return "Scope";if(u.startsWith("CHAFF"))return "Chaff";if(u.contains("CORE")||u.contains("PREORDER"))return "Weapon Core";return label.replaceFirst("\\s+#\\d+$","");}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format("%02X",v&255));return s.toString();}
    static byte[] parseHex(String s,int len){s=s.replaceAll("[^0-9A-Fa-f]","");if(s.length()!=len*2)throw new IllegalArgumentException("Expected "+(len*2)+" hex digits");byte[] b=new byte[len];for(int i=0;i<len;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    static String decimal(byte[] b){if(b.length==4){int bits=(b[0]&255)|((b[1]&255)<<8)|((b[2]&255)<<16)|((b[3]&255)<<24);float f=Float.intBitsToFloat(bits);if(Float.isFinite(f))return Float.toString(f);}long v=0;for(int i=0;i<b.length&&i<8;i++)v|=((long)b[i]&255L)<<(8*i);return Long.toUnsignedString(v);}
    static byte[] parseDecimal(String s,int len){s=s.trim();if(len==4){float f=Float.parseFloat(s);int bits=Float.floatToRawIntBits(f);return new byte[]{(byte)bits,(byte)(bits>>>8),(byte)(bits>>>16),(byte)(bits>>>24)};}long max=len>=8?-1L:((1L<<(len*8))-1L);long v=Long.parseLong(s);if(v<0||(len<8&&v>max))throw new IllegalArgumentException("Value must fit in "+len+" byte(s)");byte[] b=new byte[len];for(int i=0;i<len;i++)b[i]=(byte)(v>>>(8*i));return b;}
}
