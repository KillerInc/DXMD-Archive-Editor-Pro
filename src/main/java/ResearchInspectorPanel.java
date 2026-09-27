import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@SuppressWarnings("serial")
public class ResearchInspectorPanel extends JPanel {
    private final JComboBox<Source> sourceBox=new JComboBox<>();
    private final JTextField filterField=new JTextField(22);
    private final JLabel status=new JLabel("Select DXMD.exe to load archive research data.");
    private final ResearchModel model=new ResearchModel();
    private final JTable table=new JTable(model);
    private final TableRowSorter<ResearchModel> sorter=new TableRowSorter<>(model);
    private final JTextField resourceField=readonly(),chunkField=readonly(),resourceOffsetField=readonly(),archiveOffsetField=readonly();
    private final JTextArea rawContext=new JTextArea(4,50),interpretations=new JTextArea(9,50),evidenceArea=new JTextArea(8,34),comparisonsArea=new JTextArea(12,34);
    private final JLabel confidenceLabel=new JLabel("No row selected");
    private final JSpinner candidateDelta=new JSpinner(new SpinnerNumberModel(0,-8,8,1));
    private final JComboBox<Integer> candidateWidth=new JComboBox<>(new Integer[]{1,2,4,8});
    private final JComboBox<String> editType=new JComboBox<>(new String[]{"Hex bytes","Unsigned integer (LE)","Signed integer (LE)","Float16 (LE)","Float32 (LE)"});
    private final JTextField editValue=new JTextField(18);
    private final JButton applySelected=new JButton("Apply Selected Region");
    private File baseArchive;
    private final LinkedHashMap<String,File> dlcArchives=new LinkedHashMap<>();
    private Source activeSource;
    private ArchiveResourceIndex index;
    private String[] contexts=new String[0];

    private record Source(String title,String profileName,File file,boolean base){@Override public String toString(){return title;}}
    private record Row(long offset,int length,String label,String context,byte[] original,byte[] current,Object field){}

