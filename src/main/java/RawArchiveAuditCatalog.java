import java.io.*;
import java.util.*;

/**
 * Derived byte-level evidence from the authoritative raw DXMD archive audit set.
 *
 * This resource contains offsets/hashes/deltas only. It does not redistribute game
 * archives or mod archives. Exact duplicate archives are collapsed by SHA-256.
 */
final class RawArchiveAuditCatalog {
    enum Level { CONFIRMED, STRONG_SUSPECTED, SUSPECTED }

    static final class Mapping {
        final String archive;
        final long offset;
        final byte[] original;
        final Level level;
        final String name;
        final String evidence;
        final boolean saveRisk;

        Mapping(String archive,long offset,String originalHex,Level level,String name,String evidence,boolean saveRisk) {
            this.archive=archive; this.offset=offset; this.original=hex(originalHex);
            this.level=level; this.name=name; this.evidence=evidence; this.saveRisk=saveRisk;
        }
        int length(){ return original.length; }
    }

    private static final class Run {
        final long offset;
        final byte[] clean, mod;
        Run(long offset,String cleanHex,String modHex){this.offset=offset;clean=hex(cleanHex);mod=hex(modHex);}
        long end(){return offset+clean.length;}
    }

    private static final class Variant {
        final String archive,name,sha;
        final int runCount,changedBytes;
        final ArrayList<Run> runs=new ArrayList<>();
        Variant(String archive,String name,String sha,int runCount,int changedBytes){
            this.archive=archive;this.name=name;this.sha=sha;this.runCount=runCount;this.changedBytes=changedBytes;
        }
    }

    private static final LinkedHashMap<String,LinkedHashMap<String,Variant>> variants=new LinkedHashMap<>();
    private static final HashMap<String,String> cleanHashes=new HashMap<>();
    private static final HashMap<String,Long> cleanSizes=new HashMap<>();
    private static final HashMap<String,Mapping> mappings=new HashMap<>();

    static { installStaticBaseline(); installMappings(); }

    private RawArchiveAuditCatalog(){}

    private static void installStaticBaseline() {
        cleanSizes.put("DLCPackAssault.layer.0.all.archive",942225L);
        cleanHashes.put("DLCPackAssault.layer.0.all.archive","f96285685d6ce162184883a04fdd3a2d38d0656518ce7becf6d283d49be21919");
        cleanSizes.put("DLCPackClassic.layer.0.all.archive",169288884L);
        cleanHashes.put("DLCPackClassic.layer.0.all.archive","2fc855cd3df165c005491445e0b3b060afbc82ab7240278cd154bc6dbc8f25b6");
        cleanSizes.put("DLCPackEnforcer.layer.0.all.archive",43902707L);
        cleanHashes.put("DLCPackEnforcer.layer.0.all.archive","deaa41d6cc7fefeefac6171619579f32de9b1b86fd879b04492c55ebd2bb8fe0");
        cleanSizes.put("DLCPackIntruder.layer.0.all.archive",63927966L);
        cleanHashes.put("DLCPackIntruder.layer.0.all.archive","b98e2b80b43572c3416c127498501c627bcad3ab1065e827bdb65c31b3d5da6d");
        cleanSizes.put("DLCPackTactical.layer.0.all.archive",1144680L);
        cleanHashes.put("DLCPackTactical.layer.0.all.archive","0f2320ff1af751fa21016ed52ebcd3fe4a59fb86e182a38f6dfdda977d145aef");
        cleanSizes.put("Game.layer.1.all.archive",16986849L);
        cleanHashes.put("Game.layer.1.all.archive","a9792586ae408e48fa24f0b4fbdb7fc6b5cb09b9a5b9271dc80534c84a13fae4");
    }

