import java.io.*;
import java.util.*;

public class BaseResearchProfiles {
    public static class Field {
        long offset; String category, label; String identifiedAs=""; byte[] original, current;
        LinkedHashMap<String,byte[]> references=new LinkedHashMap<>();
        Field(long o,String c,String l,byte[] orig){offset=o;category=c;label=l;original=orig;current=orig.clone();}
        byte[] reference(String name){return references.get(name);}
    }
    public static class Profile {
        String name; long size; ArrayList<Field> fields=new ArrayList<>(); ArrayList<String> referenceNames=new ArrayList<>();
        Profile(String n,long s){name=n;size=s;}
    }
    private static Profile profile;
    static { try { load(); } catch(Exception e) { throw new RuntimeException(e); } }

    private static void load() throws Exception {
        try(BufferedReader br=CompressedResource.openParts(BaseResearchProfiles.class,
                "/base_research.part01.b64",
                "/base_research.part02.b64",
                "/base_research.tail01.b64",
                "/base_research.tail02.b64",
                "/base_research.tail03.b64",
                "/base_research.tail04.b64",
                "/base_research.tail05.b64",
                "/base_research.tail06.b64",
                "/base_research.tail07.b64",
                "/base_research.tail08.b64",
                "/base_research.tail09.b64")) {
            String line;
            while((line=br.readLine())!=null) parseBaseLine(line);
        }
        // v0.6.9 decoded XP/reward records replace the older broad diff fragments.
        applyPatch("/base_research_v069_patch.tsv.gz.b64");
        // v0.6.15 overlays focused mappings and comparison references derived from
        // the complete supplied OG + modded raw archive set.
        RawArchiveAuditCatalog.applyBase(profile);
        validateAndSort();
    }

    private static void parseBaseLine(String line) {
        if(line.isEmpty()||line.startsWith("#")) return;
        String[] x=line.split("\\t",-1);
        if(x[0].equals("PROFILE")) profile=new Profile(x[1],Long.parseLong(x[2]));
        else if(x[0].equals("REFERENCES")){ for(int i=1;i<x.length;i++) profile.referenceNames.add(x[i]); }
        else if(x[0].equals("FIELD")) profile.fields.add(parseField(x));
    }

    private static Field parseField(String[] x) {
        if(profile==null) throw new IllegalStateException("FIELD encountered before PROFILE");
        int expected=5+profile.referenceNames.size();
        if(x.length<expected) throw new IllegalArgumentException("Malformed Base Fields row: expected at least "+expected+" columns, got "+x.length);
        long off=Long.parseLong(x[1]); String cat=x[2], label=x[3]; byte[] orig=fromHex(x[4]);
        Field f=new Field(off,cat,label,orig);
        for(int i=0;i<profile.referenceNames.size();i++) f.references.put(profile.referenceNames.get(i),fromHex(x[5+i]));
        return f;
    }

    private static void applyPatch(String resource) throws Exception {
        try(BufferedReader br=CompressedResource.open(BaseResearchProfiles.class,resource)) {
            String line;
            while((line=br.readLine())!=null) {
                if(line.isEmpty()||line.startsWith("#")) continue;
                String[] x=line.split("\\t",-1);
                if(x[0].equals("REMOVED")) {
                    if(x.length < 2 || x[1].isEmpty()) continue;
                    HashSet<String> remove=new HashSet<>(Arrays.asList(x[1].split(",")));
                    profile.fields.removeIf(f -> remove.contains(f.offset+":"+f.original.length));
                } else if(x[0].equals("FIELD")) {
                    profile.fields.add(parseField(x));
                }
            }
        }
    }

    private static void validateAndSort() throws IOException {
        if(profile==null) throw new IOException("Base Fields profile was not loaded.");
        profile.fields.sort(Comparator.comparingLong(f -> f.offset));
        long previousEnd=-1;
        for(Field f:profile.fields) {
            if(f.original==null || f.original.length==0) throw new IOException("Zero-length Base Fields record at "+f.offset);
            long end=f.offset+f.original.length;
            if(f.offset<0 || end>profile.size) throw new IOException("Base Fields record outside archive bounds at "+f.offset);
            if(f.offset<previousEnd) throw new IOException("Overlapping/out-of-order Base Fields records near "+f.offset);
            previousEnd=end;
            for(String ref:profile.referenceNames) {
                byte[] value=f.reference(ref);
                if(value==null || value.length!=f.original.length)
                    throw new IOException("Reference length mismatch for "+ref+" at "+f.offset);
            }
        }
    }

    static byte[] fromHex(String s){
        if(s==null || (s.length()&1)!=0) throw new IllegalArgumentException("Invalid hex field length");
        byte[] b=new byte[s.length()/2];
        for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);
        return b;
    }
    public static Profile get(){return profile;}
    public static java.util.List<String> referenceNames(){return Collections.unmodifiableList(profile.referenceNames);}
}
