import java.io.*;
import java.util.*;

public class DLCProfiles {
    public static class Field { long offset; String label; String identifiedAs=""; byte[] original, modded, current; Field(long o,String l,byte[] a,byte[] b){offset=o;label=l;original=a;modded=b;current=a.clone();} }
    public static class Profile { String name; long size; ArrayList<Field> fields=new ArrayList<>(); Profile(String n,long s){name=n;size=s;} }
    private static final LinkedHashMap<String,Profile> profiles=new LinkedHashMap<>();
    static { try { load(); } catch(Exception e) { throw new RuntimeException(e); } }
    static void load() throws Exception {
        try(BufferedReader br=CompressedResource.open(DLCProfiles.class,"/dlc_profiles.tsv.gz.b64")){
            String line; Profile p=null;
            while((line=br.readLine())!=null){
                if(line.isEmpty()||line.startsWith("#"))continue;
                String[] x=line.split("\\t",-1);
                if(x[0].equals("PROFILE")){p=new Profile(x[1],Long.parseLong(x[2]));profiles.put(p.name,p);}
                else if(x[0].equals("FIELD")){p.fields.add(new Field(Long.parseLong(x[1]),x[4],fromHex(x[2]),fromHex(x[3])));}
            }
        }
        validateAndSort();
    }
    private static void validateAndSort() throws IOException {
        for(Profile p:profiles.values()){
            p.fields.sort(Comparator.comparingLong(f->f.offset));
            long previousEnd=-1;
            for(Field f:p.fields){
                if(f.original==null||f.original.length==0)throw new IOException("Zero-length DLC field in "+p.name+" at "+f.offset);
                if(f.modded==null||f.modded.length!=f.original.length)throw new IOException("DLC comparison length mismatch in "+p.name+" at "+f.offset);
                long end=f.offset+f.original.length;
                if(f.offset<0||end>p.size)throw new IOException("DLC field outside archive bounds in "+p.name+" at "+f.offset);
                if(f.offset<previousEnd)throw new IOException("Overlapping/out-of-order DLC fields in "+p.name+" near "+f.offset);
                previousEnd=end;
            }
        }
    }
    static byte[] fromHex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    public static Set<String> names(){return profiles.keySet();}
    public static Profile get(String n){return profiles.get(n);}
    public static String detect(String filename){for(String n:profiles.keySet())if(n.equalsIgnoreCase(filename))return n;return null;}
}