    private static void installMappings(){
        final String BASE="Game.layer.1.all.archive";
        add(BASE,7413173L,"22C6ABC4",Level.CONFIRMED,"Takedown Power Consumption Control",
                "Icarus Reflexes changes exactly this 4-byte control to 00000000 and documents removal of takedown power consumption. The raw value behaves like an internal control/identifier, not a numeric energy-cost value.",false);
        add(BASE,6577693L,"00000C42",Level.CONFIRMED,"Energy Auto-Regeneration Limit",
                "Two isolated regeneration mods change this float from 35.0 to 100.0 / 193.0. IPOAO Full differs from normal IPOAO only at this same field.",false);
        add(BASE,4570013L,"0000AA42",Level.CONFIRMED,"Biocell Energy Gain",
                "Variety differs from NoHealthRegen only by this Biocell field and takedown cost; Variety B lowers it again from 42.5 to 28.0.",false);
        add(BASE,7413189L,"00000442",Level.CONFIRMED,"Takedown Energy Cost",
                "Variety differs from NoHealthRegen at this float, changing the documented takedown energy cost from 33.0 to 80.0.",false);
        add(BASE,4937661L,"06",Level.CONFIRMED,"Tranquilizer Rifle Magazine Capacity",
                "I Need The Edge v1.1 isolates the documented Tranquilizer Rifle magazine change 6 -> 12 at this byte.",false);
        add(BASE,4981373L,"03",Level.CONFIRMED,"Lancer Rifle Magazine Capacity",
                "I Need The Edge v1.1 isolates the documented Lancer Rifle magazine change 3 -> 9 at this byte.",false);
        long[] gl={4281877L,4282621L,4283365L,4284109L};
        for(int i=0;i<gl.length;i++) add(BASE,gl[i],"02",Level.CONFIRMED,"Grenade Launcher Ammo Height ["+(i+1)+"/4]",
                "I Need The Edge v1.1 changes all four documented Grenade Launcher ammunition inventory heights from 2 to 1.",true);

        long[] aug={6611045L,7589029L,7589797L,7704685L,7718621L,7719573L,7721117L,7722581L,7723565L,7727101L};
        for(int i=0;i<aug.length;i++) add(BASE,aug[i],"0ACC4075",Level.CONFIRMED,"Experimental Augmentation Gate ["+(i+1)+"/10]",
                "Adam 2.0 changes exactly ten 0ACC4075 markers to zero and documents converting experimental augmentations to normal augmentations.",false);

        silencerFunction(BASE,4887605L,"Combat Rifle");
        add(BASE,4937973L,"01",Level.CONFIRMED,"Tranquilizer Rifle Bolt-Action Override Toggle",
                "Silence To The Guns changes this isolated toggle 01 -> 00; its guide identifies the Tranquilizer Rifle as the first bolt-action-to-semi-auto target.",false);
        add(BASE,4941421L,"00",Level.CONFIRMED,"Machine Pistol Built-in Silencer Enable Toggle",
                "Silence To The Guns changes this isolated weapon toggle 00 -> 01 while enabling the built-in silencer effect.",false);
        silencerFunction(BASE,4952957L,"Battle Rifle");
        add(BASE,4957301L,"01",Level.CONFIRMED,"Stun Pistol Bolt-Action Override Toggle",
                "Silence To The Guns changes this isolated toggle 01 -> 00; its guide identifies the Stun Pistol as the second bolt-action-to-semi-auto target.",false);
        silencerFunction(BASE,4971077L,"Revolver");
        silencerFunction(BASE,4974757L,"Cote d'Azur Combat Rifle");
        silencerFunction(BASE,4981429L,"Lancer Rifle");
        add(BASE,4981445L,"00",Level.CONFIRMED,"Lancer Rifle Built-in Silencer Enable Toggle",
                "Silence To The Guns changes this adjacent Lancer toggle 00 -> 01 while enabling its built-in silencer effect.",false);
        add(BASE,4981685L,"01",Level.CONFIRMED,"Lancer Rifle Bolt-Action Override Toggle",
                "Silence To The Guns changes this isolated toggle 01 -> 00; its guide identifies the Lancer Rifle as the third bolt-action-to-semi-auto target.",false);
        silencerFunction(BASE,4984965L,"OTAR Revolver");
        silencerFunction(BASE,6378557L,"10mm Pistol");
        silencerFunction(BASE,6755701L,"Tactical Shotgun");
        silencerFunction(BASE,6785869L,"Sniper Rifle");
        add(BASE,6785885L,"00",Level.CONFIRMED,"Sniper Rifle Built-in Silencer Enable Toggle",
                "Silence To The Guns changes this adjacent Sniper Rifle toggle 00 -> 01 while enabling its built-in silencer effect.",false);

        long[] healthOff={6576317L,6576565L,6576569L,6576577L,6576901L,6576909L,6576916L,6577141L,6577705L,6578429L,6580645L,6580649L};
        String[] healthOrig={"ACBF6294","01","01","01","01","ACBF6294","00","00","0B","01","01","01"};
        for(int i=0;i<healthOff.length;i++) add(BASE,healthOff[i],healthOrig[i],Level.STRONG_SUSPECTED,
                "Health Regeneration Disable Component ["+(i+1)+"/12]",
                "The focused NoHealthRegen archive changes this component while disabling health regeneration. The mod also has documented augmentation-cost side effects, so the exact internal sub-role is not promoted to confirmed.",false);

        add(BASE,5073853L,"7C114A8C",Level.STRONG_SUSPECTED,"Damage Reduction L1 Default-Enable Component A",
                "I Need The Edge v1.1 adds the documented default-enabled Damage Reduction L1 and changes this nearby 4-byte component; exact internal ID semantics remain unresolved.",false);
        add(BASE,5073869L,"00",Level.STRONG_SUSPECTED,"Damage Reduction L1 Default-Enable Component B",
                "I Need The Edge v1.1 changes this nearby flag 00 -> 01 while adding the documented default-enabled Damage Reduction L1.",false);
        add(BASE,7702165L,"7C114A8C",Level.STRONG_SUSPECTED,"Dermal Armor Default-Enable Control",
                "I Need The Edge v1.1 changes this field near the Dermal Armor record while documenting Dermal Armor as enabled by default in Dubai.",false);

        long[] supA={6765501L,6766405L,6767533L,6768285L,6769821L};
        String[] supAOrig={"000000","000040","000040","000040","000040"};
        long[] supB={6765741L,6766693L,6767821L,6768573L,6770109L};
        String[] supBOrig={"D8FFFFFF","F6FFFFFF","FBFFFFFF","E2FFFFFF","E7FFFFFF"};
        for(int i=0;i<5;i++){
            add(BASE,supA[i],supAOrig[i],Level.STRONG_SUSPECTED,"Suppressor Damage-Debuff Component A ["+(i+1)+"/5]",
                    "I Need The Edge v1.2 differs from v1.1 here while its only documented v1.2 weapon change is removal of suppressor damage debuffs. Exact sub-role remains unresolved.",false);
            add(BASE,supB[i],supBOrig[i],Level.CONFIRMED,"Suppressor Visual Damage Display Modifier ["+(i+1)+"/5]",
                    "Verified by the Grognougnou DXMD modding tutorial: SILENCER paragraphs use function 0519EE4D for the inventory/displayed damage adjustment only, and the documented signed values include the exact D8/FB/E2/E7... FF FF FF values present in these fields. This does not change real weapon power.",false);
        }

        add("DLCPackAssault.layer.0.all.archive",54331L,"05",Level.CONFIRMED,"Elite Battle Rifle Inventory Grid Width",
                "DLC Weapon Inventory and I Need The Edge independently change this byte 5 -> 3; Favored Elites Plus changes the same byte 3 -> 2.",true);
        add("DLCPackEnforcer.layer.0.all.archive",31578472L,"05",Level.CONFIRMED,"Elite Combat Rifle Inventory Grid Width",
                "DLC Weapon Inventory, Master Inventory and I Need The Edge change this byte 5 -> 3; Favored Elites Plus changes it 3 -> 2.",true);
        add("DLCPackTactical.layer.0.all.archive",106868L,"05",Level.CONFIRMED,"Elite Tranquilizer Rifle Inventory Grid Width",
                "DLC Weapon Inventory, Master Inventory and I Need The Edge change this byte 5 -> 3; Favored Elites Plus changes it 3 -> 2.",true);
        add("DLCPackTactical.layer.0.all.archive",107948L,"0A",Level.CONFIRMED,"Elite Tranquilizer Rifle Magazine Capacity",
                "I Need The Edge v1.1/v1.2 documents 10 -> 4 and changes this byte exactly 0A -> 04.",false);

        add("DLCPackAssault.layer.0.all.archive",55507L,"00",Level.CONFIRMED,"Elite Battle Rifle Built-in Silencer Enable Toggle",
                "Silence To The Guns changes only this Assault DLC byte 00 -> 01; its guide explicitly identifies the Elite Battle Rifle silencer toggle.",false);
        dlcSilencerFunction("DLCPackClassic.layer.0.all.archive",33327L,"Elite Revolver");
        dlcSilencerFunction("DLCPackEnforcer.layer.0.all.archive",31579416L,"Elite Combat Rifle");
        dlcSilencerFunction("DLCPackIntruder.layer.0.all.archive",14833042L,"Elite Pistol");

        long[] micro={112348L,113060L,113820L};
        for(int i=0;i<micro.length;i++) add("DLCPackTactical.layer.0.all.archive",micro[i],"0ACC4075",Level.CONFIRMED,
                "Micro-Assembler Experimental/Overclock Gate ["+(i+1)+"/3]",
                "The Micro-Assembler Overheat Fix documents exactly three 0ACC4075 markers changed to zero. Other changes in that archive are treated as inherited contamination.",false);

        add("DLCPackTactical.layer.0.all.archive",107324L,"32",Level.SUSPECTED,
                "Unresolved Tactical Field (contaminated Micro-Assembler archive)",
                "The supplied Micro-Assembler archive changes this byte, but the same file also carries unrelated Elite Tranquilizer inventory/magazine edits. Do not attribute this byte to Micro-Assembler without independent evidence.",false);
        add("DLCPackTactical.layer.0.all.archive",108070L,"20",Level.SUSPECTED,
                "Unresolved Tactical Field (contaminated Micro-Assembler archive)",
                "The supplied Micro-Assembler archive changes this byte, but the same file also carries unrelated Elite Tranquilizer inventory/magazine edits. Do not attribute this byte to Micro-Assembler without independent evidence.",false);

        add("DLCPackEnforcer.layer.0.all.archive",31593120L,"000040",Level.STRONG_SUSPECTED,
                "Elite Combat Rifle Suppressor Damage-Debuff Component A",
                "I Need The Edge v1.2 changes this component only in the documented suppressor-debuff removal update; exact internal sub-role remains unresolved.",false);
        add("DLCPackEnforcer.layer.0.all.archive",31593408L,"E7FFFFFF",Level.CONFIRMED,
                "Elite Combat Rifle Suppressor Visual Damage Display Modifier",
                "Verified by the Grognougnou DXMD modding tutorial: E7 FF FF FF is a documented SILENCER visual damage-display modifier used with function 0519EE4D. It changes the displayed damage value only, not real weapon power.",false);
        add("DLCPackIntruder.layer.0.all.archive",14844586L,"000040",Level.STRONG_SUSPECTED,
                "Elite Pistol Suppressor Damage-Debuff Component A",
                "I Need The Edge v1.2 changes this component only in the documented suppressor-debuff removal update; exact internal sub-role remains unresolved.",false);
        add("DLCPackIntruder.layer.0.all.archive",14844874L,"FBFFFFFF",Level.CONFIRMED,
                "Elite Pistol Suppressor Visual Damage Display Modifier",
                "Verified by the Grognougnou DXMD modding tutorial: FB FF FF FF is a documented SILENCER visual damage-display modifier used with function 0519EE4D. It changes the displayed damage value only, not real weapon power.",false);
    }

