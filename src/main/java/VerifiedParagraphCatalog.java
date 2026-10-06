import java.util.*;

/**
 * Verified Base-game paragraph ownership from published DXMD hex-edit research.
 *
 * This catalog supplies context only. It must never promote a field's confidence by itself.
 * A paragraph owner tells us which weapon/upgrade record contains a row; it does not prove
 * the exact semantic meaning of every changed byte inside that record.
 */
final class VerifiedParagraphCatalog {
    private record P(long start, String owner) {}

    private static final Map<String,P[]> MAP = new HashMap<>();

    static {
        put("Damage",
            p(5020368,"Machine Pistol L1"),p(5020880,"Devastator Shotgun L1"),p(5021344,"Lancer Rifle L1"),
            p(5021824,"Tactical Shotgun L1"),p(5022288,"Revolver L1"),p(5022768,"Combat Rifle L1"),
            p(5023232,"Battle Rifle L1"),p(5023712,"Sniper Rifle L1"),p(5024176,"Combat Rifle L2"),
            p(5024688,"Lancer Rifle L2"),p(5025152,"Revolver L2"),p(5025632,"Sniper Rifle L2"),
            p(5026096,"Tactical Shotgun L2"),p(5026576,"Devastator Shotgun L2"),p(5027040,"Machine Pistol L2"),
            p(5027520,"Battle Rifle L2"),p(5027984,"Sniper Rifle L3"),p(5028496,"Machine Pistol L3"),
            p(5028896,"Revolver L3"),p(5029312,"Combat Rifle L3"),p(5029712,"Battle Rifle L3"),
            p(5030128,"Lancer Rifle L3"),p(5030528,"Devastator Shotgun L3"),p(5030944,"Tactical Shotgun L3"),
            p(5031456,"10mm Pistol L1"),p(5031936,"10mm Pistol L2"),p(5032400,"10mm Pistol L3"));

        put("Rate of Fire",
            p(6543888,"Revolver L1"),p(6544304,"Machine Pistol L1"),p(6544752,"Battle Rifle L1"),
            p(6545184,"Tactical Shotgun L1"),p(6546048,"Battle Rifle L2"),p(6547392,"Revolver L2"),
            p(6547808,"Tactical Shotgun L2"),p(6548256,"Machine Pistol L2"),p(6548720,"Revolver L3"),
            p(6549168,"Tactical Shotgun L3"),p(6549552,"Machine Pistol L3"),p(6550320,"Battle Rifle L3"),
            p(6551152,"10mm Pistol L1"),p(6551600,"10mm Pistol L3"),p(6552048,"10mm Pistol L2"));

        put("Ammo Capacity",
            p(4426672,"Sniper Rifle L3"),p(4427872,"Tactical Shotgun L2"),p(4428880,"Sniper Rifle L1"),
            p(4430480,"Tranquilizer Rifle L1"),p(4431376,"Sniper Rifle L2"),p(4431792,"Stun Gun L1"),
            p(4432112,"Pistol Tutorial L1"),p(4432448,"Tranquilizer Rifle L2"),p(4433184,"Stun Gun L2"),
            p(4434608,"Tranquilizer Rifle L3"),p(4434944,"Stun Gun L3"),p(4435232,"10mm Pistol L3"));

        put("Fire Pattern",
            p(5664672,"Revolver Hair Trigger"),p(5665120,"10mm Pistol Full Auto"),p(5665456,"Machine Pistol Full Auto"),
            p(5665760,"Combat Rifle Semi Auto"),p(5666064,"Pistol Tutorial Full Auto"),p(5666576,"Tactical Shotgun Burst"));

        put("Recoil",
            p(6557104,"Tactical Shotgun L1"),p(6559328,"Revolver L1"),p(6560528,"Tranquilizer Rifle L1"),
            p(6561072,"Battle Rifle L1"),p(6562128,"10mm Pistol L1"),p(6562608,"Machine Pistol L1"),
            p(6563104,"Cote d'Azur L1"),p(6564272,"Grenade Launcher L1"),p(6569744,"Cote d'Azur L2"),
            p(6570544,"Battle Rifle L2"),p(6571104,"Combat Rifle Tutorial L2"),p(6572528,"Combat Rifle L2"),
            p(6599744,"Combat Rifle L1"));

        put("Reload Speed",
            p(5697648,"Combat Rifle L1"),p(6586192,"Tactical Shotgun L1"),p(6586576,"Tactical Shotgun L2"),
            p(6586992,"Tranquilizer Rifle L1"),p(6587680,"Lancer Rifle L1"),p(6588064,"Combat Rifle Tutorial L1"),
            p(6588432,"10mm Pistol L1"),p(6588800,"Grenade Launcher L1"),p(6589184,"Battle Rifle L2"),
            p(6589552,null),p(6589872,"Combat Rifle L2"),p(6590240,"Cote d'Azur L1"),
            p(6590560,"Stun Gun L1"),p(6590928,"Machine Pistol L2"),p(6591568,"Sniper Rifle L1"),
            p(6591920,null),p(6592304,"Machine Pistol L1"),p(6592672,"Battle Rifle L1"),
            p(6593056,"Pistol Tutorial L1"),p(6593424,"Revolver L2"),p(6593808,"Stun Gun L2"),
            p(6594112,null),p(6594432,"Grenade Launcher L2"),p(6594736,null),p(6595056,"Pistol Tutorial L2"),
            p(6595360,"Combat Rifle Tutorial L2"),p(6595680,"Tranquilizer Rifle L2"),p(6595984,null),
            p(6596304,"Cote d'Azur L2"),p(6596608,"Sniper Rifle L2"),p(6596928,null),
            p(6597232,null));

        put("Accuracy",p(4317472,"Stun Gun L1"),p(4326784,"Stun Gun L2"));
        put("Laser",p(5883232,"Stun Gun"));

        put("Silencer",
            p(6765424,"Lancer Rifle"),p(6766288,"Machine Pistol"),p(6767488,"10mm Pistol"),
            p(6768224,"Tactical Shotgun"),p(6769072,null),p(6769792,"Combat Rifle"));
    }

    private VerifiedParagraphCatalog() {}

    static String ownerFor(long offset, String family) {
        P[] a=MAP.get(family);
        if(a==null) return null;
        for(int i=0;i<a.length;i++) {
            long start=a[i].start;
            long next=i+1<a.length ? a[i+1].start : start+768;
            long end=Math.min(next,start+768);
            if(offset>=start && offset<end) return a[i].owner;
        }
        return null;
    }

    static boolean hasVerifiedOwner(long offset, String family) {
        return ownerFor(offset,family)!=null;
    }

    private static P p(long start,String owner){ return new P(start,owner); }
    private static void put(String family,P... entries){
        Arrays.sort(entries,Comparator.comparingLong(P::start));
        MAP.put(family,entries);
    }
}
