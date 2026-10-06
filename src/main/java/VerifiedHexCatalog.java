import java.io.*;
import java.util.*;

/**
 * Explicit hash/function markers published by DXMD mod authors.
 *
 * These are verified marker meanings, but proximity to a changed byte is evidence only.
 * Callers must not promote a research row solely because a marker is nearby.
 */
final class VerifiedHexCatalog {
    record Hit(long offset, String hex, String meaning) {}

    private record Marker(byte[] bytes, String hex, String meaning) {}
    private record Layout(String hex, int valueDelta, int valueWidth, String valueMeaning) {}

    private static final Marker[] MARKERS = {
        m("8BB5F8E1","Item inventory width"),
        m("8E6BC587","Weapon / multitool inventory width"),
        m("0E609B24","Inventory height"),
        m("5FA99754","Alternate inventory height"),
        m("8D0137A2","Inventory stack amount"),

        m("2A28DFE5","Consumable shop price"),
        m("B9B2FE47","Parts / disassembly / item price"),
        m("20EDD36A","Multi-Tool shop price"),
        m("C8E9D6EF","5.56mm ammo shop price"),
        m("ED3C00CD","10mm ammo shop price"),
        m("0E90F2F6","Shotgun ammo shop price"),
        m("4A2034A4","40mm ammo shop price"),
        m("6C8E04FF","7.62mm ammo shop price"),
        m("6038AA15","9mm ammo shop price"),
        m("594FCED0","Revolver ammo shop price"),
        m("5697B650","Stun Gun ammo shop price"),
        m("015248E0","Tranquilizer ammo shop price"),
        m("EF482EB8",".416 ammo shop price"),
        m("CD7BD3AF","Shotgun EMP ammo shop price"),
        m("56CFB420","Revolver EMP ammo shop price"),
        m("76771038","9mm EMP ammo shop price"),
        m("0344B883","5.56mm EMP ammo shop price"),
        m("0C092EF3","10mm EMP ammo shop price"),
        m("9A590F0A","Weapon shop price"),
        m("B368F1C3","Grenade Launcher shop price"),
        m("A76435AB","EMP grenade / mine price"),
        m("A463A1F8","Gas grenade / mine price"),
        m("563E4A81","Frag grenade / mine price"),
        m("16C64E9C","Concussion grenade / mine price"),
        m("E856DF13","Smoke grenade / mine price"),

        m("4A376618","Blurred-vision duration"),
        m("F1F9CDA5","Normal health recovery"),
        m("E1B6D572","Special / alcohol health recovery"),
        m("72C142AA","Biocell energy recovery"),
        m("8B658735","Micro-Assembler crafting cost"),
        m("65000977","Crafted item quantity"),

        m("79D898E8","Base weapon magazine capacity"),
        m("3D96AB5F","Cumulative ammo-capacity upgrade bonus"),
        m("77731DD6","Weapon upgrade stage"),
        m("585B7C22","Weapon upgrade parts cost"),
        m("EA10EDF8","Base rate-of-fire shot timing"),
        m("F8861A4C","Base rate-of-fire next-shot delay"),
        m("D42EB87C","Base rate-of-fire component"),
        m("72365784","Rate-of-fire upgrade bonus"),
        m("6712F5D4","Alternate rate-of-fire upgrade bonus"),
        m("A1B89DD4","Weapon range"),
        m("5F317320","Base reload speed"),
        m("FA600225","Reload-speed upgrade bonus"),
        m("2B4352B8","Functional weapon damage bonus / malus"),
        m("0519EE4D","Display-only weapon damage modifier"),

        m("379F683F","Context-sensitive augmentation energy / camera-FOV value"),
        m("22C6ABC4","Takedown energy-use function"),
        m("4C1BC5B1","Automatic energy-regeneration amount"),
        m("292E5FAE","Continuous augmentation energy-drain behavior"),
        m("0ACC4075","Experimental / overclock augmentation marker")
    };

