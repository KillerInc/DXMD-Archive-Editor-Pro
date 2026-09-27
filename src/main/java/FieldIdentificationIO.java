import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Import/export of user-supplied Research Inspector field IDs. */
public final class FieldIdentificationIO {
    private FieldIdentificationIO() {}

    public static String baseFieldId(BaseResearchProfiles.Field f) {
        return "BASE-" + f.offset + "-" + String.format(Locale.ROOT, "%02d", f.original.length);
    }

    public static String dlcFieldId(String profileName, DLCProfiles.Field f) {
        String pack = profileName.replace("DLCPack", "").replace(".layer.0.all.archive", "").toUpperCase(Locale.ROOT);
        return pack + "-" + f.offset + "-" + String.format(Locale.ROOT, "%02d", f.original.length);
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String uns(String s) {
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                if (c == 't') out.append('\t');
                else if (c == 'r') out.append('\r');
                else if (c == 'n') out.append('\n');
                else out.append(c);
                esc = false;
            } else if (c == '\\') esc = true;
            else out.append(c);
        }
        if (esc) out.append('\\');
        return out.toString();
    }

    public static void exportBase(Component parent, BaseResearchProfiles.Profile p) {
        if (p == null) return;
        File f = chooseSave(parent, "base-field-identifications.tsv");
        if (f == null) return;
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            writeHeader(w);
            for (BaseResearchProfiles.Field x : p.fields) {
                FieldConfidenceAudit.Assessment a = FieldConfidenceAudit.assessBase(x, null, "");
                w.println(String.join("\t", "BASE", safe(p.name), safe(baseFieldId(x)), Long.toString(x.offset),
                        Integer.toString(x.original.length), HexUtil.compact(x.original), HexUtil.compact(x.current),
                        safe(a.confidence.name()), safe(a.name + " | raw=" + x.label), safe(x.identifiedAs)));
            }
            JOptionPane.showMessageDialog(parent, "Saved IDs:\n" + f.getAbsolutePath(), "IDs saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            error(parent, "Save IDs", ex);
        }
    }

    public static void exportDlc(Component parent, String profileName, DLCProfiles.Profile p) {
        if (p == null) return;
        File f = chooseSave(parent, profileName.replace(".archive", "") + "-field-identifications.tsv");
        if (f == null) return;
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            writeHeader(w);
            for (DLCProfiles.Field x : p.fields) {
                FieldConfidenceAudit.Assessment a = FieldConfidenceAudit.assessDlc(profileName, x, "");
                w.println(String.join("\t", "DLC", safe(profileName), safe(dlcFieldId(profileName, x)), Long.toString(x.offset),
                        Integer.toString(x.original.length), HexUtil.compact(x.original), HexUtil.compact(x.current),
                        safe(a.confidence.name()), safe(a.name + " | raw=" + x.label), safe(x.identifiedAs)));
            }
            JOptionPane.showMessageDialog(parent, "Saved IDs:\n" + f.getAbsolutePath(), "IDs saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            error(parent, "Save IDs", ex);
        }
    }

    private static void writeHeader(PrintWriter w) {
        w.println("# DXMD Archive Editor Pro field-identification export v2");
        w.println("# Edit IdentifiedAs if desired. Offset is included for verification but hidden from the compact research list.");
        w.println("Scope\tArchive\tFieldID\tOffset\tLength\tOriginalHex\tCurrentHex\tConfidence\tBuiltInAssessment\tIdentifiedAs");
    }

    public static int importBase(Component parent, BaseResearchProfiles.Profile p) {
        if (p == null) return 0;
        File f = chooseOpen(parent);
        if (f == null) return 0;
        try {
            Map<String, String> ids = readIds(f);
            int n = 0;
            for (BaseResearchProfiles.Field x : p.fields) {
                String v = ids.get(baseFieldId(x));
                if (v != null) { x.identifiedAs = v; n++; }
            }
            JOptionPane.showMessageDialog(parent, "Loaded " + n + " matching Base ID(s).", "IDs loaded", JOptionPane.INFORMATION_MESSAGE);
            return n;
        } catch (Exception ex) {
            error(parent, "Load IDs", ex);
            return 0;
        }
    }

    public static int importDlc(Component parent, String profileName, DLCProfiles.Profile p) {
        if (p == null) return 0;
        File f = chooseOpen(parent);
        if (f == null) return 0;
        try {
            Map<String, String> ids = readIds(f);
            int n = 0;
            for (DLCProfiles.Field x : p.fields) {
                String v = ids.get(dlcFieldId(profileName, x));
                if (v != null) { x.identifiedAs = v; n++; }
            }
            JOptionPane.showMessageDialog(parent, "Loaded " + n + " matching " + shortProfile(profileName) + " ID(s).", "IDs loaded", JOptionPane.INFORMATION_MESSAGE);
            return n;
        } catch (Exception ex) {
            error(parent, "Load IDs", ex);
            return 0;
        }
    }

    private static Map<String, String> readIds(File f) throws IOException {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            boolean header = false;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] x = line.split("\t", -1);
                if (!header && x.length > 0 && x[0].equals("Scope")) { header = true; continue; }
                if (x.length < 10) continue;
                out.put(uns(x[2]), uns(x[9]));
            }
        }
        return out;
    }

    private static File chooseSave(Component parent, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save field IDs");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("Tab-separated text (*.tsv, *.txt)", "tsv", "txt"));
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        String n = f.getName().toLowerCase(Locale.ROOT);
        if (!n.endsWith(".tsv") && !n.endsWith(".txt")) f = new File(f.getParentFile(), f.getName() + ".tsv");
        return f;
    }

    private static File chooseOpen(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load field IDs");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("Field-ID text (*.tsv, *.txt)", "tsv", "txt"));
        return fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    private static String shortProfile(String profileName) {
        return profileName == null ? "DLC" : profileName.replace("DLCPack", "").replace(".layer.0.all.archive", "");
    }

    private static void error(Component parent, String title, Exception ex) {
        String message = ex.getMessage();
        JOptionPane.showMessageDialog(parent, message == null ? ex.toString() : message, title, JOptionPane.ERROR_MESSAGE);
    }
}
