import java.io.*;

public final class ArchiveRestore {
    private ArchiveRestore() {}

    public static void restoreBaseEditorFields(File archive) throws IOException {
        BaseResearchProfiles.Profile p = BaseResearchProfiles.get();
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("Base archive not found.");
        if (!archive.getName().equalsIgnoreCase(p.name)) throw new IOException("Expected " + p.name);
        if (archive.length() != p.size) throw new IOException("Unexpected base archive size; refusing restore.");
        BackupManager.ensureBackup(archive);
        try (RandomAccessFile raf = new RandomAccessFile(archive, "rw")) {
            for (BaseResearchProfiles.Field f : p.fields) {
                raf.seek(f.offset);
                raf.write(f.original);
            }
        }
    }

    public static void restoreDlcEditorFields(File archive, DLCProfiles.Profile p) throws IOException {
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("DLC archive not found.");
        if (p == null) throw new IOException("No DLC profile selected.");
        if (!archive.getName().equalsIgnoreCase(p.name)) throw new IOException("Expected " + p.name);
        if (archive.length() != p.size) throw new IOException("Unexpected DLC archive size; refusing restore.");
        BackupManager.ensureBackup(archive);
        try (RandomAccessFile raf = new RandomAccessFile(archive, "rw")) {
            for (DLCProfiles.Field f : p.fields) {
                raf.seek(f.offset);
                raf.write(f.original);
            }
        }
    }
}