    private static void silencerFunction(String archive,long offset,String weapon){
        add(archive,offset,"4DC71C10",Level.CONFIRMED,weapon+" Standard-Reticle Function Slot / Silencer Override",
                "Silence To The Guns documents 4DC71C10 as a sniper/standard-reticle-related function and replaces it with 26ACCD27, the built-in silencer function ID. The field is therefore a function slot used by the mod for a silencer override; 4DC71C10 itself is not a silencer ID.",false);
    }
    private static void dlcSilencerFunction(String archive,long offset,String weapon){ silencerFunction(archive,offset,weapon); }

    private static void add(String archive,long offset,String originalHex,Level level,String name,String evidence,boolean saveRisk){
        Mapping m=new Mapping(archive,offset,originalHex,level,name,evidence,saveRisk);
        mappings.put(key(archive,offset,m.length()),m);
    }

    static Mapping mapping(String archive,long offset,int length){ return mappings.get(key(archive,offset,length)); }

    static Mapping mappingOverlapping(String archive,long offset,int length){
        long end=offset+length;
        for(Mapping m:mappings.values()) if(m.archive.equals(archive) && m.offset<end && m.offset+m.length()>offset) return m;
        return null;
    }

    static java.util.List<Mapping> mappingsFor(String archive){
        ArrayList<Mapping> out=new ArrayList<>();
        for(Mapping m:mappings.values())if(m.archive.equals(archive))out.add(m);
        out.sort(Comparator.comparingLong(m->m.offset));
        return out;
    }

