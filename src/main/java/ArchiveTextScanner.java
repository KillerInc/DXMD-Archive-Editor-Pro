import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Streams printable ASCII runs without loading an entire DXMD archive into heap. */
final class ArchiveTextScanner {
    record Token(long start, long end, String text) {}

    private ArchiveTextScanner() {}

    static List<Token> scan(File file) throws IOException {
        if (file == null || !file.isFile()) throw new FileNotFoundException("Archive not found.");
        ArrayList<Token> out = new ArrayList<>();
        byte[] buffer = new byte[1024 * 1024];
        byte[] run = new byte[120];
        int runLength = 0;
        boolean overlong = false;
        long runStart = -1;
        long absolute = 0;

        try (InputStream in = new BufferedInputStream(new FileInputStream(file), buffer.length)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (n == 0) continue;
                for (int i = 0; i < n; i++, absolute++) {
                    int value = buffer[i] & 0xFF;
                    if (value >= 32 && value <= 126) {
                        if (runLength == 0 && !overlong) runStart = absolute;
                        if (!overlong && runLength < run.length) run[runLength++] = buffer[i];
                        else overlong = true;
                    } else {
                        finish(out, run, runLength, overlong, runStart, absolute);
                        runLength = 0;
                        overlong = false;
                        runStart = -1;
                    }
                }
            }
        }
        finish(out, run, runLength, overlong, runStart, absolute);
        return out;
    }

    private static void finish(List<Token> out, byte[] run, int length, boolean overlong, long start, long end) {
        if (overlong || length < 4 || start < 0) return;
        String text = new String(run, 0, length, StandardCharsets.US_ASCII).trim();
        if (!text.isEmpty()) out.add(new Token(start, end, text));
    }
}
