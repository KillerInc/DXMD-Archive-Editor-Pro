import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Maps physical archive offsets back to the internal resource/chunk declared by DXMD's ARCH directory. */
final class ArchiveResourceIndex {
    record Location(String resourceName, int chunkIndex, long resourceOffset, long archiveOffset, long chunkLength) {}
    private record Region(long start, long end, String resourceName, int chunkIndex, long resourceBegin) {}
    private record CacheEntry(long size, long modified, ArchiveResourceIndex index) {}

    private static final HashMap<String, CacheEntry> CACHE = new HashMap<>();
    private static final int MAX_STRING = 16 * 1024 * 1024;

    private final ArrayList<Region> regions = new ArrayList<>();
    private final String archiveName;

    private ArchiveResourceIndex(String archiveName) { this.archiveName = archiveName; }

    static synchronized ArchiveResourceIndex load(File archive) throws IOException {
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("Archive not found.");
        String path = archive.getCanonicalPath();
        long size = archive.length(), modified = archive.lastModified();
        CacheEntry hit = CACHE.get(path);
        if (hit != null && hit.size == size && hit.modified == modified) return hit.index;

        ArchiveResourceIndex out = parse(archive);
        CACHE.put(path, new CacheEntry(size, modified, out));
        return out;
    }

    private static ArchiveResourceIndex parse(File archive) throws IOException {
        ArchiveResourceIndex out = new ArchiveResourceIndex(archive.getName());
        try (RandomAccessFile raf = new RandomAccessFile(archive, "r")) {
            if (raf.length() < 24) throw new IOException("ARCH file is too small: " + archive.getName());
            byte[] magic = new byte[4];
            raf.readFully(magic);
            if (!Arrays.equals(magic, new byte[]{'A', 'R', 'C', 'H'}))
                throw new IOException("Not a DXMD ARCH archive: " + archive.getName());

            readIntLE(raf); // unknown/version
            int fileCount = readIntLE(raf);
            int linkedCount = readIntLE(raf);
            long directoryOffset = readLongLE(raf);
            long fileSize = raf.length();
            if (fileCount < 0 || linkedCount < 0 || directoryOffset < 24 || directoryOffset >= fileSize)
                throw new IOException("Invalid ARCH directory metadata.");
            // Every linked entry and file entry consumes bytes, so counts larger than the file itself can only be corrupt.
            if ((long) linkedCount > fileSize / 5L || (long) fileCount > fileSize / 32L)
                throw new IOException("Unreasonable ARCH directory counts.");

            raf.seek(directoryOffset);
            ArrayList<String> links = new ArrayList<>(linkedCount);
            for (int i = 0; i < linkedCount; i++) {
                int n = readIntLE(raf);
                links.add(readString0(raf, n));
            }

            int selfIndex = -1;
            for (int i = 0; i < links.size(); i++) {
                if (links.get(i).equalsIgnoreCase(archive.getName())) { selfIndex = i; break; }
            }

            for (int fi = 0; fi < fileCount; fi++) {
                ensureRemaining(raf, 24);
                raf.seek(raf.getFilePointer() + 24);
                int nameLen = readIntLE(raf);
                String name = readString0(raf, nameLen);
                int chunkCount = readIntLE(raf);
                if (chunkCount < 0 || (long) chunkCount > (fileSize - raf.getFilePointer()) / 28L)
                    throw new IOException("Invalid ARCH chunk count for " + name + ".");

                for (int ci = 0; ci < chunkCount; ci++) {
                    int archiveIndex = readIntLE(raf);
                    long resourceBegin = readLongLE(raf);
                    long archiveOffset = readLongLE(raf);
                    long length = readLongLE(raf);
                    boolean rangeValid = archiveOffset >= 0 && length > 0 && resourceBegin >= 0
                            && archiveOffset <= fileSize && length <= fileSize - archiveOffset;
                    if (archiveIndex == selfIndex && rangeValid)
                        out.regions.add(new Region(archiveOffset, archiveOffset + length, name, ci, resourceBegin));
                }
            }
        }
        out.regions.sort(Comparator.comparingLong(Region::start));
        return out;
    }

    Location locate(long archiveOffset) {
        int lo = 0, hi = regions.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            Region r = regions.get(mid);
            if (archiveOffset < r.start) hi = mid - 1;
            else if (archiveOffset >= r.end) lo = mid + 1;
            else return new Location(r.resourceName, r.chunkIndex,
                    r.resourceBegin + (archiveOffset - r.start), archiveOffset, r.end - r.start);
        }
        return null;
    }

    int regionCount() { return regions.size(); }
    String archiveName() { return archiveName; }

    private static String readString0(RandomAccessFile raf, int n) throws IOException {
        if (n < 0 || n > MAX_STRING) throw new IOException("Invalid ARCH string length: " + n);
        ensureRemaining(raf, (long) n + 1L);
        byte[] b = new byte[n];
        raf.readFully(b);
        int zero = raf.read();
        if (zero != 0) throw new IOException("ARCH string is not NUL-terminated.");
        return new String(b, StandardCharsets.UTF_8);
    }

    private static void ensureRemaining(RandomAccessFile raf, long needed) throws IOException {
        long remaining = raf.length() - raf.getFilePointer();
        if (needed < 0 || needed > remaining) throw new EOFException("Truncated ARCH directory.");
    }

    private static int readIntLE(RandomAccessFile raf) throws IOException {
        int a = raf.read(), b = raf.read(), c = raf.read(), d = raf.read();
        if ((a | b | c | d) < 0) throw new EOFException();
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    private static long readLongLE(RandomAccessFile raf) throws IOException {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            int b = raf.read();
            if (b < 0) throw new EOFException();
            v |= ((long) b) << (8 * i);
        }
        return v;
    }
}