    static String cleanHash(String archive){return cleanHashes.get(archive);}
    static long cleanSize(String archive){Long n=cleanSizes.get(archive);return n==null?-1:n;}

    static java.util.List<String> referenceNames(String archive){ return Collections.emptyList(); }
    static boolean hasReference(String archive,String name){ return false; }
    static byte[] referenceBytes(String archive,String name,long offset,byte[] original){ return original.clone(); }

    static String evidenceSummary(String archive,long offset,int length){
        return "Full raw audit baseline: 48 deduplicated variants (47 modified plus one exact-OG control), 7,222 changed byte-runs across the supplied Base/DLC archive set.";
    }

    static int uniqueVariantCount(){ return 48; }
    static int rawRunCount(){ return 7222; }

    static void applyBase(BaseResearchProfiles.Profile p){
        if(p==null)return;
        applyBaseMappings(p);
        LinkedHashSet<String> names=new LinkedHashSet<>(p.referenceNames);
        names.addAll(referenceNames(p.name));
        p.referenceNames.clear();p.referenceNames.addAll(names);
        for(BaseResearchProfiles.Field f:p.fields){
            for(String name:p.referenceNames){
                if(hasReference(p.name,name)) f.references.put(name,referenceBytes(p.name,name,f.offset,f.original));
                else if(!f.references.containsKey(name)) f.references.put(name,f.original.clone());
            }
        }
    }