    public ResearchInspectorPanel(){
        setLayout(new BorderLayout(8,8));
        JPanel top=new JPanel(new BorderLayout(8,4)),left=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        left.add(new JLabel("Archive:"));left.add(sourceBox);left.add(new JLabel("Filter:"));left.add(filterField);
        JButton reload=new JButton("Reload"),saveIds=new JButton("Save IDs..."),loadIds=new JButton("Load IDs...");
        left.add(reload);left.add(saveIds);left.add(loadIds);top.add(left,BorderLayout.WEST);
        top.add(new JLabel("Research view: changed-byte runs are evidence, not assumed field boundaries."),BorderLayout.SOUTH);add(top,BorderLayout.NORTH);

        table.setRowHeight(22);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setRowSorter(sorter);sorter.setSortKeys(Collections.emptyList());table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);configureColumns();
        JScrollPane listScroll=new JScrollPane(table);listScroll.setPreferredSize(new Dimension(620,600));
        rawContext.setEditable(false);rawContext.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));rawContext.setLineWrap(true);rawContext.setWrapStyleWord(true);
        interpretations.setEditable(false);interpretations.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        evidenceArea.setEditable(false);evidenceArea.setLineWrap(true);evidenceArea.setWrapStyleWord(true);comparisonsArea.setEditable(false);comparisonsArea.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));

        JPanel identity=new JPanel(new GridLayout(4,2,6,3));identity.setBorder(BorderFactory.createTitledBorder("Selected Region"));
        identity.add(new JLabel("Internal Resource"));identity.add(resourceField);identity.add(new JLabel("Chunk"));identity.add(chunkField);identity.add(new JLabel("Resource Offset"));identity.add(resourceOffsetField);identity.add(new JLabel("Archive Offset"));identity.add(archiveOffsetField);
        JPanel candidate=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));candidate.setBorder(BorderFactory.createTitledBorder("Candidate Boundary / Interpretation"));
        candidate.add(new JLabel("Start delta:"));candidate.add(candidateDelta);candidate.add(new JLabel("Width:"));candidate.add(candidateWidth);candidate.add(new JLabel("Interpret as:"));candidate.add(editType);candidate.add(editValue);candidate.add(applySelected);
        JPanel center=new JPanel();center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));center.add(identity);
        JScrollPane rawScroll=new JScrollPane(rawContext);rawScroll.setBorder(BorderFactory.createTitledBorder("Raw Context"));center.add(rawScroll);
        JScrollPane intScroll=new JScrollPane(interpretations);intScroll.setBorder(BorderFactory.createTitledBorder("Multiple Interpretations"));center.add(intScroll);center.add(candidate);
        JPanel right=new JPanel(new BorderLayout(5,5)),confidence=new JPanel(new BorderLayout());confidence.setBorder(BorderFactory.createTitledBorder("Confidence"));confidence.add(confidenceLabel,BorderLayout.CENTER);right.add(confidence,BorderLayout.NORTH);
        JScrollPane evTop=new JScrollPane(evidenceArea),evBottom=new JScrollPane(comparisonsArea);evTop.setBorder(BorderFactory.createTitledBorder("Evidence"));evBottom.setBorder(BorderFactory.createTitledBorder("Comparison Evidence"));
        JSplitPane evSplit=new JSplitPane(JSplitPane.VERTICAL_SPLIT,evTop,evBottom);evSplit.setResizeWeight(.4);right.add(evSplit,BorderLayout.CENTER);
        JSplitPane detailSplit=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,center,right);detailSplit.setResizeWeight(.62);JSplitPane mainSplit=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,listScroll,detailSplit);mainSplit.setResizeWeight(.38);add(mainSplit,BorderLayout.CENTER);add(status,BorderLayout.SOUTH);

        sourceBox.addActionListener(e->{if(sourceBox.getSelectedItem()!=null)loadSource((Source)sourceBox.getSelectedItem());});reload.addActionListener(e->{if(activeSource!=null)loadSource(activeSource);});saveIds.addActionListener(e->saveIds());loadIds.addActionListener(e->loadIds());
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())updateInspector();});candidateDelta.addChangeListener(e->updateCandidate());candidateWidth.addActionListener(e->updateCandidate());editType.addActionListener(e->syncEditValue());applySelected.addActionListener(e->applyCandidate());
        filterField.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
        sourceBox.setPrototypeDisplayValue(new Source("Tactical — DLCPackTactical.layer.0.all.archive","",null,false));
    }

    public void setDetectedArchives(File base,Map<String,File> dlc){baseArchive=base;dlcArchives.clear();if(dlc!=null)dlcArchives.putAll(dlc);rebuildSources();}
    public void setBaseArchive(File base){baseArchive=base;rebuildSources();}
    public File getBaseArchive(){return baseArchive;}
    public Map<String,File> getDlcArchives(){return Collections.unmodifiableMap(dlcArchives);}
    public void reloadCurrent(){if(activeSource!=null)loadSource(activeSource);}

    private void rebuildSources(){
        Source old=(Source)sourceBox.getSelectedItem();String oldProfile=old==null?null:old.profileName;DefaultComboBoxModel<Source> m=new DefaultComboBoxModel<>();
        m.addElement(new Source("Base — Game.layer.1.all.archive","Game.layer.1.all.archive",baseArchive,true));for(String n:DLCProfiles.names())m.addElement(new Source(shortDlc(n)+" — "+n,n,dlcArchives.get(n),false));sourceBox.setModel(m);
        if(oldProfile!=null)for(int i=0;i<m.getSize();i++)if(m.getElementAt(i).profileName.equals(oldProfile)){sourceBox.setSelectedIndex(i);return;}sourceBox.setSelectedIndex(0);
    }

    private void loadSource(Source s){
        activeSource=s;ArrayList<Row> rows=new ArrayList<>();contexts=new String[0];index=null;
        try{
            if(s.base){
                BaseResearchProfiles.Profile p=BaseResearchProfiles.get();
                if(s.file!=null&&s.file.isFile()){try(RandomAccessFile raf=new RandomAccessFile(s.file,"r")){for(BaseResearchProfiles.Field f:p.fields){raf.seek(f.offset);byte[] b=new byte[f.original.length];raf.readFully(b);f.current=b;}}try{contexts=ArchiveContextResolver.resolve(s.file,p.fields);}catch(Exception ex){contexts=new String[p.fields.size()];}try{index=ArchiveResourceIndex.load(s.file);}catch(Exception ignored){}}
                else contexts=new String[p.fields.size()];
                for(int i=0;i<p.fields.size();i++){BaseResearchProfiles.Field f=p.fields.get(i);rows.add(new Row(f.offset,f.original.length,f.label,context(i,ArchiveContextResolver.fallback(f)),f.original,f.current,f));}
            }else{
                DLCProfiles.Profile p=DLCProfiles.get(s.profileName);
                if(s.file!=null&&s.file.isFile()){try(RandomAccessFile raf=new RandomAccessFile(s.file,"r")){for(DLCProfiles.Field f:p.fields){raf.seek(f.offset);byte[] b=new byte[f.original.length];raf.readFully(b);f.current=b;}}try{contexts=DLCArchiveContextResolver.resolve(s.file,p.fields);}catch(Exception ex){contexts=new String[p.fields.size()];}try{index=ArchiveResourceIndex.load(s.file);}catch(Exception ignored){}}
                else contexts=new String[p.fields.size()];
                for(int i=0;i<p.fields.size();i++){DLCProfiles.Field f=p.fields.get(i);rows.add(new Row(f.offset,f.original.length,f.label,context(i,DLCArchiveContextResolver.fallback(f)),f.original,f.current,f));}
            }
            model.setRows(rows);configureColumns();status.setText((s.file!=null&&s.file.isFile()?"Loaded "+s.file.getAbsolutePath():"Profile loaded; archive file not detected")+" | "+rows.size()+" research rows"+(index==null?"":" | "+index.regionCount()+" mapped resource chunks"));if(!rows.isEmpty())table.setRowSelectionInterval(0,0);
        }catch(Exception ex){model.setRows(new ArrayList<>());status.setText("Load failed: "+ex.getMessage());JOptionPane.showMessageDialog(this,ex.getMessage(),"Research Inspector",JOptionPane.ERROR_MESSAGE);}
    }

    private String context(int i,String fallback){return i<contexts.length&&contexts[i]!=null&&!contexts[i].isBlank()?contexts[i]:fallback;}

    private void updateInspector(){
        int vr=table.getSelectedRow();if(vr<0){clearInspector();return;}Row row=model.rows.get(table.convertRowIndexToModel(vr));archiveOffsetField.setText(Long.toString(row.offset));candidateDelta.setValue(suggestDelta(row));candidateWidth.setSelectedItem(suggestWidth(row));
        ArchiveResourceIndex.Location loc=index==null?null:index.locate(row.offset);if(loc==null){resourceField.setText("Unmapped / archive not loaded");chunkField.setText("");resourceOffsetField.setText("");}else{resourceField.setText(loc.resourceName());chunkField.setText(Integer.toString(loc.chunkIndex()));resourceOffsetField.setText(Long.toString(loc.resourceOffset()));}
        FieldConfidenceAudit.Assessment a=assessment(row);confidenceLabel.setText(a.confidence+" — "+a.name);evidenceArea.setText(a.evidence+(a.saveRisk?"\n\nWARNING: save-risk field.":""));evidenceArea.setCaretPosition(0);comparisonsArea.setText(comparisons(row));comparisonsArea.setCaretPosition(0);updateCandidate();
    }

    private int suggestDelta(Row row){
        if(activeSource==null||activeSource.file==null||!activeSource.file.isFile())return 0;
        if(row.length==2&&row.offset>=2)try{if(plausibleFloat(f32(readAt(row.offset-2,4))))return -2;}catch(Exception ignored){}
        if(row.length==3&&row.offset>=1)try{if(plausibleFloat(f32(readAt(row.offset-1,4))))return -1;}catch(Exception ignored){}
        return 0;
    }
    private int suggestWidth(Row row){int d=suggestDelta(row);if(d<0&&(row.length-d)==4)return 4;return row.length==1||row.length==2||row.length==4||row.length==8?row.length:4;}

    private void updateCandidate(){
        int vr=table.getSelectedRow();if(vr<0)return;Row row=model.rows.get(table.convertRowIndexToModel(vr));int delta=((Number)candidateDelta.getValue()).intValue(),width=(Integer)candidateWidth.getSelectedItem();long start=row.offset+delta;
        try{byte[] b=activeSource!=null&&activeSource.file!=null&&activeSource.file.isFile()?readAt(start,width):sliceOriginal(row,delta,width);rawContext.setText(contextHex(start,width));interpretations.setText(describe(start,b,row,delta));interpretations.setCaretPosition(0);syncEditValue();applySelected.setEnabled(activeSource!=null&&activeSource.file!=null&&activeSource.file.isFile());}
        catch(Exception ex){rawContext.setText("Could not read candidate region: "+ex.getMessage());interpretations.setText("");applySelected.setEnabled(false);}
    }

    private String contextHex(long start,int width)throws IOException{
        if(activeSource==null||activeSource.file==null||!activeSource.file.isFile())return "Archive not loaded.";long from=Math.max(0,start-16),to=Math.min(activeSource.file.length(),start+width+16);byte[] b=readAt(from,(int)(to-from));StringBuilder s=new StringBuilder();
        for(int i=0;i<b.length;i++){long off=from+i;if(off==start)s.append("[ ");s.append(String.format("%02X",b[i]&255));if(off==start+width-1)s.append(" ]");s.append(' ');}return s.toString().trim();
    }

    private String describe(long start,byte[] b,Row row,int delta){
        StringBuilder s=new StringBuilder();s.append("Candidate start: ").append(start).append(" (row ").append(delta>=0?"+":"").append(delta).append(")\nWidth: ").append(b.length).append(" byte(s)\nHex: ").append(hexSpaced(b)).append("\n\n");
        if(b.length<=8){s.append("Unsigned LE: ").append(Long.toUnsignedString(unsignedLE(b))).append('\n').append("Signed LE:   ").append(signedLE(b)).append('\n');}if(b.length>=2)s.append("Float16 LE: ").append(HalfFloat.formatLE(new byte[]{b[0],b[1]})).append('\n');if(b.length==4)s.append("Float32 LE: ").append(Float.toString(f32(b))).append('\n');if(b.length==8)s.append("Float64 LE: ").append(Double.toString(ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble())).append('\n');
        if(delta!=0||b.length!=row.length)s.append("\nBOUNDARY NOTE: this interpretation extends beyond the original changed-byte run (row width ").append(row.length).append(").");return s.toString();
    }

    private void syncEditValue(){
        int vr=table.getSelectedRow();if(vr<0)return;Row row=model.rows.get(table.convertRowIndexToModel(vr));int delta=((Number)candidateDelta.getValue()).intValue(),width=(Integer)candidateWidth.getSelectedItem();
        try{byte[] b=activeSource!=null&&activeSource.file!=null&&activeSource.file.isFile()?readAt(row.offset+delta,width):sliceOriginal(row,delta,width);String t=(String)editType.getSelectedItem();if(t.startsWith("Hex"))editValue.setText(hexSpaced(b));else if(t.startsWith("Unsigned"))editValue.setText(Long.toUnsignedString(unsignedLE(b)));else if(t.startsWith("Signed"))editValue.setText(Long.toString(signedLE(b)));else if(t.startsWith("Float16"))editValue.setText(width>=2?HalfFloat.formatLE(new byte[]{b[0],b[1]}):"");else if(t.startsWith("Float32"))editValue.setText(width==4?Float.toString(f32(b)):"");}catch(Exception ex){editValue.setText("");}
    }

    private void applyCandidate(){
        int vr=table.getSelectedRow();if(vr<0||activeSource==null||activeSource.file==null)return;Row row=model.rows.get(table.convertRowIndexToModel(vr));int delta=((Number)candidateDelta.getValue()).intValue(),width=(Integer)candidateWidth.getSelectedItem();long start=row.offset+delta;
        try{byte[] value=parseEdit((String)editType.getSelectedItem(),editValue.getText(),width);String boundary=delta!=0||width!=row.length?"\n\nWARNING: this selection does not match the current research-row boundary. That is allowed for boundary research, but it may affect bytes outside the diff fragment.":"";int ok=JOptionPane.showConfirmDialog(this,"Write "+hexSpaced(value)+" at archive offset "+start+"?"+boundary+"\n\nA .bak will be created/preserved first.","Apply research edit",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(ok!=JOptionPane.YES_OPTION)return;BackupManager.ensureBackup(activeSource.file);try(RandomAccessFile raf=new RandomAccessFile(activeSource.file,"rw")){raf.seek(start);raf.write(value);}loadSource(activeSource);JOptionPane.showMessageDialog(this,"Research edit applied. Backup preserved at "+activeSource.file.getAbsolutePath()+".bak");}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Invalid research edit",JOptionPane.ERROR_MESSAGE);}
    }

    private byte[] parseEdit(String type,String text,int width){
        text=text.trim();if(type.startsWith("Hex")){String h=text.replaceAll("[^0-9A-Fa-f]","");if(h.length()!=width*2)throw new IllegalArgumentException("Expected "+(width*2)+" hex digits.");byte[] b=new byte[width];for(int i=0;i<width;i++)b[i]=(byte)Integer.parseInt(h.substring(i*2,i*2+2),16);return b;}
        if(type.startsWith("Float16")){if(width!=2)throw new IllegalArgumentException("Float16 requires width 2.");return HalfFloat.parseLE(text);}if(type.startsWith("Float32")){if(width!=4)throw new IllegalArgumentException("Float32 requires width 4.");float f=Float.parseFloat(text);int bits=Float.floatToRawIntBits(f);return new byte[]{(byte)bits,(byte)(bits>>>8),(byte)(bits>>>16),(byte)(bits>>>24)};}
        long v=Long.parseLong(text),min=type.startsWith("Signed")&&width<8?-(1L<<(width*8-1)):0,max=type.startsWith("Signed")&&width<8?(1L<<(width*8-1))-1:(width==8?Long.MAX_VALUE:(1L<<(width*8))-1);if(v<min||v>max)throw new IllegalArgumentException("Value does not fit selected width.");byte[] b=new byte[width];for(int i=0;i<width;i++)b[i]=(byte)(v>>>(8*i));return b;
    }

    private String comparisons(Row row){
        StringBuilder s=new StringBuilder();s.append(String.format("%-30s  %-22s  %s%n","Source","Hex","Decoded"));s.append("Original                       ").append(String.format("%-22s",hexSpaced(row.original))).append("  ").append(shortDecode(row.original)).append('\n');
        if(activeSource.base){BaseResearchProfiles.Field f=(BaseResearchProfiles.Field)row.field;for(Map.Entry<String,byte[]>e:f.references.entrySet())if(e.getValue()!=null&&!Arrays.equals(e.getValue(),f.original))s.append(String.format("%-30s  %-22s  %s%n",trim(e.getKey(),30),hexSpaced(e.getValue()),shortDecode(e.getValue())));}
        else{DLCProfiles.Field f=(DLCProfiles.Field)row.field;for(String n:DLCReferenceProfiles.namesFor(activeSource.profileName)){byte[] b=DLCReferenceProfiles.get(activeSource.profileName,n,f.offset,f);if(b!=null&&!Arrays.equals(b,f.original))s.append(String.format("%-30s  %-22s  %s%n",trim(n,30),hexSpaced(b),shortDecode(b)));}}return s.toString();
    }
    private String shortDecode(byte[] b){if(b.length==4)return "F32="+Float.toString(f32(b));if(b.length==2)return "F16="+HalfFloat.formatLE(b)+" / U16="+unsignedLE(b);return "U="+Long.toUnsignedString(unsignedLE(b));}
    private FieldConfidenceAudit.Assessment assessment(Row row){return activeSource.base?FieldConfidenceAudit.assessBase((BaseResearchProfiles.Field)row.field,null,row.context):FieldConfidenceAudit.assessDlc(activeSource.profileName,(DLCProfiles.Field)row.field,row.context);}
    private String confidence(Row row){return assessment(row).confidence.toString().replace('_',' ');}private String displayName(Row row){return assessment(row).name;}

    private void saveIds(){if(activeSource==null)return;if(activeSource.base)FieldIdentificationIO.exportBase(this,BaseResearchProfiles.get());else FieldIdentificationIO.exportDlc(this,activeSource.profileName,DLCProfiles.get(activeSource.profileName));}
    private void loadIds(){if(activeSource==null)return;if(activeSource.base)FieldIdentificationIO.importBase(this,BaseResearchProfiles.get());else FieldIdentificationIO.importDlc(this,activeSource.profileName,DLCProfiles.get(activeSource.profileName));model.fireTableDataChanged();}
    private String userId(Row row){return activeSource.base?((BaseResearchProfiles.Field)row.field).identifiedAs:((DLCProfiles.Field)row.field).identifiedAs;}
    private void setUserId(Row row,String v){if(activeSource.base)((BaseResearchProfiles.Field)row.field).identifiedAs=v;else((DLCProfiles.Field)row.field).identifiedAs=v;}
    private void filter(){String q=filterField.getText().trim();sorter.setRowFilter(q.isEmpty()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(q)));}
    private void configureColumns(){if(table.getColumnCount()<1)return;int[] w={125,360,150,220,170,75,120,110};for(int i=0;i<table.getColumnCount()&&i<w.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);for(int i=0;i<table.getColumnCount();i++)sorter.setSortable(i,false);}
    private void clearInspector(){for(JTextField f:new JTextField[]{resourceField,chunkField,resourceOffsetField,archiveOffsetField})f.setText("");rawContext.setText("");interpretations.setText("");evidenceArea.setText("");comparisonsArea.setText("");confidenceLabel.setText("No row selected");}
    private byte[] readAt(long off,int len)throws IOException{if(off<0||off+len>activeSource.file.length())throw new IOException("Selection outside archive bounds.");byte[] b=new byte[len];try(RandomAccessFile raf=new RandomAccessFile(activeSource.file,"r")){raf.seek(off);raf.readFully(b);}return b;}
    private byte[] sliceOriginal(Row r,int delta,int width){if(delta<0||delta+width>r.original.length)return new byte[width];return Arrays.copyOfRange(r.original,delta,delta+width);}
    private static boolean plausibleFloat(float f){return Float.isFinite(f)&&Math.abs(f)>=0.000001f&&Math.abs(f)<=100000f;}
    private static float f32(byte[] b){return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();}
    private static long unsignedLE(byte[] b){long v=0;for(int i=0;i<b.length&&i<8;i++)v|=((long)b[i]&255L)<<(8*i);return v;}
    private static long signedLE(byte[] b){long v=unsignedLE(b);if(b.length>=8)return v;long sign=1L<<(b.length*8-1);return(v&sign)==0?v:v-(1L<<(b.length*8));}
    private static String hexSpaced(byte[] b){StringBuilder s=new StringBuilder();for(int i=0;i<b.length;i++){if(i>0)s.append(' ');s.append(String.format("%02X",b[i]&255));}return s.toString();}
    private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,n-1)+"…";}private static String shortDlc(String n){return n.replace("DLCPack","").replace(".layer.0.all.archive","");}private static JTextField readonly(){JTextField f=new JTextField();f.setEditable(false);return f;}

    private final class ResearchModel extends AbstractTableModel{
        private final String[] columns={"Status","Attribute","User ID","Context","Internal Resource","Chunk","Resource Offset","Archive Offset"};private ArrayList<Row> rows=new ArrayList<>();
        void setRows(ArrayList<Row> r){rows=r;fireTableStructureChanged();SwingUtilities.invokeLater(ResearchInspectorPanel.this::configureColumns);}public int getRowCount(){return rows.size();}public int getColumnCount(){return columns.length;}public String getColumnName(int c){return columns[c];}
        public Object getValueAt(int r,int c){Row row=rows.get(r);ArchiveResourceIndex.Location loc=index==null?null:index.locate(row.offset);return switch(c){case 0->confidence(row);case 1->displayName(row);case 2->userId(row);case 3->row.context;case 4->loc==null?"":loc.resourceName();case 5->loc==null?"":loc.chunkIndex();case 6->loc==null?"":loc.resourceOffset();case 7->row.offset;default->"";};}
        public boolean isCellEditable(int r,int c){return c==2;}public void setValueAt(Object v,int r,int c){if(c==2){setUserId(rows.get(r),String.valueOf(v).trim());fireTableCellUpdated(r,c);}}
    }
}
