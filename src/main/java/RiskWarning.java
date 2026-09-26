import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Session-only warning gate for edits known to carry save/game risk. */
public final class RiskWarning {
    private static boolean suppressForSession = false;

    private RiskWarning() {}

    public static boolean confirm(Component parent, String title, String reason, List<String> fields) {
        if (suppressForSession) return true;

        StringBuilder html = new StringBuilder("<html><body style='width:560px'>");
        html.append("<b>Warning: this change can break an existing save or cause inventory crashes.</b><br><br>");
        html.append(reason == null ? "" : reason).append("<br><br>");
        if (fields != null && !fields.isEmpty()) {
            html.append("<b>Risky field(s) being changed:</b><br>");
            int shown = Math.min(fields.size(), 12);
            for (int i = 0; i < shown; i++) html.append("&#8226; ").append(escape(fields.get(i))).append("<br>");
            if (fields.size() > shown) html.append("&#8226; ...and ").append(fields.size() - shown).append(" more<br>");
            html.append("<br>");
        }
        html.append("<b>Recommended:</b> use these edits on a new game. If testing an existing save, remove/drop affected items from inventory first, save, then make the edit. Keep the editor-created .bak backup.<br><br>");
        html.append("Continue anyway?");
        html.append("</body></html>");

        JLabel message = new JLabel(html.toString());
        JCheckBox dontShow = new JCheckBox("Don't show this warning again this session");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(message);
        panel.add(Box.createVerticalStrut(8));
        panel.add(dontShow);

        Object[] options = {"Continue", "Cancel"};
        int result = JOptionPane.showOptionDialog(parent, panel, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (result == 0) {
            if (dontShow.isSelected()) suppressForSession = true;
            return true;
        }
        return false;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
