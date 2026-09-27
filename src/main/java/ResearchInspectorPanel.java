import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@SuppressWarnings("serial")
public class ResearchInspectorPanel extends JPanel {
    private final JComboBox<Source> sourceBox = new JComboBox<>();
    private final JTextField filterField = new JTextField(22);
    private final JLabel status = new JLabel("Select DXMD.exe to load archive research data.");
    private final ResearchModel model = new ResearchModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<ResearchModel> sorter = new TableRowSorter<>(model);

    private final JTextField resourceField = readonly();
    private final JTextField chunkField = readonly();
    private final JTextField resourceOffsetField = readonly();
    private final JTextField archiveOffsetField = readonly();

    private final JTextField logicalResourceField = readonly();
    private final JTextField headerLibField = readonly();
    private final JTextField resourceIdField = readonly();
    private final JTextField ownerIdField = readonly();
    private final JTextField payloadOffsetField = readonly();
    private final JTextField payloadLengthField = readonly();
    private final JTextField resourceTypeField = readonly();
    private final JTextField flagsField = readonly();

    private final JTextArea rawContext = new JTextArea(4, 50);
    private final JTextArea interpretations = new JTextArea(9, 50);
    private final JTextArea evidenceArea = new JTextArea(8, 34);
    private final ComparisonModel comparisonModel = new ComparisonModel();
    private final JTable comparisonsTable = new JTable(comparisonModel);
    private final JLabel confidenceLabel = new JLabel("No row selected");
    private final JSpinner candidateDelta = new JSpinner(new SpinnerNumberModel(0, -8, 8, 1));
    private final JComboBox<Integer> candidateWidth = new JComboBox<>(new Integer[]{1, 2, 4, 8});
    private final JComboBox<String> editType = new JComboBox<>(new String[]{
            "Hex bytes", "Unsigned integer (LE)", "Signed integer (LE)", "Float16 (LE)", "Float32 (LE)"});
    private final JTextField editValue = new JTextField(18);
    private final JButton applySelected = new JButton("Apply Candidate");

    private File baseArchive;
    private final LinkedHashMap<String, File> dlcArchives = new LinkedHashMap<>();
    private Source activeSource;
    private ArchiveResourceIndex index;
    private String[] contexts = new String[0];
    private boolean rebuildingSources;

    private record Source(String title, String profileName, File file, boolean base) {
        @Override public String toString() { return title; }
    }

    private record Row(long offset, int length, String label, String context,
                       byte[] original, byte[] current, Object field) {}

