import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

@SuppressWarnings({"serial", "this-escape"})
public class BaseFieldsPanel extends JPanel {
    private final JTextField fileField=new JTextField();
    private final JTextField filterField=new JTextField(18);
    private final JLabel status=new JLabel("Load DXMD.exe or the base archive.");
    private final ArrayList<JComboBox<String>> compareBoxes=new ArrayList<JComboBox<String>>(3);
    private final ResearchTableModel model=new ResearchTableModel();
    private final JTable table=new JTable(model);
    private final TableRowSorter<ResearchTableModel> sorter=new TableRowSorter<>(model);
    private File selectedFile;
    private String[] nearbyContexts=new String[0];

    public BaseFieldsPanel(){
        setLayout(new BorderLayout(8,8));
        JPanel top=new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        JPanel row1=new JPanel(new BorderLayout(6,6));
        row1.add(new JLabel("Base archive:"),BorderLayout.WEST); fileField.setEditable(false); row1.add(fileField,BorderLayout.CENTER);
        JButton choose=new JButton("Select EXE / Archive"); row1.add(choose,BorderLayout.EAST); top.add(row1);

        JPanel compareRow=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        compareRow.add(new JLabel("Compare Original against:"));
        java.util.List<String> refs=BaseResearchProfiles.referenceNames();
        for(int i=0;i<3;i++){
            JComboBox<String> box=new JComboBox<>(); box.addItem("(None)"); for(String n:refs) box.addItem(n);
            if(i<refs.size()) box.setSelectedItem(refs.get(i));
            box.addActionListener(e->{ model.fireTableStructureChanged(); configureTable(); });
            compareBoxes.add(box);
            compareRow.add(box);
        }
        top.add(compareRow);

        JPanel row2=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        JButton original=new JButton("Original preset"); JButton useCompare=new JButton("Use Compare 1 as preset"); JButton reload=new JButton("Reload current");
        JButton restoreOriginal=new JButton("Restore editor fields"); JButton restoreBak=new JButton("Restore .bak"); JButton apply=new JButton("Apply");
        JButton saveIds=new JButton("Save Identifications..."); JButton loadIds=new JButton("Load Identifications...");
        row2.add(new JLabel("Filter:")); row2.add(filterField); row2.add(original); row2.add(useCompare); row2.add(reload); row2.add(saveIds); row2.add(loadIds); row2.add(restoreOriginal); row2.add(restoreBak); row2.add(apply); top.add(row2);
        top.add(new JLabel("Original is always shown. Nearby context is derived conservatively from archive identifiers; rows are locked to physical archive order."));
        top.add(makeLegend());
        add(top,BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setRowHeight(Math.max(table.getRowHeight(),22)); table.setRowSorter(sorter); table.setDefaultRenderer(Object.class,new GroupRenderer());
        table.getTableHeader().setToolTipText("Base Fields remains in physical archive order. Column sorting is disabled; filtering does not reorder rows.");
        add(new JScrollPane(table),BorderLayout.CENTER); add(status,BorderLayout.SOUTH); configureTable();

        choose.addActionListener(e->selectFile()); original.addActionListener(e->{model.useOriginal();autoSizeColumns();});
        useCompare.addActionListener(e->{String n=(String)compareBoxes.get(0).getSelectedItem(); if(n!=null&&!n.equals("(None)")){model.useReference(n);autoSizeColumns();}});
        reload.addActionListener(e->loadSelected());
        saveIds.addActionListener(e->{if(model.profile!=null)FieldIdentificationIO.exportBase(this,model.profile);});
        loadIds.addActionListener(e->{if(model.profile!=null){FieldIdentificationIO.importBase(this,model.profile);model.fireTableDataChanged();autoSizeColumns();}});
        restoreOriginal.addActionListener(e->restoreOriginalFields()); restoreBak.addActionListener(e->restoreBackup()); apply.addActionListener(e->applyChanges());
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){updateFilter();}public void removeUpdate(javax.swing.event.DocumentEvent e){updateFilter();}public void changedUpdate(javax.swing.event.DocumentEvent e){updateFilter();}});
    }

    private JPanel makeLegend(){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,1));
        p.add(new JLabel("Legend:"));
        JLabel known=new JLabel("Confirmed / isolated field"); known.setForeground(new Color(0,128,0));
        JLabel strong=new JLabel("Strong suspected"); strong.setForeground(new Color(70,100,180));
        JLabel modified=new JLabel("Modified from Original"); modified.setForeground(Color.RED);
        JLabel research=new JLabel("Research / not yet confirmed");
        JLabel danger=new JLabel("Warning / save-risk field"); danger.setForeground(new Color(180,90,0));
        JLabel match=new JLabel("Hex matches Original"); match.setOpaque(true); match.setBackground(new Color(0,80,0)); match.setForeground(Color.WHITE); match.setBorder(BorderFactory.createEmptyBorder(1,5,1,5));
        JLabel differs=new JLabel("Hex differs from Original"); differs.setOpaque(true); differs.setBackground(new Color(173,216,230)); differs.setForeground(Color.BLACK); differs.setBorder(BorderFactory.createEmptyBorder(1,5,1,5));
        p.add(known); p.add(new JLabel("|")); p.add(strong); p.add(new JLabel("|")); p.add(modified); p.add(new JLabel("|")); p.add(research); p.add(new JLabel("|")); p.add(danger); p.add(new JLabel("|")); p.add(match); p.add(new JLabel("|")); p.add(differs);
        return p;
    }

    private List<String> selectedComparisons(){
        ArrayList<String> out=new ArrayList<>();
        if(compareBoxes==null) return out;
        for(JComboBox<String> b:compareBoxes){
            if(b==null) continue;
            String s=(String)b.getSelectedItem();
            if(s!=null&&!s.equals("(None)")&&!out.contains(s)) out.add(s);
        }
        return out;
    }

    private void configureTable(){
        table.setDefaultRenderer(Object.class,new GroupRenderer());
        sorter.setSortKeys(Collections.emptyList());
        for(int c=0;c<model.getColumnCount();c++) sorter.setSortable(c,false);
        autoSizeColumns();
    }
    public void loadArchive(File f){selectedFile=f;fileField.setText(f==null?"":f.getAbsolutePath());loadSelected();}
    public void clearArchive(String msg){selectedFile=null;fileField.setText("");nearbyContexts=new String[0];model.setProfile(null);status.setText(msg);}
    public File getSelectedFile(){return selectedFile;}

    private void selectFile(){JFileChooser fc=new JFileChooser();fc.setDialogTitle("Select DXMD.exe or Game.layer.1.all.archive");fc.setFileSelectionMode(JFileChooser.FILES_ONLY);fc.setAcceptAllFileFilterUsed(false);fc.setFileFilter(new FileNameExtensionFilter("DXMD executable / archives (*.exe, *.archive)","exe","archive"));if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)Launcher.openGameOrArchive(fc.getSelectedFile(),this);}
    private void loadSelected(){
        if(selectedFile==null)return;
        try{
            BaseResearchProfiles.Profile p=BaseResearchProfiles.get();
            if(!selectedFile.getName().equalsIgnoreCase(p.name))throw new IOException("Expected "+p.name+".");
            if(selectedFile.length()!=p.size)throw new IOException("Unexpected file size. Expected "+p.size+" bytes, got "+selectedFile.length()+".");
            try(RandomAccessFile raf=new RandomAccessFile(selectedFile,"r")){
                for(BaseResearchProfiles.Field f:p.fields){raf.seek(f.offset);byte[] b=new byte[f.original.length];raf.readFully(b);f.current=b;}
            }
            try{nearbyContexts=ArchiveContextResolver.resolve(selectedFile,p.fields);}
            catch(Exception contextError){
                nearbyContexts=new String[p.fields.size()];
                for(int i=0;i<p.fields.size();i++)nearbyContexts[i]=ArchiveContextResolver.fallback(p.fields.get(i));
            }
            model.setProfile(p);configureTable();
            status.setText("Loaded "+p.fields.size()+" base field records in archive order. Context labels were re-evaluated from nearby archive identifiers. Identity: "+BackupManager.identify(selectedFile)+(BackupManager.hasBackup(selectedFile)?" | .bak available":""));
        }catch(Exception ex){nearbyContexts=new String[0];model.setProfile(null);JOptionPane.showMessageDialog(this,ex.getMessage(),"Load error",JOptionPane.ERROR_MESSAGE);}
    }
    private void applyChanges(){
        if(selectedFile==null||model.profile==null){status.setText("No base archive loaded.");return;}
        if(table.isEditing())table.getCellEditor().stopCellEditing();
        ArrayList<String> risky=new ArrayList<>();
        for(BaseResearchProfiles.Field f:model.profile.fields) if(isDangerous(f)&&!Arrays.equals(f.current,f.original)) risky.add(f.label);
        if(!risky.isEmpty()&&!RiskWarning.confirm(this,"Risky Base Archive Edit",
                "These fields change inventory item dimensions. DXMD saves can retain the old dimensions; if an affected item is already present in inventory, changing its size can make the save unusable or crash the inventory screen.",risky)) return;
        try{File backup=BackupManager.ensureBackup(selectedFile);try(RandomAccessFile raf=new RandomAccessFile(selectedFile,"rw")){for(BaseResearchProfiles.Field f:model.profile.fields){raf.seek(f.offset);raf.write(f.current);}}Launcher.refreshBaseViews(selectedFile);status.setText("Applied and reloaded Base Fields values. Backup: "+backup.getAbsolutePath());JOptionPane.showMessageDialog(this,"Changes applied and current values reloaded.\nBackup kept at:\n"+backup.getAbsolutePath());}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Apply error",JOptionPane.ERROR_MESSAGE);}
    }
    private void restoreOriginalFields(){if(selectedFile==null)return;int a=JOptionPane.showConfirmDialog(this,"Restore all confirmed Base Fields mappings to clean-game values?\n\nSuspected/unidentified research records and unrelated mod bytes are left unchanged. A .bak is preserved/created first.","Restore editor fields",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(a!=JOptionPane.YES_OPTION)return;try{ArchiveRestore.restoreBaseEditorFields(selectedFile);Launcher.refreshBaseViews(selectedFile);status.setText("Restored editor-supported base fields to original values. .bak preserved.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Restore error",JOptionPane.ERROR_MESSAGE);}}
    private void restoreBackup(){if(selectedFile==null)return;if(!BackupManager.hasBackup(selectedFile)){JOptionPane.showMessageDialog(this,"No .bak exists for this archive yet.","Restore .bak",JOptionPane.INFORMATION_MESSAGE);return;}int a=JOptionPane.showConfirmDialog(this,"Replace the current base archive with its exact .bak copy?","Restore exact backup",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(a!=JOptionPane.YES_OPTION)return;try{BackupManager.restoreBackup(selectedFile);Launcher.refreshBaseViews(selectedFile);status.setText("Restored exact .bak copy and reloaded current values.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Restore .bak error",JOptionPane.ERROR_MESSAGE);}}
    private void updateFilter(){String q=filterField.getText().trim();if(q.isEmpty())sorter.setRowFilter(null);else sorter.setRowFilter(RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(q),0,1,2));}

    static String hex(byte[] b){if(b==null)return "";StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format("%02X",v&255));return s.toString();}
    static byte[] parseHex(String s,int len){s=s.replaceAll("[^0-9A-Fa-f]","");if(s.length()!=len*2)throw new IllegalArgumentException("Expected "+(len*2)+" hex digits");byte[] b=new byte[len];for(int i=0;i<len;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    static String decimal(byte[] b){if(b.length==4){int bits=(b[0]&255)|((b[1]&255)<<8)|((b[2]&255)<<16)|((b[3]&255)<<24);float f=Float.intBitsToFloat(bits);if(Float.isFinite(f)&&Math.abs(f)>=0.000001f&&Math.abs(f)<10000000f)return Float.toString(f);}long v=0;for(int i=0;i<b.length&&i<8;i++)v|=((long)b[i]&255L)<<(8*i);return Long.toUnsignedString(v);}
    static byte[] parseDecimal(String s,int len){s=s.trim();if(len==4&&s.matches(".*[.eE].*")){float f=Float.parseFloat(s);if(!Float.isFinite(f))throw new IllegalArgumentException("Float value must be finite");int bits=Float.floatToRawIntBits(f);return new byte[]{(byte)bits,(byte)(bits>>>8),(byte)(bits>>>16),(byte)(bits>>>24)};}long max=len>=8?-1L:((1L<<(len*8))-1L);long v=Long.parseLong(s);if(v<0||(len<8&&v>max))throw new IllegalArgumentException("Value must fit in "+len+" byte(s)");byte[] b=new byte[len];for(int i=0;i<len;i++)b[i]=(byte)(v>>>(8*i));return b;}
    private boolean isDangerous(BaseResearchProfiles.Field f){
        return FieldConfidenceAudit.isBaseSaveRisk(f);
    }

    private String nearbyContext(int row){
        if(row>=0&&row<nearbyContexts.length&&nearbyContexts[row]!=null&&!nearbyContexts[row].trim().isEmpty())return nearbyContexts[row];
        if(model.profile!=null&&row>=0&&row<model.profile.fields.size())return ArchiveContextResolver.fallback(model.profile.fields.get(row));
        return "";
    }

    private String nearbySpecificName(int row){
        if(model.profile==null)return null;
        String resolved=nearbyContext(row);
        if(resolved!=null&&!resolved.isEmpty()&&!resolved.equals("No nearby readable identifier")&&!resolved.contains(" … ")&&!resolved.contains(" → ")){
            String u=resolved.toUpperCase(Locale.ROOT);
            boolean useful=u.contains("MM")||u.contains("GAUGE")||u.contains("GRENADE")||u.contains("RIFLE")||u.contains("PISTOL")||u.contains("SHOTGUN")||u.contains("BIOCELL")||u.contains("PAINKILLER")||u.contains("HYPOSTIM")||u.contains("PRAXIS")||u.contains("WEAPON_PARTS")||u.contains("MULTITOOL");
            if(useful)return resolved;
        }
        BaseResearchProfiles.Field f=model.profile.fields.get(row);
        String best=null; long bestDist=Long.MAX_VALUE;
        for(int i=Math.max(0,row-5);i<=Math.min(model.profile.fields.size()-1,row+5);i++){
            if(i==row)continue; BaseResearchProfiles.Field n=model.profile.fields.get(i);
            String l=n.label==null?"":n.label; String u=l.toUpperCase(Locale.ROOT);
            if(u.startsWith("RAW FIELD")||u.matches(".*#\\d+$")) continue;
            boolean useful=u.contains("MM")||u.contains("GAUGE")||u.contains("GRENADE")||u.contains("RIFLE")||u.contains("PISTOL")||u.contains("SHOTGUN")||u.contains("BIOCELL")||u.contains("PAINKILLER")||u.contains("HYPOSTIM")||u.contains("PRAXIS")||u.contains("WEAPON_PARTS")||u.contains("MULTITOOL");
            if(!useful)continue; long d=Math.abs(n.offset-f.offset); if(d<bestDist&&d<=500){bestDist=d;best=l.replaceFirst("\\s+#\\d+$","");}
        }
        return best;
    }

    private String attributeName(int row){
        BaseResearchProfiles.Field f=model.profile.fields.get(row);
        FieldConfidenceAudit.Assessment a=FieldConfidenceAudit.assessBase(f,nearbySpecificName(row),nearbyContext(row));
        if(a.saveRisk) return "WARNING — CAN BREAK SAVES: "+a.name;
        return a.name;
    }

    private void autoSizeColumns(){if(table.getColumnCount()==0)return;FontMetrics fm=table.getFontMetrics(table.getFont());for(int c=0;c<table.getColumnCount();c++){TableColumn col=table.getColumnModel().getColumn(c);int w=fm.stringWidth(table.getColumnName(c))+28;int rows=Math.min(table.getRowCount(),500);for(int r=0;r<rows;r++){Object v=table.getValueAt(r,c);if(v!=null)w=Math.max(w,fm.stringWidth(String.valueOf(v))+24);}int max=(c<=2)?390:190;col.setPreferredWidth(Math.min(max,Math.max(w,85)));}}

    class GroupRenderer extends JLabel implements TableCellRenderer{
        private final Color matchBg=new Color(0,80,0);
        private final Color differentBg=new Color(173,216,230);
        GroupRenderer(){setOpaque(true);setBorder(BorderFactory.createEmptyBorder(2,5,2,5));}
        public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int viewRow,int col){
            setText(value==null?"":String.valueOf(value));
            int row=t.convertRowIndexToModel(viewRow);
            BaseResearchProfiles.Field f=model.profile==null?null:model.profile.fields.get(row);
            boolean modified=f!=null&&!Arrays.equals(f.current,f.original);
            FieldConfidenceAudit.Assessment assessment=f==null?null:FieldConfidenceAudit.assessBase(f,nearbySpecificName(row),nearbyContext(row));
            boolean known=assessment!=null&&assessment.confidence==FieldConfidenceAudit.Confidence.CONFIRMED;
            boolean strong=assessment!=null&&assessment.confidence==FieldConfidenceAudit.Confidence.STRONG_SUSPECTED;
            boolean danger=assessment!=null&&assessment.saveRisk;
            setToolTipText(assessment==null?null:assessment.evidence);
            Color stateColor=modified?Color.RED:(danger?new Color(180,90,0):(known?new Color(0,128,0):(strong?new Color(70,100,180):null)));
            boolean hexColumn=col>=4;
            if(hexColumn&&f!=null){
                byte[] shown;
                if(col==4) shown=f.current;
                else if(col==5) shown=f.original;
                else shown=f.reference(selectedComparisons().get(col-6));
                boolean matches=shown!=null&&Arrays.equals(shown,f.original);
                setBackground(matches?matchBg:differentBg);
                setForeground(matches?Color.WHITE:Color.BLACK);
            }else if(selected){
                setBackground(t.getSelectionBackground());
                setForeground(stateColor!=null?stateColor:t.getSelectionForeground());
            }else{
                setBackground(t.getBackground());
                setForeground(stateColor!=null?stateColor:t.getForeground());
            }
            boolean start=row==0;
            if(!start&&model.profile!=null)start=!attributeName(row).equals(attributeName(row-1));
            setBorder(start?BorderFactory.createCompoundBorder(new MatteBorder(2,0,0,0,t.getGridColor()),BorderFactory.createEmptyBorder(3,5,2,5)):BorderFactory.createEmptyBorder(2,5,2,5));
            return this;
        }
    }

    class ResearchTableModel extends AbstractTableModel{
        BaseResearchProfiles.Profile profile;
        void setProfile(BaseResearchProfiles.Profile p){profile=p;fireTableStructureChanged();}
        public int getRowCount(){return profile==null?0:profile.fields.size();}
        public int getColumnCount(){return 6+selectedComparisons().size();}
        public String getColumnName(int c){if(c==0)return "Attribute Name";if(c==1)return "Nearby context / field";if(c==2)return "User ID";if(c==3)return "Current decimal";if(c==4)return "Current hex";if(c==5)return "Original hex";return selectedComparisons().get(c-6)+" hex";}
        public Object getValueAt(int r,int c){BaseResearchProfiles.Field f=profile.fields.get(r);if(c==0)return attributeName(r);if(c==1)return nearbyContext(r);if(c==2)return f.identifiedAs;if(c==3)return decimal(f.current);if(c==4)return hex(f.current);if(c==5)return hex(f.original);String n=selectedComparisons().get(c-6);return hex(f.reference(n));}
        public boolean isCellEditable(int r,int c){return c==2||c==3;}
        public void setValueAt(Object v,int r,int c){BaseResearchProfiles.Field f=profile.fields.get(r);try{if(c==2)f.identifiedAs=String.valueOf(v).trim();else if(c==3)f.current=parseDecimal(String.valueOf(v),f.original.length);else return;}catch(Exception ex){JOptionPane.showMessageDialog(BaseFieldsPanel.this,ex.getMessage(),"Invalid value",JOptionPane.ERROR_MESSAGE);}fireTableRowsUpdated(r,r);}
        void useOriginal(){if(profile!=null){for(BaseResearchProfiles.Field f:profile.fields)f.current=f.original.clone();fireTableDataChanged();}}
        void useReference(String n){if(profile!=null){for(BaseResearchProfiles.Field f:profile.fields){byte[] b=f.reference(n);if(b!=null)f.current=b.clone();}fireTableDataChanged();}}
    }
}