    private static final Layout[] LAYOUTS = {
        layout("8BB5F8E1",16,4,"item inventory width"),
        layout("8E6BC587",16,4,"weapon / multitool inventory width"),
        layout("0E609B24",16,4,"inventory height"),
        layout("8D0137A2",16,4,"stack amount"),
        layout("4A376618",16,4,"blurred-vision duration Float32"),
        layout("72C142AA",16,4,"Biocell energy recovery Float32"),
        layout("8B658735",16,4,"Micro-Assembler crafting cost"),
        layout("79D898E8",16,4,"base magazine capacity"),
        layout("3D96AB5F",16,4,"cumulative ammo-capacity bonus"),
        layout("77731DD6",16,4,"weapon upgrade stage"),
        layout("585B7C22",16,4,"weapon upgrade parts cost"),
        layout("EA10EDF8",16,4,"base rate-of-fire timing Float32"),
        layout("72365784",16,4,"rate-of-fire upgrade bonus Float32"),
        layout("A1B89DD4",16,4,"weapon range Float32"),
        layout("5F317320",16,4,"base reload speed Float32"),
        layout("22C6ABC4",16,4,"takedown energy cost Float32"),
        layout("4C1BC5B1",16,4,"automatic energy-regeneration amount Float32")
    };

    private VerifiedHexCatalog() {}

    static List<Hit> findNear(File file, long center, int radius) {
        if (file==null || !file.isFile()) return List.of();
        long start=Math.max(0,center-radius);
        long end=Math.min(file.length(),center+radius+1L);
        int len=(int)Math.max(0,end-start);
        if(len==0) return List.of();
        byte[] data=new byte[len];
        try(RandomAccessFile raf=new RandomAccessFile(file,"r")){
            raf.seek(start);
            raf.readFully(data);
        } catch(IOException ex){
            return List.of();
        }

        ArrayList<Hit> out=new ArrayList<>();
        for(Marker marker:MARKERS){
            for(int at=indexOf(data,marker.bytes,0);at>=0;at=indexOf(data,marker.bytes,at+1)){
                out.add(new Hit(start+at,marker.hex,marker.meaning));
            }
        }
        out.sort(Comparator.comparingLong(Hit::offset));
        return out;
    }

    static String evidenceNear(File file,long center,int radius){ return evidenceNear(file,center,1,radius); }

    static String evidenceNear(File file,long center,int rowLength,int radius){
        List<Hit> hits=findNear(file,center,radius);
        if(hits.isEmpty()) return "";
        StringBuilder s=new StringBuilder();
        s.append("Verified nearby marker");
        if(hits.size()!=1)s.append('s');
        s.append(':');
        for(Hit h:hits){
            long d=h.offset-center;
            s.append("\n  ").append(h.hex).append(" = ").append(h.meaning)
                    .append(" @ ").append(h.offset)
                    .append(" (").append(d>=0?"+":"").append(d).append(" bytes from row)");
            Layout l=layoutFor(h.hex);
            if(l!=null){
                long valueStart=h.offset+l.valueDelta;
                long valueEnd=valueStart+l.valueWidth;
                long rowEnd=center+Math.max(1,rowLength);
                if(center<valueEnd && rowEnd>valueStart){
                    s.append("\n    SELECTED RUN OVERLAPS VERIFIED VALUE: ").append(l.valueMeaning)
                            .append(" @ ").append(valueStart).append("..").append(valueEnd-1);
                } else {
                    s.append("\n    Published value location: marker +").append(l.valueDelta)
                            .append(" = ").append(l.valueMeaning)
                            .append(" @ ").append(valueStart).append("..").append(valueEnd-1);
                }
            }
        }
        s.append("\nMarker proximity identifies surrounding record structure. Only an explicit published layout can show that the selected run overlaps the named value; confidence still follows the existing audit rules.");
        return s.toString();
    }

    private static Layout layoutFor(String hex){
        for(Layout l:LAYOUTS) if(l.hex.equals(hex)) return l;
        return null;
    }

    private static Marker m(String hex,String meaning){return new Marker(parse(hex),hex,meaning);}
    private static Layout layout(String hex,int delta,int width,String meaning){return new Layout(hex,delta,width,meaning);}

    private static byte[] parse(String hex){
        byte[] b=new byte[hex.length()/2];
        for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);
        return b;
    }

    private static int indexOf(byte[] data,byte[] needle,int from){
        outer: for(int i=Math.max(0,from);i+needle.length<=data.length;i++){
            for(int j=0;j<needle.length;j++)if(data[i+j]!=needle[j])continue outer;
            return i;
        }
        return -1;
    }
}
