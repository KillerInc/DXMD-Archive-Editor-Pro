from pathlib import Path
import base64,gzip

R=Path("src/main/resources")
parts=["base_research.part01.b64","base_research.part02.b64"]+[f"base_research.tail{i:02d}.b64" for i in range(1,10)]
raw=gzip.decompress(base64.b64decode("".join((R/p).read_text() for p in parts))).decode()
refs=[]; rows=[]
for line in raw.splitlines():
    if not line or line.startswith("#"): continue
    x=line.split("\t")
    if x[0]=="REFERENCES": refs=x[1:]
    elif x[0]=="FIELD":
        rows.append([int(x[1]),x[2],x[3],x[4],dict(zip(refs,x[5:]))])

patch=gzip.decompress(base64.b64decode((R/"base_research_v069_patch.tsv.gz.b64").read_text())).decode()
remove=set()
for line in patch.splitlines():
    if not line or line.startswith("#"): continue
    x=line.split("\t")
    if x[0]=="REMOVED": remove.update(x[1].split(","))
    elif x[0]=="FIELD": rows.append([int(x[1]),x[2],x[3],x[4],dict(zip(refs,x[5:]))])
rows=[r for r in rows if f"{r[0]}:{len(r[3])//2}" not in remove]
rows.sort()

P={}
def add(fam,data):
    P.setdefault(fam,[]).extend(data)
add("Damage",[
("Machine Pistol L1",5020368),("Devastator Shotgun L1",5020880),("Lancer Rifle L1",5021344),("Tactical Shotgun L1",5021824),
("Revolver L1",5022288),("Combat Rifle L1",5022768),("Battle Rifle L1",5023232),("Sniper Rifle L1",5023712),
("Combat Rifle L2",5024176),("Lancer Rifle L2",5024688),("Revolver L2",5025152),("Sniper Rifle L2",5025632),
("Tactical Shotgun L2",5026096),("Devastator Shotgun L2",5026576),("Machine Pistol L2",5027040),("Battle Rifle L2",5027520),
("Sniper Rifle L3",5027984),("Machine Pistol L3",5028496),("Revolver L3",5028896),("Combat Rifle L3",5029312),
("Battle Rifle L3",5029712),("Lancer Rifle L3",5030128),("Devastator Shotgun L3",5030528),("Tactical Shotgun L3",5030944),
("10mm Pistol L1",5031456),("10mm Pistol L2",5031936),("10mm Pistol L3",5032400)])
add("Rate of Fire",[("Revolver L1",6543888),("Machine Pistol L1",6544304),("Battle Rifle L1",6544752),("Tactical Shotgun L1",6545184),
("Battle Rifle L2",6546048),("Revolver L2",6547392),("Tactical Shotgun L2",6547808),("Machine Pistol L2",6548256),
("Revolver L3",6548720),("Tactical Shotgun L3",6549168),("Machine Pistol L3",6549552),("Battle Rifle L3",6550320),
("10mm Pistol L1",6551152),("10mm Pistol L3",6551600),("10mm Pistol L2",6552048)])
add("Ammo Capacity",[("Sniper Rifle L3",4426672),("Tactical Shotgun L2",4427872),("Sniper Rifle L1",4428880),("Tranquilizer Rifle L1",4430480),
("Sniper Rifle L2",4431376),("Stun Gun L1",4431792),("Tranquilizer Rifle L2",4432448),("Stun Gun L2",4433184),
("Tranquilizer Rifle L3",4434608),("Stun Gun L3",4434944),("10mm Pistol L3",4435232)])
add("Fire Pattern",[("Revolver Hair Trigger",5664672),("10mm Pistol Full Auto",5665120),("Machine Pistol Full Auto",5665456),("Combat Rifle Semi Auto",5665760),("Tactical Shotgun Burst",5666576)])
add("Silencer",[("Lancer Rifle",6765424),("Machine Pistol",6766288),("10mm Pistol",6767488),("Tactical Shotgun",6768224),("UNKNOWN",6769072),("Combat Rifle",6769792)])
add("Recoil",[("Tactical Shotgun L1",6557104),("Revolver L1",6559328),("Tranquilizer Rifle L1",6560528),("Battle Rifle L1",6561072),
("10mm Pistol L1",6562128),("Machine Pistol L1",6562608),("Cote d'Azur L1",6563104),("Grenade Launcher L1",6564272),
("Cote d'Azur L2",6569744),("Battle Rifle L2",6570544),("Combat Rifle Tutorial L2",6571104),("Combat Rifle L2",6572528),("Combat Rifle L1",6599744)])
add("Reload Speed",[("Combat Rifle L1",5697648),("Tactical Shotgun L1",6586192),("Tactical Shotgun L2",6586576),("Tranquilizer Rifle L1",6586992),
("Lancer Rifle L1",6587680),("Combat Rifle Tutorial L1",6588064),("10mm Pistol L1",6588432),("Grenade Launcher L1",6588800),
("Battle Rifle L2",6589184),("Combat Rifle L2",6589872),("Stun Gun L1",6590560),("Machine Pistol L2",6590928),("Sniper Rifle L1",6591568),
("Machine Pistol L1",6592304),("Battle Rifle L1",6592672),("Pistol Tutorial L1",6593056),("Revolver L2",6593424),("Stun Gun L2",6593808),
("UNKNOWN",6594112),("Grenade Launcher L2",6594432),("UNKNOWN",6594736),("Pistol Tutorial L2",6595056),("Combat Rifle Tutorial L2",6595360),
("Tranquilizer Rifle L2",6595680),("UNKNOWN",6595984),("Cote d'Azur L2",6596304),("Sniper Rifle L2",6596608),("Devastator Shotgun L2",6596928),("UNKNOWN",6597232)])

prefix={"Damage":"DAMAGE","Rate of Fire":"RATE_OF_FIRE","Ammo Capacity":"AMMO_CAPACITY","Fire Pattern":"FIRE_PATTERN","Silencer":"SILENCER","Recoil":"RECOIL","Reload Speed":"RELOAD_SPEED"}
out=[f"Decoded rows: {len(rows)}","Boundary-safe verified paragraph ownership reconciliation",""]
safe=[]
for fam,plist in P.items():
    plist=sorted(plist,key=lambda z:z[1])
    for i,(owner,start) in enumerate(plist):
        end=plist[i+1][1] if i+1<len(plist) else start+768
        rr=[r for r in rows if start<=r[0]<end]
        out.append(f"[{fam}] {owner} {start}..{end}")
        for r in rr:
            ok=r[2].upper().startswith(prefix[fam])
            changed=[k for k,v in r[4].items() if v and v!=r[3]]
            out.append(f"  {'MATCH' if ok else '     '} {r[0]} +{r[0]-start} {r[2]} orig={r[3]} changed={','.join(changed) or '-'}")
            if ok and owner!="UNKNOWN": safe.append((r[0],r[2],fam,owner,start,end))
        out.append("")
out+=["SAFE OWNER CONTEXT CANDIDATES","offset\tlabel\tfamily\towner\tparagraph\tend"]
seen=set()
for x in safe:
    if x[:2] in seen: continue
    seen.add(x[:2]); out.append("\t".join(map(str,x)))
Path("RESEARCH_RECONCILIATION.txt").write_text("\n".join(out)+"\n")
print("\n".join(out[-100:]))
