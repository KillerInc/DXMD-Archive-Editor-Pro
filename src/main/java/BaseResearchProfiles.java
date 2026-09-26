import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public final class BaseResearchProfiles {
    public static final class Field {
        long offset; String category; String label; String identifiedAs = ""; byte[] original; byte[] current;
        final LinkedHashMap<String, byte[]> references = new LinkedHashMap<String, byte[]>();
        Field(long offset, String category, String label, byte[] original) {
            this.offset=offset; this.category=category; this.label=label; this.original=original; this.current=original.clone();
        }
        byte[] reference(String name) { return references.get(name); }
    }
    public static final class Profile {
        String name; long size; final ArrayList<Field> fields=new ArrayList<Field>(); final ArrayList<String> referenceNames=new ArrayList<String>();
        Profile(String name,long size){this.name=name;this.size=size;}
    }
    private static Profile profile;
    static { try { load(); } catch(Exception e) { throw new ExceptionInInitializerError(e); } }
    private BaseResearchProfiles() {}

    private static void load() throws Exception {
        try (BufferedReader reader=CompressedResource.open(BaseResearchProfiles.class,"/base_research.tsv.gz.b64")) {
            String line;
            while((line=reader.readLine())!=null){
                if(line.isEmpty()||line.startsWith("#")) continue;
                String[] parts=line.split("\\t",-1);
                if(parts[0].equals("PROFILE")) profile=new Profile(parts[1],Long.parseLong(parts[2]));
                else if(parts[0].equals("REFERENCES")) { requireProfile(); for(int i=1;i<parts.length;i++) profile.referenceNames.add(parts[i]); }
                else if(parts[0].equals("FIELD")) { requireProfile(); profile.fields.add(parseField(parts)); }
            }
        }
        if(profile==null) throw new IllegalStateException("Base field profile is empty.");
    }
    private static Field parseField(String[] parts){
        int expected=5+profile.referenceNames.size();
        if(parts.length<expected) throw new IllegalArgumentException("Malformed Base Fields row at offset "+(parts.length>1?parts[1]:"unknown"));
        Field field=new Field(Long.parseLong(parts[1]),parts[2],parts[3],fromHex(parts[4]));
        for(int i=0;i<profile.referenceNames.size();i++) field.references.put(profile.referenceNames.get(i),fromHex(parts[5+i]));
        return field;
    }
    private static void requireProfile(){if(profile==null)throw new IllegalStateException("PROFILE record must appear before field data.");}
    static byte[] fromHex(String value){
        if((value.length()&1)!=0)throw new IllegalArgumentException("Odd-length hex value.");
        byte[] bytes=new byte[value.length()/2];
        for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
        return bytes;
    }
    public static Profile get(){return profile;}
    public static java.util.List<String> referenceNames(){return Collections.unmodifiableList(profile.referenceNames);}
}
