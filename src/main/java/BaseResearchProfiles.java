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
        InputStream in=BaseResearchProfiles.class.getResourceAsStream("/base_research.tsv");
        if(in==null) throw new FileNotFoundException("base_research.tsv");
        try(BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"))){
            String line;
            while((line=br.readLine())!=null){
                if(line.isEmpty()||line.startsWith("#")) continue;
                String[] x=line.split("\\t",-1);
                if(x[0].equals("PROFILE")) profile=new Profile(x[1],Long.parseLong(x[2]));
                else if(x[0].equals("REFERENCES")){ for(int i=1;i<x.length;i++) profile.referenceNames.add(x[i]); }
                else if(x[0].equals("FIELD")){
                    long off=Long.parseLong(x[1]); String cat=x[2], label=x[3]; byte[] orig=fromHex(x[4]);
                    Field f=new Field(off,cat,label,orig);
                    for(int i=0;i<profile.referenceNames.size();i++) f.references.put(profile.referenceNames.get(i),fromHex(x[5+i]));
                    profile.fields.add(f);
                }
            }
        }
    }
    static byte[] fromHex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    public static Profile get(){return profile;}
    public static java.util.List<String> referenceNames(){return Collections.unmodifiableList(profile.referenceNames);}
}
