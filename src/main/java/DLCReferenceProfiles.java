import java.io.*;
import java.util.*;

public class DLCReferenceProfiles {
    private static final LinkedHashMap<String,LinkedHashMap<String,HashMap<Long,byte[]>>> data=new LinkedHashMap<>();
    static { try { load(); } catch(Exception e) { throw new RuntimeException(e); } }
    private static void load() throws Exception {
        try(BufferedReader br=CompressedResource.open(DLCReferenceProfiles.class,"/dlc_compare_profiles.tsv.gz.b64")){
            String line, archive=null, profile=null;
            while((line=br.readLine())!=null){
                if(line.isEmpty()||line.startsWith("#"))continue;
                String[] x=line.split("\\t",-1);
                if(x[0].equals("PROFILE")){
                    archive=x[1]; profile=x[2];
                    data.computeIfAbsent(archive,k->new LinkedHashMap<>()).put(profile,new HashMap<Long,byte[]>());
                } else if(x[0].equals("FIELD")&&archive!=null&&profile!=null){
                    data.get(archive).get(profile).put(Long.parseLong(x[1]),fromHex(x[2]));
                }
            }
        }
    }
    public static java.util.List<String> namesFor(String archive){
        LinkedHashSet<String> names=new LinkedHashSet<>();
        names.add("Hardcore");
        LinkedHashMap<String,HashMap<Long,byte[]>> m=data.get(archive); if(m!=null)names.addAll(m.keySet());
        // v0.6.15: expose every deduplicated raw comparison variant supplied for this archive.
        names.addAll(RawArchiveAuditCatalog.referenceNames(archive));
        return new ArrayList<>(names);
    }
    public static byte[] get(String archive,String profile,long offset,DLCProfiles.Field baseField){
        // Preserve the original legacy Hardcore comparison under its historical name.
        if("Hardcore".equals(profile)) return baseField.modded;
        // Raw OG-vs-mod evidence is authoritative when that exact comparison exists.
        if(RawArchiveAuditCatalog.hasReference(archive,profile))
            return RawArchiveAuditCatalog.referenceBytes(archive,profile,offset,baseField.original);
        LinkedHashMap<String,HashMap<Long,byte[]>> a=data.get(archive); if(a==null)return baseField.original;
        HashMap<Long,byte[]> p=a.get(profile); if(p==null)return baseField.original;
        byte[] b=p.get(offset); return b==null?baseField.original:b;
    }
    private static byte[] fromHex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
}
