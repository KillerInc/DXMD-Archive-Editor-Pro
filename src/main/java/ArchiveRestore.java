import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class ArchiveRestore {
    private ArchiveRestore() {}

    /** Restore only confirmed/known base mappings; leave suspected/unidentified research bytes alone. */
    public static void restoreBaseEditorFields(File archive) throws IOException {
        BaseResearchProfiles.Profile profile = BaseResearchProfiles.get();
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("Base archive not found.");
        if (!archive.getName().equalsIgnoreCase(profile.name)) throw new IOException("Expected " + profile.name);
        if (archive.length() != profile.size) throw new IOException("Unexpected base archive size; refusing restore.");
        BackupManager.ensureBackup(archive);
        try (RandomAccessFile file = new RandomAccessFile(archive, "rw")) {
            for (BaseResearchProfiles.Field field : profile.fields) {
                if (field.category == null || !field.category.startsWith("KNOWN")) continue;
                file.seek(field.offset);
                file.write(field.original);
            }
        }
    }

    public static void restoreDlcEditorFields(File archive, DLCProfiles.Profile profile) throws IOException {
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("DLC archive not found.");
        if (profile == null) throw new IOException("No DLC profile selected.");
        if (!archive.getName().equalsIgnoreCase(profile.name)) throw new IOException("Expected " + profile.name);
        if (archive.length() != profile.size) throw new IOException("Unexpected DLC archive size; refusing restore.");
        BackupManager.ensureBackup(archive);
        try (RandomAccessFile file = new RandomAccessFile(archive, "rw")) {
            for (DLCProfiles.Field field : profile.fields) {
                file.seek(field.offset);
                file.write(field.original);
            }
        }
    }
}
