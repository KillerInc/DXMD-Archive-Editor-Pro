import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class BackupManager {
    private BackupManager() {}

    public static File backupFile(File archive) {
        return new File(archive.getAbsolutePath() + ".bak");
    }

    public static File ensureBackup(File archive) throws IOException {
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("Archive not found.");
        File backup = backupFile(archive);
        if (!backup.exists()) {
            Files.copy(archive.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        }
        return backup;
    }

    public static boolean hasBackup(File archive) {
        return archive != null && backupFile(archive).isFile();
    }

    public static void restoreBackup(File archive) throws IOException {
        File backup = backupFile(archive);
        if (!backup.isFile()) throw new FileNotFoundException("No .bak exists for " + archive.getName());
        Files.copy(backup.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static String sha256(File file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buf = new byte[1024 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            StringBuilder s = new StringBuilder();
            for (byte b : md.digest()) s.append(String.format("%02x", b & 255));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static final Map<String,String> KNOWN = new HashMap<>();
    static {
        KNOWN.put("a9792586ae408e48fa24f0b4fbdb7fc6b5cb09b9a5b9271dc80534c84a13fae4", "Known Original");
        KNOWN.put("45908aaa0437bbc9b6e3957ce0788fe069b1abebd482f20d89fced915f553eb3", "Hardcore Normal");
        KNOWN.put("1c8abf0a2e9b000b9e3cd686d6e9cef71ccb1a82fe5fbab0ca994f2a05e27e1b", "Hardcore Optional");
        KNOWN.put("b390677a090cc72e3f509d7c5e584b2e74fd0644f10b575c2bb65383cbee65fc", "No Health Regen");
        KNOWN.put("86f17af991f38b029c38bf3a372076e4a3ea707bed519f4ac4f8d1e78ce7d9e9", "No Health Regen - Variety");
        KNOWN.put("75fd4e75a44bd83139bb8d89de4d1088272ff8b143e119186538bd1217e1d5e3", "No Health Regen - Variety B");
        KNOWN.put("d657bc6ebb5de5eb9e65e76563d3c946e274f2d34629bb726c8e905db9fc34e4", "Energy Regen - Half");
        KNOWN.put("76a6e75b235e57d60c72821436c8701117ff8e14961602cb16ae37eb376e9d2c", "Energy Regen - Full");
        KNOWN.put("38a21179a06d17b3afb9a8a093da9e95c41bedcc68b896e58b83e17e57a93c80", "Inventory Stacking");
        KNOWN.put("f96285685d6ce162184883a04fdd3a2d38d0656518ce7becf6d283d49be21919", "Known Original DLC");
        KNOWN.put("2fc855cd3df165c005491445e0b3b060afbc82ab7240278cd154bc6dbc8f25b6", "Known Original DLC");
        KNOWN.put("deaa41d6cc7fefeefac6171619579f32de9b1b86fd879b04492c55ebd2bb8fe0", "Known Original DLC");
        KNOWN.put("b98e2b80b43572c3416c127498501c627bcad3ab1065e827bdb65c31b3d5da6d", "Known Original DLC");
        KNOWN.put("0f2320ff1af751fa21016ed52ebcd3fe4a59fb86e182a38f6dfdda977d145aef", "Known Original DLC");
    }

    public static String identify(File file) {
        try {
            String label = KNOWN.get(sha256(file));
            return label == null ? "Modified / unknown hash (editing allowed)" : label;
        } catch (IOException e) {
            return "Hash unavailable (editing allowed)";
        }
    }
}
