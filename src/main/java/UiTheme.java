import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Enumeration;

/** Dark navy/teal UI used by the v0.7 research interface. */
final class UiTheme {
    static final Color APP_BG = new Color(9, 16, 27);
    static final Color PANEL = new Color(13, 25, 39);
    static final Color PANEL_ALT = new Color(16, 31, 48);
    static final Color FIELD = new Color(8, 20, 33);
    static final Color TABLE = new Color(10, 23, 37);
    static final Color HEADER = new Color(18, 39, 58);
    static final Color BORDER = new Color(39, 66, 88);
    static final Color TEXT = new Color(223, 233, 241);
    static final Color MUTED = new Color(143, 164, 181);
    static final Color ACCENT = new Color(25, 213, 193);
    static final Color ACCENT_2 = new Color(35, 176, 199);
    static final Color SELECT = new Color(13, 112, 126);
    static final Color BUTTON = new Color(18, 54, 72);
    static final Color DANGER = new Color(222, 120, 79);
    static final Color CONFIRMED = new Color(71, 210, 151);
    static final Color STRONG = new Color(96, 151, 236);
    static final Color SUSPECTED = new Color(224, 181, 89);

    private UiTheme() {}

    static void install() {
        try { UIManager.setLookAndFeel(new NimbusLookAndFeel()); }
        catch (Exception ignored) {}

        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        FontUIResource font = new FontUIResource(base);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) UIManager.put(key, font);
        }

        UIManager.put("control", new ColorUIResource(PANEL));
        UIManager.put("info", new ColorUIResource(PANEL_ALT));
        UIManager.put("nimbusBase", new ColorUIResource(new Color(20, 81, 99)));
        UIManager.put("nimbusBlueGrey", new ColorUIResource(new Color(24, 46, 63)));
        UIManager.put("nimbusLightBackground", new ColorUIResource(FIELD));
        UIManager.put("nimbusSelectionBackground", new ColorUIResource(SELECT));
        UIManager.put("text", new ColorUIResource(TEXT));
        UIManager.put("textText", new ColorUIResource(TEXT));
        UIManager.put("textHighlight", new ColorUIResource(SELECT));
        UIManager.put("textHighlightText", new ColorUIResource(Color.WHITE));
        UIManager.put("controlText", new ColorUIResource(TEXT));
        UIManager.put("menuText", new ColorUIResource(TEXT));
        UIManager.put("Table.background", new ColorUIResource(TABLE));
        UIManager.put("Table.foreground", new ColorUIResource(TEXT));
        UIManager.put("Table.selectionBackground", new ColorUIResource(SELECT));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.gridColor", new ColorUIResource(BORDER));
        UIManager.put("TableHeader.background", new ColorUIResource(HEADER));
        UIManager.put("TableHeader.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextField.background", new ColorUIResource(FIELD));
        UIManager.put("TextField.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextArea.background", new ColorUIResource(FIELD));
        UIManager.put("TextArea.foreground", new ColorUIResource(TEXT));
        UIManager.put("ComboBox.background", new ColorUIResource(FIELD));
        UIManager.put("ComboBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("Button.background", new ColorUIResource(BUTTON));
        UIManager.put("Button.foreground", new ColorUIResource(TEXT));
        UIManager.put("TabbedPane.background", new ColorUIResource(APP_BG));
        UIManager.put("TabbedPane.foreground", new ColorUIResource(TEXT));
        UIManager.put("TabbedPane.selected", new ColorUIResource(PANEL_ALT));
        UIManager.put("Panel.background", new ColorUIResource(PANEL));
        UIManager.put("Label.foreground", new ColorUIResource(TEXT));
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(MUTED));
        UIManager.put("OptionPane.background", new ColorUIResource(PANEL));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("ToolTip.background", new ColorUIResource(HEADER));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXT));
    }

    static void apply(Component root) {
        if (root == null) return;
        style(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) apply(child);
        }
    }

    private static void style(Component c) {
        c.setFont(new Font("Segoe UI", c.getFont() == null ? Font.PLAIN : c.getFont().getStyle(),
                Math.max(12, c.getFont() == null ? 13 : c.getFont().getSize())));

        if (c instanceof JFrame frame) {
            frame.getContentPane().setBackground(APP_BG);
        } else if (c instanceof JPanel panel) {
            panel.setBackground(PANEL);
            recolorBorder(panel);
        } else if (c instanceof JTable table) {
            table.setBackground(TABLE);
            table.setForeground(TEXT);
            table.setSelectionBackground(SELECT);
            table.setSelectionForeground(Color.WHITE);
            table.setGridColor(BORDER);
            table.setShowHorizontalLines(true);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(0, 1));
            table.setRowHeight(Math.max(24, table.getRowHeight()));
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(HEADER);
                header.setForeground(TEXT);
                header.setOpaque(true);
                header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
            }
        } else if (c instanceof JTextArea area) {
            area.setBackground(FIELD);
            area.setForeground(TEXT);
            area.setCaretColor(ACCENT);
            area.setSelectionColor(SELECT);
            area.setSelectedTextColor(Color.WHITE);
            area.setBorder(new EmptyBorder(6, 8, 6, 8));
        } else if (c instanceof JTextField field) {
            field.setBackground(FIELD);
            field.setForeground(TEXT);
            field.setCaretColor(ACCENT);
            field.setSelectionColor(SELECT);
            field.setSelectedTextColor(Color.WHITE);
            field.setBorder(new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(4, 7, 4, 7)));
        } else if (c instanceof JComboBox<?> combo) {
            combo.setBackground(FIELD);
            combo.setForeground(TEXT);
            combo.setBorder(new LineBorder(BORDER));
        } else if (c instanceof JButton button) {
            button.setBackground(BUTTON);
            button.setForeground(TEXT);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorder(new CompoundBorder(new LineBorder(new Color(36, 104, 125)), new EmptyBorder(5, 10, 5, 10)));
        } else if (c instanceof JSpinner spinner) {
            spinner.setBackground(FIELD);
            spinner.setForeground(TEXT);
            spinner.setBorder(new LineBorder(BORDER));
        } else if (c instanceof JLabel label) {
            label.setForeground(TEXT);
        } else if (c instanceof JTabbedPane tabs) {
            tabs.setBackground(APP_BG);
            tabs.setForeground(TEXT);
            tabs.setBorder(new LineBorder(BORDER));
        } else if (c instanceof JScrollPane scroll) {
            scroll.setBackground(PANEL);
            scroll.setBorder(new LineBorder(BORDER));
            if (scroll.getViewport() != null) scroll.getViewport().setBackground(TABLE);
        } else if (c instanceof JSplitPane split) {
            split.setBackground(APP_BG);
            split.setDividerSize(5);
            split.setBorder(new EmptyBorder(0,0,0,0));
        } else if (c instanceof JSeparator separator) {
            separator.setForeground(BORDER);
            separator.setBackground(BORDER);
        }
        recolorBorder(c);
    }

    private static void recolorBorder(JComponent c) {
        Border b = c.getBorder();
        if (b instanceof TitledBorder titled) {
            titled.setTitleColor(MUTED);
            titled.setBorder(new LineBorder(BORDER));
        }
    }
}
