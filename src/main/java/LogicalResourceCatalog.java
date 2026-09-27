import java.util.*;

/**
 * Verified DXMD HeaderLib/BIN1 structure for resource libraries currently touched by
 * the research profiles. These mappings were derived from the user-supplied
 * pc_headerlib collection with the DXMD-v002 layout in dxmd_dawn_extract.py.
 *
 * This catalog is deliberately conservative: if a resource library / payload range
 * is not present here, the editor reports that HeaderLib metadata is unavailable
 * instead of guessing.
 */
final class LogicalResourceCatalog {
    record LogicalResource(
            String resourceLibrary,
            long payloadStart,
            long payloadLength,
            String logicalPath,
            String headerLib,
            int libraryIndex,
            long resourceId,
            long ownerId,
            long flags,
            long magic,
            long embeddedSize,
            long chunkSize) {
        long payloadEnd() { return payloadStart + payloadLength; }
        long payloadOffset(long resourceOffset) { return resourceOffset - payloadStart; }
        boolean contains(long resourceOffset) { return resourceOffset >= payloadStart && resourceOffset < payloadEnd(); }
        String typeName() {
            int dot = logicalPath == null ? -1 : logicalPath.lastIndexOf('.');
            return dot >= 0 && dot + 1 < logicalPath.length() ? logicalPath.substring(dot + 1) : "unknown";
        }
        String magicHex() { return String.format("0x%08X", magic & 0xFFFFFFFFL); }
        String resourceIdHex() { return String.format("0x%08X", resourceId & 0xFFFFFFFFL); }
        String ownerIdHex() { return String.format("0x%08X", ownerId & 0xFFFFFFFFL); }
        String flagsHex() { return String.format("0x%X", flags); }
    }

    private static final Map<String,List<LogicalResource>> BY_LIBRARY;
    static {
        LinkedHashMap<String,List<LogicalResource>> m = new LinkedHashMap<>();
        add(m, new LogicalResource(
                "B3E62D6B1E7E6C974E9271254F1EA472.pc_resourcelib",
                148_976_546L,
                5_618_676L,
                "[assembly:/scenes/_globalinclude/globalinclude.include].pc_entitytemplate",
                "F75BA09D656F6CA5CDFADD3C1511D75A.pc_headerlib",
                21,
                0x800033F9L,
                0x34D0A7EEL,
                0x00B42C7FE282EBB2L,
                0x54454D50L,
                136_434L,
                0xFFFFFFFFL));
        add(m, new LogicalResource(
                "9930F10393FF9D6B652AC30C4143B46D.pc_resourcelib",
                29_849_273L,
                457_292L,
                "[assembly:/scenes/dlc_01/live_global.entity].pc_entitytemplate",
                "09382868F0EE1CDA1BA6BEE48D74EE22.pc_headerlib",
                12,
                0x80000389L,
                0x0C2312CBL,
                0x00D79089279C6D3AL,
                0x54454D50L,
                11_386L,
                0xFFFFFFFFL));
        LinkedHashMap<String,List<LogicalResource>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String,List<LogicalResource>> e : m.entrySet())
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        BY_LIBRARY = Collections.unmodifiableMap(frozen);
    }

    private LogicalResourceCatalog() {}

    private static void add(Map<String,List<LogicalResource>> map, LogicalResource r) {
        map.computeIfAbsent(r.resourceLibrary.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(r);
    }

    static LogicalResource locate(String resourceLibrary, long resourceOffset) {
        if (resourceLibrary == null) return null;
        List<LogicalResource> list = BY_LIBRARY.get(resourceLibrary.toLowerCase(Locale.ROOT));
        if (list == null) return null;
        for (LogicalResource r : list) if (r.contains(resourceOffset)) return r;
        return null;
    }

    static int mappingCount() {
        int n = 0;
        for (List<LogicalResource> l : BY_LIBRARY.values()) n += l.size();
        return n;
    }

    static int libraryCount() { return BY_LIBRARY.size(); }

    static void prewarm() {
        // Touch the immutable map during the background loading phase.
        mappingCount();
    }

    static String sourceDescription() {
        return "DXMD v002 HeaderLib/BIN1 metadata derived from the supplied pc_headerlib set";
    }
}