    private static void applyBaseMappings(BaseResearchProfiles.Profile p){
        for(Mapping m:mappingsFor(p.name)){
            BaseResearchProfiles.Field exact=null;
            Iterator<BaseResearchProfiles.Field> it=p.fields.iterator();
            while(it.hasNext()){
                BaseResearchProfiles.Field f=it.next();
                if(f.offset==m.offset && f.original.length==m.length()){exact=f;continue;}
                if(f.offset<m.offset+m.length() && f.offset+f.original.length>m.offset)it.remove();
            }
            String category=category(m.level);
            if(exact==null){ exact=new BaseResearchProfiles.Field(m.offset,category,m.name,m.original.clone()); p.fields.add(exact); }
            else { exact.category=category; exact.label=m.name; exact.original=m.original.clone(); exact.current=m.original.clone(); exact.references.clear(); }
        }
    }

    static void applyDlc(DLCProfiles.Profile p){
        if(p==null)return;
        for(Mapping m:mappingsFor(p.name)){
            DLCProfiles.Field exact=null;
            Iterator<DLCProfiles.Field> it=p.fields.iterator();
            while(it.hasNext()){
                DLCProfiles.Field f=it.next();
                if(f.offset==m.offset && f.original.length==m.length()){exact=f;continue;}
                if(f.offset<m.offset+m.length() && f.offset+f.original.length>m.offset)it.remove();
            }
            byte[] hardcore=referenceBytes(p.name,"Hardcore",m.offset,m.original);
            if(exact==null){ exact=new DLCProfiles.Field(m.offset,m.name,m.original.clone(),hardcore); p.fields.add(exact); }
            else { exact.label=m.name; exact.original=m.original.clone(); exact.current=m.original.clone(); exact.modded=hardcore; }
        }
    }

    private static String category(Level l){
        if(l==Level.CONFIRMED)return "KNOWN - Raw archive audit";
        if(l==Level.STRONG_SUSPECTED)return "Strong suspected - Raw archive audit";
        return "Suspected - Raw archive audit";
    }

    private static String key(String archive,long offset,int length){return archive+"|"+offset+"|"+length;}

    private static byte[] hex(String s){
        if(s==null||(s.length()&1)!=0)throw new IllegalArgumentException("Invalid hex: "+s);
        byte[] out=new byte[s.length()/2];
        for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);
        return out;
    }
}