    public ResearchInspectorPanel() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(8, 4));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        left.add(new JLabel("Archive:"));
        left.add(sourceBox);
        left.add(new JLabel("Filter:"));
        left.add(filterField);
        JButton reload = new JButton("Reload");
        JButton saveIds = new JButton("Save IDs...");
        JButton loadIds = new JButton("Load IDs...");
        left.add(reload);
        left.add(saveIds);
        left.add(loadIds);
        top.add(left, BorderLayout.WEST);
        top.add(new JLabel("Changed-byte runs are evidence, not field boundaries."), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Collections.emptyList());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configureColumns();
        JScrollPane listScroll = new JScrollPane(table);
        listScroll.setPreferredSize(new Dimension(650, 600));

        rawContext.setEditable(false);
        rawContext.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawContext.setLineWrap(true);
        rawContext.setWrapStyleWord(true);
        interpretations.setEditable(false);
        interpretations.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        evidenceArea.setEditable(false);
        evidenceArea.setLineWrap(true);
        evidenceArea.setWrapStyleWord(true);
        comparisonsTable.setRowHeight(22);
        comparisonsTable.setFillsViewportHeight(true);
        comparisonsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        comparisonsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        comparisonsTable.getTableHeader().setReorderingAllowed(false);

        JPanel identity = new JPanel(new GridLayout(4, 2, 6, 3));
        identity.setBorder(BorderFactory.createTitledBorder("Selected Region"));
        identity.add(new JLabel("Internal Resource")); identity.add(resourceField);
        identity.add(new JLabel("Chunk")); identity.add(chunkField);
        identity.add(new JLabel("Resource Offset")); identity.add(resourceOffsetField);
        identity.add(new JLabel("Archive Offset")); identity.add(archiveOffsetField);

        JPanel structure = new JPanel(new GridLayout(8, 2, 6, 3));
        structure.setBorder(BorderFactory.createTitledBorder("Resource Structure"));
        structure.add(new JLabel("Logical Resource")); structure.add(logicalResourceField);
        structure.add(new JLabel("HeaderLib")); structure.add(headerLibField);
        structure.add(new JLabel("Resource ID")); structure.add(resourceIdField);
        structure.add(new JLabel("Owner ID")); structure.add(ownerIdField);
        structure.add(new JLabel("Payload Offset")); structure.add(payloadOffsetField);
        structure.add(new JLabel("Payload Length")); structure.add(payloadLengthField);
        structure.add(new JLabel("Resource Type / Magic")); structure.add(resourceTypeField);
        structure.add(new JLabel("Flags")); structure.add(flagsField);

        JPanel candidate = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        candidate.setBorder(BorderFactory.createTitledBorder("Candidate"));
        candidate.add(new JLabel("Delta:")); candidate.add(candidateDelta);
        candidate.add(new JLabel("Width:")); candidate.add(candidateWidth);
        candidate.add(new JLabel("Type:")); candidate.add(editType);
        candidate.add(editValue); candidate.add(applySelected);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(identity);
        center.add(structure);
        JScrollPane rawScroll = new JScrollPane(rawContext);
        rawScroll.setBorder(BorderFactory.createTitledBorder("Raw Context"));
        center.add(rawScroll);
        JScrollPane intScroll = new JScrollPane(interpretations);
        intScroll.setBorder(BorderFactory.createTitledBorder("Interpretations"));
        center.add(intScroll);
        center.add(candidate);

        JPanel right = new JPanel(new BorderLayout(5, 5));
        JPanel confidence = new JPanel(new BorderLayout());
        confidence.setBorder(BorderFactory.createTitledBorder("Confidence"));
        confidence.add(confidenceLabel, BorderLayout.CENTER);
        right.add(confidence, BorderLayout.NORTH);
        JScrollPane evTop = new JScrollPane(evidenceArea);
        JScrollPane evBottom = new JScrollPane(comparisonsTable);
        configureComparisonColumns();
        evTop.setBorder(BorderFactory.createTitledBorder("Evidence"));
        evBottom.setBorder(BorderFactory.createTitledBorder("Comparisons"));
        JSplitPane evSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, evTop, evBottom);
        evSplit.setResizeWeight(.4);
        right.add(evSplit, BorderLayout.CENTER);

        JSplitPane detailSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, center, right);
        detailSplit.setResizeWeight(.64);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailSplit);
        mainSplit.setResizeWeight(.42);
        add(mainSplit, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        sourceBox.addActionListener(e -> {
            if (!rebuildingSources && sourceBox.getSelectedItem() != null)
                loadSourceAsync((Source) sourceBox.getSelectedItem());
        });
        reload.addActionListener(e -> { if (activeSource != null) loadSourceAsync(activeSource); });
        saveIds.addActionListener(e -> saveIds());
        loadIds.addActionListener(e -> loadIds());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateInspector();
        });
        candidateDelta.addChangeListener(e -> updateCandidate());
        candidateWidth.addActionListener(e -> updateCandidate());
        editType.addActionListener(e -> syncEditValue());
        applySelected.addActionListener(e -> applyCandidate());
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        sourceBox.setPrototypeDisplayValue(new Source(
                "Tactical", "", null, false));
    }

    public void setDetectedArchives(File base, Map<String, File> dlc) {
        baseArchive = base;
        dlcArchives.clear();
        if (dlc != null) dlcArchives.putAll(dlc);
        rebuildSources();
    }

    public void setBaseArchive(File base) { baseArchive = base; rebuildSources(); }
    public File getBaseArchive() { return baseArchive; }
    public Map<String, File> getDlcArchives() { return Collections.unmodifiableMap(dlcArchives); }
    public void reloadCurrent() { if (activeSource != null) loadSource(activeSource); }

    private void rebuildSources() {
        Source old = (Source) sourceBox.getSelectedItem();
        String oldProfile = old == null ? null : old.profileName;
        rebuildingSources = true;
        try {
            DefaultComboBoxModel<Source> m = new DefaultComboBoxModel<>();
            m.addElement(new Source("Base", "Game.layer.1.all.archive", baseArchive, true));
            for (String n : DLCProfiles.names()) m.addElement(new Source(shortDlc(n), n, dlcArchives.get(n), false));
            sourceBox.setModel(m);
            int selected = 0;
            if (oldProfile != null) for (int i = 0; i < m.getSize(); i++)
                if (m.getElementAt(i).profileName.equals(oldProfile)) { selected = i; break; }
            sourceBox.setSelectedIndex(selected);
        } finally { rebuildingSources = false; }
        Source selected = (Source) sourceBox.getSelectedItem();
        if (selected != null) loadSource(selected);
    }

    private void loadSourceAsync(Source s) {
        if (s == null) return;
        if (s.file == null || !s.file.isFile()) { loadSource(s); return; }
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Loading " + s.title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        JLabel message = new JLabel("Preparing archive...");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true); bar.setStringPainted(true); bar.setString("Working");
        bar.setPreferredSize(new Dimension(360,22));
        final int[] dots={0};
        javax.swing.Timer animation=new javax.swing.Timer(260,e->{dots[0]=(dots[0]+1)%4;bar.setString("Working"+".".repeat(dots[0]));});
        animation.start();
        panel.add(message,BorderLayout.NORTH); panel.add(bar,BorderLayout.CENTER);
        dialog.setContentPane(panel); dialog.pack(); dialog.setResizable(false); dialog.setLocationRelativeTo(this); UiTheme.apply(dialog);
        SwingWorker<Void,String> worker=new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                publish("Indexing resources..."); ArchiveResourceIndex.load(s.file);
                publish("Scanning identifiers...");
                if (s.base) ArchiveContextResolver.resolve(s.file,BaseResearchProfiles.get().fields);
                else DLCArchiveContextResolver.resolve(s.file,DLCProfiles.get(s.profileName).fields);
                publish("Mapping structure..."); LogicalResourceCatalog.prewarm();
                return null;
            }
            @Override protected void process(java.util.List<String> chunks){if(!chunks.isEmpty())message.setText(chunks.get(chunks.size()-1));}
            @Override protected void done(){
                animation.stop();
                try { get(); bar.setIndeterminate(false); bar.setValue(100); bar.setString("Ready"); loadSource(s); }
                catch(Exception ex){Throwable c=ex.getCause()==null?ex:ex.getCause();JOptionPane.showMessageDialog(ResearchInspectorPanel.this,c.getMessage()==null?c.toString():c.getMessage(),"Archive load error",JOptionPane.ERROR_MESSAGE);}
                finally { dialog.dispose(); }
            }
        };
        worker.execute(); dialog.setVisible(true);
    }

    private void loadSource(Source s) {
        activeSource = s;
        ArrayList<Row> rows = new ArrayList<>();
        contexts = new String[0];
        index = null;
        try {
            if (s.base) {
                BaseResearchProfiles.Profile p = BaseResearchProfiles.get();
                if (s.file != null && s.file.isFile()) {
                    try (RandomAccessFile raf = new RandomAccessFile(s.file, "r")) {
                        for (BaseResearchProfiles.Field f : p.fields) {
                            raf.seek(f.offset);
                            byte[] b = new byte[f.original.length];
                            raf.readFully(b);
                            f.current = b;
                        }
                    }
                    try { contexts = ArchiveContextResolver.resolve(s.file, p.fields); }
                    catch (Exception ex) { contexts = new String[p.fields.size()]; }
                    try { index = ArchiveResourceIndex.load(s.file); }
                    catch (Exception ignored) {}
                } else contexts = new String[p.fields.size()];
                for (int i = 0; i < p.fields.size(); i++) {
                    BaseResearchProfiles.Field f = p.fields.get(i);
                    rows.add(new Row(f.offset, f.original.length, f.label,
                            context(i, ArchiveContextResolver.fallback(f)), f.original, f.current, f));
                }
            } else {
                DLCProfiles.Profile p = DLCProfiles.get(s.profileName);
                if (s.file != null && s.file.isFile()) {
                    try (RandomAccessFile raf = new RandomAccessFile(s.file, "r")) {
                        for (DLCProfiles.Field f : p.fields) {
                            raf.seek(f.offset);
                            byte[] b = new byte[f.original.length];
                            raf.readFully(b);
                            f.current = b;
                        }
                    }
                    try { contexts = DLCArchiveContextResolver.resolve(s.file, p.fields); }
                    catch (Exception ex) { contexts = new String[p.fields.size()]; }
                    try { index = ArchiveResourceIndex.load(s.file); }
                    catch (Exception ignored) {}
                } else contexts = new String[p.fields.size()];
                for (int i = 0; i < p.fields.size(); i++) {
                    DLCProfiles.Field f = p.fields.get(i);
                    rows.add(new Row(f.offset, f.original.length, f.label,
                            context(i, DLCArchiveContextResolver.fallback(f)), f.original, f.current, f));
                }
            }

            model.setRows(rows);
            configureColumns();
            int structured = countStructured(rows);
            status.setText(s.title + " | " + rows.size() + " rows | Structure " + structured + "/" + rows.size()
                    + (s.file != null && s.file.isFile() ? " | " + s.file.getAbsolutePath() : " | archive not detected"));
            if (!rows.isEmpty()) table.setRowSelectionInterval(0, 0);
        } catch (Exception ex) {
            model.setRows(new ArrayList<>());
            status.setText("Load failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Research Inspector", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int countStructured(ArrayList<Row> rows) {
        if (index == null) return 0;
        int n = 0;
        for (Row row : rows) {
            ArchiveResourceIndex.Location loc = index.locate(row.offset);
            if (logical(loc) != null) n++;
        }
        return n;
    }

    private String context(int i, String fallback) {
        return i < contexts.length && contexts[i] != null && !contexts[i].isBlank() ? contexts[i] : fallback;
    }

    private LogicalResourceCatalog.LogicalResource logical(ArchiveResourceIndex.Location loc) {
        return loc == null ? null : LogicalResourceCatalog.locate(loc.resourceName(), loc.resourceOffset());
    }

    private void updateInspector() {
        int vr = table.getSelectedRow();
        if (vr < 0) { clearInspector(); return; }
        Row row = model.rows.get(table.convertRowIndexToModel(vr));
        archiveOffsetField.setText(Long.toString(row.offset));
        candidateDelta.setValue(suggestDelta(row));
        candidateWidth.setSelectedItem(suggestWidth(row));

        ArchiveResourceIndex.Location loc = index == null ? null : index.locate(row.offset);
        if (loc == null) {
            resourceField.setText("Unmapped / archive not loaded");
            chunkField.setText("");
            resourceOffsetField.setText("");
        } else {
            resourceField.setText(loc.resourceName());
            chunkField.setText(Integer.toString(loc.chunkIndex()));
            resourceOffsetField.setText(Long.toString(loc.resourceOffset()));
        }
        updateStructure(loc);

        FieldConfidenceAudit.Assessment a = assessment(row);
        confidenceLabel.setText(a.confidence.toString().replace('_', ' ') + " — " + FieldConfidenceAudit.compactDisplayName(a, row.label));
        String evidence = a.evidence + (a.saveRisk ? "\n\nWARNING: save-risk field." : "");
        LogicalResourceCatalog.LogicalResource lr = logical(loc);
        if (lr != null) {
            long po = lr.payloadOffset(loc.resourceOffset());
            evidence += "\n\nStructure: " + lr.logicalPath() + " @ payload +0x"
                    + Long.toHexString(po).toUpperCase(Locale.ROOT)
                    + ". Location alone does not prove gameplay meaning.";
        }
        evidenceArea.setText(evidence);
        evidenceArea.setCaretPosition(0);
        comparisonModel.setRows(comparisonRows(row));
        updateCandidate();
    }

    private void updateStructure(ArchiveResourceIndex.Location loc) {
        LogicalResourceCatalog.LogicalResource lr = logical(loc);
        if (lr == null || loc == null) {
            logicalResourceField.setText(loc == null ? "" : "HeaderLib metadata unavailable for this resource library");
            headerLibField.setText("");
            resourceIdField.setText("");
            ownerIdField.setText("");
            payloadOffsetField.setText("");
            payloadLengthField.setText("");
            resourceTypeField.setText("");
            flagsField.setText("");
            return;
        }
        logicalResourceField.setText(lr.logicalPath());
        headerLibField.setText(lr.headerLib() + " / library " + lr.libraryIndex());
        resourceIdField.setText(lr.resourceIdHex());
        ownerIdField.setText(lr.ownerIdHex());
        long payloadOffset = lr.payloadOffset(loc.resourceOffset());
        payloadOffsetField.setText(String.format("+0x%X (%d)", payloadOffset, payloadOffset));
        payloadLengthField.setText(String.format("%,d bytes", lr.payloadLength()));
        resourceTypeField.setText(lr.typeName() + " / " + lr.magicHex());
        flagsField.setText(lr.flagsHex());
    }

    private int suggestDelta(Row row) {
        if (activeSource == null || activeSource.file == null || !activeSource.file.isFile()) return 0;
        if (row.length == 2 && row.offset >= 2) {
            try { if (plausibleFloat(f32(readAt(row.offset - 2, 4)))) return -2; }
            catch (Exception ignored) {}
        }
        if (row.length == 3 && row.offset >= 1) {
            try { if (plausibleFloat(f32(readAt(row.offset - 1, 4)))) return -1; }
            catch (Exception ignored) {}
        }
        return 0;
    }

    private int suggestWidth(Row row) {
        int d = suggestDelta(row);
        if (d < 0 && (row.length - d) == 4) return 4;
        return row.length == 1 || row.length == 2 || row.length == 4 || row.length == 8 ? row.length : 4;
    }

    private void updateCandidate() {
        int vr = table.getSelectedRow();
        if (vr < 0) return;
        Row row = model.rows.get(table.convertRowIndexToModel(vr));
        int delta = ((Number) candidateDelta.getValue()).intValue();
        int width = (Integer) candidateWidth.getSelectedItem();
        long start = row.offset + delta;
        try {
            byte[] b = activeSource != null && activeSource.file != null && activeSource.file.isFile()
                    ? readAt(start, width) : sliceOriginal(row, delta, width);
            rawContext.setText(contextHex(start, width));
            interpretations.setText(describe(start, b, row, delta));
            interpretations.setCaretPosition(0);
            syncEditValue();
            applySelected.setEnabled(activeSource != null && activeSource.file != null && activeSource.file.isFile());
        } catch (Exception ex) {
            rawContext.setText("Could not read candidate region: " + ex.getMessage());
            interpretations.setText("");
            applySelected.setEnabled(false);
        }
    }

    private String contextHex(long start, int width) throws IOException {
        if (activeSource == null || activeSource.file == null || !activeSource.file.isFile()) return "Archive not loaded.";
        long from = Math.max(0, start - 16);
        long to = Math.min(activeSource.file.length(), start + width + 16);
        byte[] b = readAt(from, (int) (to - from));
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            long off = from + i;
            if (off == start) s.append("[ ");
            s.append(String.format("%02X", b[i] & 255));
            if (off == start + width - 1) s.append(" ]");
            s.append(' ');
        }
        return s.toString().trim();
    }

    private String describe(long start, byte[] b, Row row, int delta) {
        StringBuilder s = new StringBuilder();
        s.append("Candidate start: ").append(start).append(" (row ")
                .append(delta >= 0 ? "+" : "").append(delta).append(")\n")
                .append("Width: ").append(b.length).append(" byte(s)\n")
                .append("Hex: ").append(hexSpaced(b)).append("\n");

        ArchiveResourceIndex.Location loc = index == null ? null : index.locate(start);
        LogicalResourceCatalog.LogicalResource lr = logical(loc);
        if (lr != null && loc != null) {
            long po = lr.payloadOffset(loc.resourceOffset());
            s.append("Logical payload offset: +0x")
                    .append(Long.toHexString(po).toUpperCase(Locale.ROOT))
                    .append(" (").append(po).append(")\n")
                    .append("Logical resource: ").append(lr.logicalPath()).append("\n");
        }
        s.append('\n');

        if (b.length <= 8) {
            s.append("Unsigned LE: ").append(Long.toUnsignedString(unsignedLE(b))).append('\n')
                    .append("Signed LE:   ").append(signedLE(b)).append('\n');
        }
        if (b.length >= 2) s.append("Float16 LE: ").append(HalfFloat.formatLE(new byte[]{b[0], b[1]})).append('\n');
        if (b.length == 4) s.append("Float32 LE: ").append(Float.toString(f32(b))).append('\n');
        if (b.length == 8) s.append("Float64 LE: ")
                .append(Double.toString(ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble())).append('\n');
        if (delta != 0 || b.length != row.length)
            s.append("\nBoundary differs from diff run (row width ").append(row.length).append(").");
        return s.toString();
    }

    private void syncEditValue() {
        int vr = table.getSelectedRow();
        if (vr < 0) return;
        Row row = model.rows.get(table.convertRowIndexToModel(vr));
        int delta = ((Number) candidateDelta.getValue()).intValue();
        int width = (Integer) candidateWidth.getSelectedItem();
        try {
            byte[] b = activeSource != null && activeSource.file != null && activeSource.file.isFile()
                    ? readAt(row.offset + delta, width) : sliceOriginal(row, delta, width);
            String t = (String) editType.getSelectedItem();
            if (t.startsWith("Hex")) editValue.setText(hexSpaced(b));
            else if (t.startsWith("Unsigned")) editValue.setText(Long.toUnsignedString(unsignedLE(b)));
            else if (t.startsWith("Signed")) editValue.setText(Long.toString(signedLE(b)));
            else if (t.startsWith("Float16")) editValue.setText(width >= 2 ? HalfFloat.formatLE(new byte[]{b[0], b[1]}) : "");
            else if (t.startsWith("Float32")) editValue.setText(width == 4 ? Float.toString(f32(b)) : "");
        } catch (Exception ex) {
            editValue.setText("");
        }
    }

    private void applyCandidate() {
        int vr = table.getSelectedRow();
        if (vr < 0 || activeSource == null || activeSource.file == null) return;
        Row row = model.rows.get(table.convertRowIndexToModel(vr));
        int delta = ((Number) candidateDelta.getValue()).intValue();
        int width = (Integer) candidateWidth.getSelectedItem();
        long start = row.offset + delta;
        try {
            byte[] value = parseEdit((String) editType.getSelectedItem(), editValue.getText(), width);
            String boundary = delta != 0 || width != row.length
                    ? "\n\nWARNING: this selection does not match the current research-row boundary. That is allowed for boundary research, but it may affect bytes outside the diff fragment."
                    : "";
            int ok = JOptionPane.showConfirmDialog(this,
                    "Write " + hexSpaced(value) + " at archive offset " + start + "?" + boundary
                            + "\n\nA .bak will be created/preserved first.",
                    "Apply research edit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            BackupManager.ensureBackup(activeSource.file);
            try (RandomAccessFile raf = new RandomAccessFile(activeSource.file, "rw")) {
                raf.seek(start);
                raf.write(value);
            }
            loadSource(activeSource);
            JOptionPane.showMessageDialog(this,
                    "Research edit applied. Backup preserved at " + activeSource.file.getAbsolutePath() + ".bak");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid research edit", JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] parseEdit(String type, String text, int width) {
        text = text.trim();
        if (type.startsWith("Hex")) {
            String h = text.replaceAll("[^0-9A-Fa-f]", "");
            if (h.length() != width * 2) throw new IllegalArgumentException("Expected " + (width * 2) + " hex digits.");
            byte[] b = new byte[width];
            for (int i = 0; i < width; i++) b[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            return b;
        }
        if (type.startsWith("Float16")) {
            if (width != 2) throw new IllegalArgumentException("Float16 requires width 2.");
            return HalfFloat.parseLE(text);
        }
        if (type.startsWith("Float32")) {
            if (width != 4) throw new IllegalArgumentException("Float32 requires width 4.");
            float f = Float.parseFloat(text);
            int bits = Float.floatToRawIntBits(f);
            return new byte[]{(byte) bits, (byte) (bits >>> 8), (byte) (bits >>> 16), (byte) (bits >>> 24)};
        }
        boolean signed = type.startsWith("Signed");
        int bits = width * 8;
        BigInteger value = new BigInteger(text);
        BigInteger modulus = BigInteger.ONE.shiftLeft(bits);
        BigInteger min = signed ? BigInteger.ONE.shiftLeft(bits - 1).negate() : BigInteger.ZERO;
        BigInteger max = signed ? BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE) : modulus.subtract(BigInteger.ONE);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) throw new IllegalArgumentException("Value does not fit selected width.");
        if (value.signum() < 0) value = value.add(modulus);
        byte[] b = new byte[width];
        for (int i = 0; i < width; i++) b[i] = value.shiftRight(8 * i).byteValue();
        return b;
    }

    private ArrayList<ComparisonRow> comparisonRows(Row row) {
        ArrayList<ComparisonRow> rows = new ArrayList<>();
        rows.add(new ComparisonRow("Original", hexSpaced(row.original), shortDecode(row.original)));
        if (activeSource.base) {
            BaseResearchProfiles.Field f = (BaseResearchProfiles.Field) row.field;
            for (Map.Entry<String, byte[]> e : f.references.entrySet()) {
                if (e.getValue() != null && !Arrays.equals(e.getValue(), f.original))
                    rows.add(new ComparisonRow(e.getKey(), hexSpaced(e.getValue()), shortDecode(e.getValue())));
            }
        } else {
            DLCProfiles.Field f = (DLCProfiles.Field) row.field;
            for (String n : DLCReferenceProfiles.namesFor(activeSource.profileName)) {
                byte[] b = DLCReferenceProfiles.get(activeSource.profileName, n, f.offset, f);
                if (b != null && !Arrays.equals(b, f.original))
                    rows.add(new ComparisonRow(n, hexSpaced(b), shortDecode(b)));
            }
        }
        return rows;
    }

    private String shortDecode(byte[] b) {
        if (b.length == 4) return "F32=" + Float.toString(f32(b));
        if (b.length == 2) return "F16=" + HalfFloat.formatLE(b) + " / U16=" + unsignedLE(b);
        return "U=" + Long.toUnsignedString(unsignedLE(b));
    }

    private FieldConfidenceAudit.Assessment assessment(Row row) {
        return activeSource.base
                ? FieldConfidenceAudit.assessBase((BaseResearchProfiles.Field) row.field, null, row.context)
                : FieldConfidenceAudit.assessDlc(activeSource.profileName, (DLCProfiles.Field) row.field, row.context);
    }

    private String confidence(Row row) { return assessment(row).confidence.toString().replace('_', ' '); }
    private String displayName(Row row) {
        return FieldConfidenceAudit.compactDisplayName(assessment(row), row.label);
    }

    private void saveIds() {
        if (activeSource == null) return;
        if (activeSource.base) FieldIdentificationIO.exportBase(this, BaseResearchProfiles.get());
        else FieldIdentificationIO.exportDlc(this, activeSource.profileName, DLCProfiles.get(activeSource.profileName));
    }

    private void loadIds() {
        if (activeSource == null) return;
        if (activeSource.base) FieldIdentificationIO.importBase(this, BaseResearchProfiles.get());
        else FieldIdentificationIO.importDlc(this, activeSource.profileName, DLCProfiles.get(activeSource.profileName));
        model.fireTableDataChanged();
    }

    private String userId(Row row) {
        return activeSource.base ? ((BaseResearchProfiles.Field) row.field).identifiedAs
                : ((DLCProfiles.Field) row.field).identifiedAs;
    }

    private void setUserId(Row row, String v) {
        if (activeSource.base) ((BaseResearchProfiles.Field) row.field).identifiedAs = v;
        else ((DLCProfiles.Field) row.field).identifiedAs = v;
    }

    private void filter() {
        String q = filterField.getText().trim();
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
    }

    private void configureColumns() {
        if (table.getColumnCount() < 1) return;
        int[] w = {125, 360, 150};
        for (int i = 0; i < table.getColumnCount() && i < w.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        for (int i = 0; i < table.getColumnCount(); i++) sorter.setSortable(i, false);
    }

    private void configureComparisonColumns() {
        if (comparisonsTable.getColumnCount() != 3) return;
        comparisonsTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        comparisonsTable.getColumnModel().getColumn(0).setMinWidth(120);
        comparisonsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        comparisonsTable.getColumnModel().getColumn(1).setMinWidth(100);
        comparisonsTable.getColumnModel().getColumn(2).setPreferredWidth(360);
        comparisonsTable.getColumnModel().getColumn(2).setMinWidth(180);
    }

    private void clearInspector() {
        for (JTextField f : new JTextField[]{resourceField, chunkField, resourceOffsetField, archiveOffsetField,
                logicalResourceField, headerLibField, resourceIdField, ownerIdField, payloadOffsetField,
                payloadLengthField, resourceTypeField, flagsField}) f.setText("");
        rawContext.setText("");
        interpretations.setText("");
        evidenceArea.setText("");
        comparisonModel.setRows(new ArrayList<>());
        confidenceLabel.setText("No row selected");
    }

    private byte[] readAt(long off, int len) throws IOException {
        if (off < 0 || off + len > activeSource.file.length()) throw new IOException("Selection outside archive bounds.");
        byte[] b = new byte[len];
        try (RandomAccessFile raf = new RandomAccessFile(activeSource.file, "r")) {
            raf.seek(off);
            raf.readFully(b);
        }
        return b;
    }

    private byte[] sliceOriginal(Row r, int delta, int width) {
        if (delta < 0 || delta + width > r.original.length)
            throw new IllegalArgumentException("Load the archive to inspect bytes outside the diff fragment.");
        return Arrays.copyOfRange(r.original, delta, delta + width);
    }

    private static boolean plausibleFloat(float f) {
        return Float.isFinite(f) && Math.abs(f) >= 0.000001f && Math.abs(f) <= 100000f;
    }
    private static float f32(byte[] b) { return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat(); }
    private static long unsignedLE(byte[] b) {
        long v = 0;
        for (int i = 0; i < b.length && i < 8; i++) v |= ((long) b[i] & 255L) << (8 * i);
        return v;
    }
    private static long signedLE(byte[] b) {
        long v = unsignedLE(b);
        if (b.length >= 8) return v;
        long sign = 1L << (b.length * 8 - 1);
        return (v & sign) == 0 ? v : v - (1L << (b.length * 8));
    }
    private static String hexSpaced(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) s.append(' ');
            s.append(String.format("%02X", b[i] & 255));
        }
        return s.toString();
    }
    private static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }
    private static String shortDlc(String n) { return n.replace("DLCPack", "").replace(".layer.0.all.archive", ""); }
    private static JTextField readonly() { JTextField f = new JTextField(); f.setEditable(false); return f; }

    private record ComparisonRow(String source, String hex, String decoded) {}

    private static final class ComparisonModel extends AbstractTableModel {
        private final String[] columns = {"Source", "Hex", "Decoded"};
        private ArrayList<ComparisonRow> rows = new ArrayList<>();

        void setRows(ArrayList<ComparisonRow> newRows) {
            rows = newRows == null ? new ArrayList<>() : newRows;
            fireTableDataChanged();
        }

        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int c) { return columns[c]; }
        public Object getValueAt(int r, int c) {
            ComparisonRow row = rows.get(r);
            return switch (c) {
                case 0 -> row.source();
                case 1 -> row.hex();
                case 2 -> row.decoded();
                default -> "";
            };
        }
    }

    private final class ResearchModel extends AbstractTableModel {
        private final String[] columns = {"Status", "Attribute", "User ID"};
        private ArrayList<Row> rows = new ArrayList<>();

        void setRows(ArrayList<Row> r) {
            rows = r;
            fireTableStructureChanged();
            SwingUtilities.invokeLater(ResearchInspectorPanel.this::configureColumns);
        }
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int c) { return columns[c]; }

        public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            return switch (c) {
                case 0 -> confidence(row);
                case 1 -> displayName(row);
                case 2 -> userId(row);
                default -> "";
            };
        }

        public boolean isCellEditable(int r, int c) { return c == 2; }
        public void setValueAt(Object v, int r, int c) {
            if (c == 2) {
                setUserId(rows.get(r), String.valueOf(v).trim());
                fireTableCellUpdated(r, c);
            }
        }
    }
}
