final class HexUtil {
    private HexUtil() {}

    static String compact(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02X", b & 0xFF));
        return out.toString();
    }

    static String spaced(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder out = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) out.append(' ');
            out.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return out.toString();
    }
}
