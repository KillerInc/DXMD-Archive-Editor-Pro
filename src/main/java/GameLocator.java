import java.io.*;
import java.util.*;

public class GameLocator {
    public static class Result {
        public final File exe, gameRoot, baseArchive;
        public final LinkedHashMap<String, File> dlcArchives;
        Result(File exe, File root, File base, LinkedHashMap<String, File> dlc) {
            this.exe=exe; this.gameRoot=root; this.baseArchive=base; this.dlcArchives=dlc;
        }
    }

    /** Locate from an explicitly selected DXMD.exe. */
    public static Result locate(File exe) throws IOException {
        if (exe == null || !exe.isFile() || !exe.getName().equalsIgnoreCase("DXMD.exe"))
            throw new IOException("Please select DXMD.exe.");
        File retail = exe.getParentFile();
        File root = retail;
        if (retail != null && retail.getName().equalsIgnoreCase("retail")) root = retail.getParentFile();
        if (!isValidatedGameRoot(root))
            throw new IOException("The selected DXMD.exe is not inside a recognized Deus Ex: Mankind Divided installation.");
        return buildResult(root);
    }

    /**
     * Resolve the real game install from any file chosen inside it. This deliberately
     * walks parent directories only. It never searches siblings or nearby folders, so
     * backup/copy archives beside the editor cannot be mistaken for the live install.
     */
    public static Result locateFromHint(File hint) throws IOException {
        if (hint == null) throw new IOException("No file was selected.");
        File start = hint.isDirectory() ? hint : hint.getParentFile();
        File root = findValidatedRootUpwards(start, 10);
        if (root == null)
            throw new IOException("Could not identify a DXMD game installation from the selected location.");
        return buildResult(root);
    }

    /**
     * At boot, use the JAR/classes location only as an anchor. We walk upward and
     * require the complete expected game structure. We do not scan the JAR's nearby
     * folders for archive filenames.
     */
    public static Result locateFromApplication() {
        try {
            File code = new File(GameLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File start = code.isDirectory() ? code : code.getParentFile();
            File root = findValidatedRootUpwards(start, 10);
            return root == null ? null : buildResult(root);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Result buildResult(File root) throws IOException {
        if (!isValidatedGameRoot(root)) throw new IOException("Not a recognized DXMD game directory.");
        File exe = new File(new File(root, "retail"), "DXMD.exe");
        File base = new File(new File(root, "runtime"), "Game.layer.1.all.archive");

        LinkedHashMap<String, File> dlc = new LinkedHashMap<>();
        File dlcRuntime = new File(new File(root, "DLC"), "runtime");
        for (String name : DLCProfiles.names()) {
            File f = new File(dlcRuntime, name);
            dlc.put(name, f.isFile() ? f : null);
        }
        return new Result(exe, root, base.isFile() ? base : null, dlc);
    }

    private static boolean isValidatedGameRoot(File root) {
        if (root == null || !root.isDirectory()) return false;
        File exe = new File(new File(root, "retail"), "DXMD.exe");
        File base = new File(new File(root, "runtime"), "Game.layer.1.all.archive");
        return exe.isFile() && base.isFile();
    }

    private static File findValidatedRootUpwards(File start, int maxLevels) {
        File cur = start;
        for (int i=0; cur != null && i<=maxLevels; i++, cur=cur.getParentFile()) {
            if (isValidatedGameRoot(cur)) return cur;
        }
        return null;
    }
}
