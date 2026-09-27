import java.util.*;

/**
 * Verified DXMD HeaderLib/BIN1 structure for resource libraries currently touched by
 * the research profiles. These mappings were derived from the user-supplied Base and
 * DLC pc_headerlib collections with the DXMD-v002 layout in dxmd_dawn_extract.py.
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

        // Base game
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

        // Assault DLC
        add(m, new LogicalResource(
                "3F7F13408DC798B22EAE4EFAA3DBA7FA.pc_resourcelib",
                43_584L,
                22_141L,
                "[assembly:/scenes/dlc_01/50_items1/dlc_pack_assault.entity].pc_entitytemplate",
                "691C2E0CBB6C8508966ECD3629EB9166.pc_headerlib",
                10,
                0x80000014L,
                0xEED198E2L,
                0x00D57918A3904031L,
                0x54454D50L,
                1_390L,
                0xFFFFFFFFL));

        // Classic DLC
        add(m, new LogicalResource(
                "BBC4D5C3C785288B60CF2CE47A3A8973.pc_resourcelib",
                27_247L,
                21_442L,
                "[assembly:/scenes/dlc_01/50_items1/dlc_pack_classic.entity].pc_entitytemplate",
                "3584430BB09444177D638A1FA84EA5E8.pc_headerlib",
                0,
                0x80000011L,
                0x2FB2AFA9L,
                0x00CAF9474586EA0CL,
                0x54454D50L,
                1_348L,
                0xFFFFFFFFL));

        // Enforcer DLC
        add(m, new LogicalResource(
                "266C28A09FE8B55019B5D5878A9A2230.pc_resourcelib",
                31_572_800L,
                26_813L,
                "[assembly:/scenes/dlc_01/50_items1/dlc_pack_enforcer.entity].pc_entitytemplate",
                "69A090C7EBC0CB82E8FEDFBB62407C32.pc_headerlib",
                0,
                0x80000019L,
                0xA3103EB0L,
                0x00F403DCCE76EBBAL,
                0x54454D50L,
                1_712L,
                0xFFFFFFFFL));

        // Intruder DLC
        add(m, new LogicalResource(
                "77A929A4687159D92C5EB5386ED29C88.pc_resourcelib",
                14_826_314L,
                24_322L,
                "[assembly:/scenes/dlc_01/50_items1/dlc_pack_intruder.entity].pc_entitytemplate",
                "86AD8DC5C260D3FF07BC6394EDD05F7E.pc_headerlib",
                0,
                0x8000001FL,
                0x826DE86FL,
                0x00252B139EC73A77L,
                0x54454D50L,
                1_488L,
                0xFFFFFFFFL));

        // Tactical DLC: MicroAssembler entity type
        add(m, new LogicalResource(
                "AADBB1331996C05945184BC2C0C12695.pc_resourcelib",
                1_119L,
                2_420L,
                "[modules:/game.main.moduleinfo?zmicroassembleraugmentation.class].pc_entitytype",
                "9D6F736E503915ED4E6E573E3F4A2C1E.pc_headerlib",
                10,
                0x80000005L,
                0x44EFBDFDL,
                0x00199414D40D55C9L,
                0x43505054L,
                18L,
                0xFFFFFFFFL));

        // Tactical DLC: pack entity
        add(m, new LogicalResource(
                "AADBB1331996C05945184BC2C0C12695.pc_resourcelib",
                81_255L,
                25_322L,
                "[assembly:/scenes/dlc_01/50_items1/dlc_pack_tactical.entity].pc_entitytemplate",
                "9D6F736E503915ED4E6E573E3F4A2C1E.pc_headerlib",
                10,
                0x8000001DL,
                0x44EFBDFDL,
                0x0042DA3AB2F3C1F8L,
                0x54454D50L,
                1_278L,
                0xFFFFFFFFL));

        // Tactical DLC: preorder tranquilizer rifle NPC template
        add(m, new LogicalResource(
                "49519347F788290D4AA2F5D1FD4561D2.pc_resourcelib",
                21_309L,
                11_705L,
                "[assembly:/objects/pickables/weapons/ballistic/tranquilizerrifle_preorder/templates/tranquilizerrifle_preorder_templates.template?/core_equip_npc b116e37d-c912-4403-9a82-29d854bce05e.entitytemplate].pc_entitytemplate",
                "9D6F736E503915ED4E6E573E3F4A2C1E.pc_headerlib",
                12,
                0x80000004L,
                0xE81BB3CCL,
                0x00ED5BD1E2804388L,
                0x54454D50L,
                732L,
                0xFFFFFFFFL));

        // Tactical DLC: preorder tranquilizer rifle player template
        add(m, new LogicalResource(
                "49519347F788290D4AA2F5D1FD4561D2.pc_resourcelib",
                57_141L,
                25_428L,
                "[assembly:/objects/pickables/weapons/ballistic/tranquilizerrifle_preorder/templates/tranquilizerrifle_preorder_templates.template?/core_equip_player a1bcd139-b67b-40bb-ac60-49a97ca2bd6b.entitytemplate].pc_entitytemplate",
                "9D6F736E503915ED4E6E573E3F4A2C1E.pc_headerlib",
                12,
                0x80000006L,
                0xE81BB3CCL,
                0x008856FAB0F26795L,
                0x54454D50L,
                1_250L,
                0xFFFFFFFFL));

        LinkedHashMap<String,List<LogicalResource>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String,List<LogicalResource>> e : m.entrySet()) {
            ArrayList<LogicalResource> ranges = new ArrayList<>(e.getValue());
            ranges.sort(Comparator.comparingLong(LogicalResource::payloadStart));
            frozen.put(e.getKey(), List.copyOf(ranges));
        }
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
        return "DXMD v002 HeaderLib/BIN1 metadata derived from the supplied Base + DLC pc_headerlib sets";
    }
}
