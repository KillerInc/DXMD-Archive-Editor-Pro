import java.util.ArrayList;

import options.BooleanOption;
import options.ByteOption;
import options.FloatOption;
import options.InventoryXOption;
import options.Option;
import options.ShortOption;

/**
 * Canonical catalog of normal base-game editor controls.
 *
 * Research-only and suspected values stay in Research Inspector. This class contains
 * only mappings that are established strongly enough to expose as ordinary
 * controls.
 */
public final class BaseOptionCatalog {
    private BaseOptionCatalog() {}

    private static ArrayList<Long> a(long... values) {
        ArrayList<Long> out = new ArrayList<Long>();
        for (long value : values) out.add(value);
        return out;
    }

    private static ArrayList<Short> shorts(int... values) {
        ArrayList<Short> out = new ArrayList<Short>();
        for (int value : values) out.add((short) value);
        return out;
    }

    public static ArrayList<Option> weaponStats() {
        ArrayList<Option> out = new ArrayList<Option>();
        out.add(new ByteOption(a(4937661), "Tranquilizer Rifle Magazine", "Sets the base Tranquilizer Rifle magazine capacity. Clean game value is 6. Range:0-255", 6));
        out.add(new ByteOption(a(4981373), "Lancer Rifle Magazine", "Sets the base Lancer Rifle magazine capacity. Clean game value is 3. Range:0-255", 3));
        out.add(new ShortOption(a(4264429, 4265549, 4267085, 4268245, 4269245, 4270501, 4285101, 4286237, 4287013, 4288053, 4288885, 4290013, 6615853, 6616941, 6966957, 7525853),
                "Weapon Ammo Stack", "Sets the max inventory stack size of standard weapon ammunition (grenade-launcher ammo excluded). Range:0-65535", 200));
        out.add(new ShortOption(a(4282117, 4282861, 4283605, 4284349),
                "Grenade Launcher Ammo Stack", "Sets the max inventory stack size of the four grenade-launcher ammunition types. Range:0-65535", 10));
        return out;
    }

    public static ArrayList<Option> playerStats() {
        ArrayList<Option> out = new ArrayList<Option>();
        out.add(new FloatOption(a(6577693), "Energy Auto-Regen Limit", "Sets the energy value automatic regeneration can recover to. Clean game value is 35.0; independently isolated by multiple mod comparisons. Range:0-1000", 35.0f, 0.0f, 1000.0f));
        out.add(new FloatOption(a(4570013), "Biocell Energy Gain", "Sets the energy restored by a Biocell. Clean game value is 85.0; controlled No Health Regen variants isolate 42.5 and 28.0. Range:0-1000", 85.0f, 0.0f, 1000.0f));
        out.add(new FloatOption(a(7413189), "Takedown Energy Cost", "Sets the energy consumed by a takedown. Clean game value is 33.0; set to 0 for no takedown energy cost. Range:0-1000", 33.0f, 0.0f, 1000.0f));
        out.add(new BooleanOption(a(6611045, 7589029, 7589797, 7704685, 7718621, 7719573, 7721117, 7722581, 7723565, 7727101),
                shorts(10, 10, 10, 10, 10, 10, 10, 10, 10, 10),
                shorts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                "Augs Non-Experimental", "Makes the mapped experimental augmentations non-experimental, removing the need for the neuroplasticity calibrator.", false));
        return out;
    }

    public static ArrayList<Option> inventoryStats() {
        ArrayList<Option> out = new ArrayList<Option>();
        out.add(new InventoryXOption(a(6784805), "Sniper Rifle Width", dimensionDesc("Sniper Rifle width", 7), 7));
        out.add(new InventoryXOption(a(4936557), "Tranquilizer Rifle Width", dimensionDesc("Tranquilizer Rifle width", 7), 7));
        out.add(new InventoryXOption(a(4987389, 6754613), "Shotgun Width", dimensionDesc("Tactical/Devastator Shotgun width", 5), 5));
        out.add(new InventoryXOption(a(4967181), "Grenade Launcher Width", dimensionDesc("Grenade Launcher width", 4), 4));
        out.add(new InventoryXOption(a(4940389), "Machine Pistol Width", dimensionDesc("Machine Pistol width", 4), 4));
        out.add(new InventoryXOption(a(4951797), "Battle Rifle Width", dimensionDesc("Battle Rifle width", 6), 6));
        out.add(new InventoryXOption(a(4980221), "Lancer Rifle Width", dimensionDesc("Lancer Rifle width", 6), 6));
        out.add(new InventoryXOption(a(4886637), "Combat Rifle Width", dimensionDesc("Combat Rifle width", 5), 5));
        out.add(new InventoryXOption(a(4973813), "Cote d'Azur Rifle Width", dimensionDesc("Cote d'Azur Combat Rifle width", 5), 5));
        out.add(new InventoryXOption(a(4570061), "Biocell Width", dimensionDesc("Biocell width", 2), 2));
        out.add(new InventoryXOption(a(5795997), "Hypostim Width", dimensionDesc("Hypostim width", 2), 2));
        out.add(new InventoryXOption(a(6118589), "Multi-Tool Width", dimensionDesc("Multi-Tool width", 2), 2));
        out.add(new ByteOption(a(4281877, 4282621, 4283365, 4284109), "Grenade Launcher Ammo Height",
                "Sets the mapped inventory dimension for the four grenade-launcher ammunition types. Clean game value is 2 tiles. Range:0-255 " + dimensionWarning(), 2));

        out.add(new ShortOption(a(4907021, 4908021, 5202949, 5204013, 5709093, 5710741, 5711741, 5723317, 5862917, 6783125, 6784101),
                "Grenade / Mine Stack", "Sets the max inventory stack size of thrown grenades and mines. Range:0-65535", 10));
        out.add(new ShortOption(a(4570277), "Biocell Stack", "Sets the max inventory stack size of Biocells. Range:0-65535", 25));
        out.add(new ShortOption(a(4912973), "Painkiller Stack", "Sets the max inventory stack size of painkillers. Range:0-65535", 25));
        out.add(new ShortOption(a(5796213), "Hypostim Stack", "Sets the max inventory stack size of Hypostims. Range:0-65535", 25));
        out.add(new ShortOption(a(5865197), "Weapon Parts Stack", "Sets the max inventory stack size of Weapon Parts. Clean game value is 999. Range:0-65535", 999));
        return out;
    }

    public static ArrayList<Option> economyCrafting() {
        ArrayList<Option> out = new ArrayList<Option>();
        out.add(new ShortOption(a(7736149), "Praxis Shop Cost", "Sets the credit cost of a Praxis Kit in stores. Range:0-65535", 10000));
        out.add(new ShortOption(a(4570325), "Biocell Shop Cost", "Sets the credit cost of a Biocell in stores. Range:0-65535", 200));
        out.add(new ShortOption(a(5796261), "Hypostim Shop Cost", "Sets the credit cost of a Hypostim in stores. Range:0-65535", 150));
        out.add(new ShortOption(a(4913021), "Painkiller Shop Cost", "Sets the credit cost of a Painkiller bottle in stores. Range:0-65535", 50));
        out.add(new ShortOption(a(6119021), "Multi-Tool Shop Cost", "Sets the credit cost of a Multi-Tool in stores. Range:0-65535", 800));
        out.add(new ShortOption(a(7558933), "Typhoon Ammo Shop Cost", "Sets the credit cost of a single Typhoon ammo unit; pack prices derive from this value. Range:0-65535", 500));
        out.add(new ShortOption(a(5852637), "Tesla Ammo Shop Cost", "Sets the credit cost of a single TESLA ammo unit; pack prices derive from this value. Range:0-65535", 200));
        out.add(new ShortOption(a(5853429), "Nanoblade Ammo Shop Cost", "Sets the credit cost of a single Nanoblade ammo unit; pack prices derive from this value. Range:0-65535", 200));
        out.add(new ShortOption(a(5865149), "Weapon Part Shop Cost", "Sets the credit cost of a single Weapon Part; pack prices derive from this value. Range:0-65535", 5));
        out.add(new ShortOption(a(6789165), "Reveal Shop Cost", "Sets the credit cost of Reveal hacking software. Range:0-65535", 250));
        out.add(new ShortOption(a(6789837), "Stealth Shop Cost", "Sets the credit cost of Stealth hacking software. Range:0-65535", 250));
        out.add(new ShortOption(a(6790509), "Nuke Shop Cost", "Sets the credit cost of Nuke hacking software. Range:0-65535", 150));
        out.add(new ShortOption(a(6791181), "Datascan Shop Cost", "Sets the credit cost of Datascan hacking software. Range:0-65535", 200));
        out.add(new ShortOption(a(6791853), "Stop! Shop Cost", "Sets the credit cost of Stop! hacking software. Range:0-65535", 200));
        out.add(new ShortOption(a(6792525), "Overclock Shop Cost", "Sets the credit cost of Overclock hacking software. Range:0-65535", 200));
        out.add(new ShortOption(a(7562693), "Typhoon Ammo Crafting Cost", "Sets the Weapon Parts needed to craft a 3-pack of Typhoon ammo. Range:0-65535", 75));
        out.add(new ShortOption(a(7557853), "Mine Template Crafting Cost", "Sets the Weapon Parts needed to craft a Mine Template. Range:0-65535", 75));
        out.add(new ShortOption(a(5865517), "Biocell Crafting Cost", "Sets the Weapon Parts needed to craft a Biocell. Range:0-65535", 120));
        out.add(new ShortOption(a(6119341), "Multi-Tool Crafting Cost", "Sets the Weapon Parts needed to craft a Multi-Tool. Range:0-65535", 120));
        out.add(new ShortOption(a(6172733), "Nanoblade Crafting Cost", "Sets the Weapon Parts needed to craft a Nanoblade ammo pack. Range:0-65535", 75));
        out.add(new ShortOption(a(7441693), "Tesla Ammo Crafting Cost", "Sets the Weapon Parts needed to craft a TESLA ammo pack. Range:0-65535", 75));
        return out;
    }

    public static ArrayList<Option> xpRewards() {
        ArrayList<Option> out = new ArrayList<Option>();
        addXP(out, "Script Kiddie (Hack L1)", "Hacking level 1 reward; internal hacking_lvl_1.", 25, 5401405);
        addXP(out, "Grey Hat (Hack L2)", "Hacking level 2 reward; internal hacking_lvl_2.", 50, 5401429);
        addXP(out, "Black Hat (Hack L3)", "Hacking level 3 reward; internal hacking_lvl_3.", 75, 5401453);
        addXP(out, "Network Adept (Hack L4)", "Hacking level 4 reward; internal hacking_lvl_4.", 100, 5401477);
        addXP(out, "Master Hacker (Hack L5)", "Hacking level 5 reward; internal hacking_lvl_5.", 125, 5401501);
        addXP(out, "First Try", "Complete a hack on the first attempt; internal hacking_firsttry.", 5, 5401525);
        addXP(out, "Access Granted (Code L1)", "Password/keycode level 1 reward; internal pw_lvl_1.", 25, 5402277);
        addXP(out, "Free Admission (Code L2)", "Password/keycode level 2 reward; internal pw_lvl_2.", 50, 5402301);
        addXP(out, "Open Sesame (Code L3)", "Password/keycode level 3 reward; internal pw_lvl_3.", 75, 5402325);
        addXP(out, "Entering without Breaking (Code L4)", "Password/keycode level 4 reward; internal pw_lvl_4.", 100, 5402349);
        addXP(out, "Master Felonist (Code L5)", "Password/keycode level 5 reward; internal pw_lvl_5.", 125, 5402373);
        addXP(out, "Ghost", "Objective completed unseen; internal obj_notseen.", 200, 5400197);
        addXP(out, "Smooth Operator", "Objective completed without triggering an alarm; internal obj_noalarm.", 200, 5400221);
        addXP(out, "Reset", "Alarm state subsides/returns to cautious; internal moodswing_cautiousreturn.", 10, 5402165);
        int[] mainXP = {500, 1000, 1500, 500, 1950, 3150, 250, 1000, 1000};
        long[] mainOff = {5400269, 5400293, 5400317, 5400461, 5400485, 5400509, 5400533, 5400557, 5400581};
        for (int i = 0; i < mainXP.length; i++) addXP(out, "Getting Things Done " + (i + 1), "Main-objective XP record " + (i + 1) + "; internal obj_getthingsdone family.", mainXP[i], mainOff[i]);
        int[] sideXP = {350, 750, 1500, 0, 350, 750, 1500, 250, 750, 750};
        long[] sideOff = {5400365, 5400389, 5400413, 5400605, 5400629, 5400653, 5400677, 5400701, 5400725, 5400749};
        for (int i = 0; i < sideXP.length; i++) addXP(out, "Completionist " + (i + 1), "Side-objective XP record " + (i + 1) + "; internal obj_sidequest family.", sideXP[i], sideOff[i]);
        addXP(out, "Paving the Way", "Remote-hack environmental device; internal remotehacking_environment.", 5, 5402477);
        addXP(out, "Machina", "Remote-hack security device/vehicle; internal remotehacking_success.", 10, 5401949);
        addXP(out, "Flawless", "Remote hack without a mistimed input; internal remotehacking_nomiss_alt.", 5, 5401973);
        addXP(out, "Traveler", "Exploration reward; internal secretarea_traveler.", 100, 5402861);
        addXP(out, "Explorer", "Exploration reward; internal secretarea_explorer.", 200, 5402885);
        addXP(out, "Pathfinder", "Exploration reward; internal secretarea_pathfinder.", 300, 5402909);
        addXP(out, "Trailblazer", "Exploration reward; internal secretarea_trailblazer.", 400, 5402933);
        addXP(out, "Scholar", "Read a unique eBook; internal collect_scholar.", 100, 5405541);
        addXP(out, "Wait Your Turn", "Failed CASIE/QTE interrupt; internal social_interrupt_fail.", 50, 5401677);
        addXP(out, "Stop the Press", "Successful CASIE/QTE interrupt; internal social_interrupt_win.", 200, 5401789);
        addXP(out, "Life Lesson", "Major persuasion failure; internal social_debate_lose.", 250, 5401901);
        addXP(out, "Split Decision", "Major persuasion partial success; internal social_debate_neutral.", 500, 5405365);
        addXP(out, "Silver Tongue", "Major persuasion success; internal social_debate_win.", 1000, 5403117);
        addXP(out, "Read the Room", "Minor persuasion failure; internal social_persuade_fail.", 100, 5402533);
        addXP(out, "On the Fence", "Minor persuasion partial success; internal social_persuade_split.", 250, 5402589);
        addXP(out, "Spin Doctor", "Minor persuasion success; internal social_persuade_win.", 500, 5402645);
        addXP(out, "Trooper XP", "Base Trooper-tier neutralization XP; internal combat_incap_smallfry.", 10, 5403165);
        addXP(out, "Veteran XP", "Base Veteran-tier neutralization XP; internal combat_incap_veteran.", 20, 5405757);
        addXP(out, "Elite XP", "Base Elite-tier neutralization XP; internal combat_incap_bigdawg.", 30, 5398965);
        addXP(out, "Marchenko XP", "Viktor Marchenko base reward; internal combat_incap_sorrytodisappoint.", 100, 5404285);
        addXP(out, "Merciful Soul", "Non-lethal neutralization bonus; internal combat_xp_nonlethal.", 20, 5398989, 5403189, 5404309, 5405781);
        addXP(out, "Marksman", "Headshot neutralization bonus; internal combat_xp_headshot.", 10, 5399013, 5403213, 5404333, 5405805);
        addXP(out, "Expedient", "Standard melee-takedown bonus. The boss-specific 20-XP record remains research-only.", 10, 5399037, 5403237, 5405829);
        addXP(out, "Multitasker", "Double-takedown bonus; internal combat_xp_takedownmulti.", 45, 5399061, 5402221, 5403261, 5404381, 5405853);
        addXP(out, "Shock Therapy", "TESLA neutralization bonus.", 10, 5399085, 5403285, 5404405, 5405877);
        addXP(out, "Surprise", "Punch-through-wall neutralization bonus.", 10, 5399109, 5403309, 5404429, 5405901);
        addXP(out, "Close Shave", "Nanoblade neutralization bonus.", 10, 5399133, 5403333, 5404453, 5405925);
        addXP(out, "Dust to Dust", "P.E.P.S. focused-blast neutralization bonus.", 10, 5399157, 5403357, 5404477, 5405949);
        addXP(out, "Introvert", "Typhoon neutralization bonus.", 10, 5399181, 5403381, 5404501, 5405973);
        addXP(out, "Juggernaut", "Charged Icarus Dash neutralization bonus.", 10, 5399205, 5403405, 5404525, 5405997);
        addXP(out, "Crash Landing", "Icarus Strike neutralization bonus.", 10, 5399229, 5403429, 5404549, 5406021);
        addXP(out, "Piece by Piece", "Armor-destruction XP chunk; internal combat_xp_piecebypiece.", 5, 5399253, 5402693, 5403453, 5404573, 5406045);
        addXP(out, "Sharpshooter", "Focus Enhancement streak bonus.", 5, 5399277, 5403061, 5403477, 5404597, 5406069);
        addXP(out, "Chain Reaction", "Multi-target TESLA bonus.", 5, 5399301, 5400085, 5403501, 5404621, 5406093);
        addXP(out, "Master Blaster", "Explosive Nanoblade multi-kill bonus.", 5, 5399325, 5402109, 5403525, 5404645, 5406117);
        addXP(out, "Ring of Fire", "Multi-target Typhoon bonus.", 5, 5399373, 5402749, 5403573, 5404693, 5406165);
        addXP(out, "Blown Away", "Fragmentation grenade/mine neutralization bonus.", 5, 5399397, 5403597, 5404717, 5406189);
        addXP(out, "Collateral Damage", "Multi-target fragmentation explosion bonus.", 10, 5399421, 5400141, 5403621, 5404741, 5406213);
        addXP(out, "Scrap Metal", "Destroy a turret; internal combat_disable_turret.", 15, 5402805);
        addXP(out, "Void Warranty", "Destroy a flying drone; internal combat_disable_drone.", 20, 5406877);
        addXP(out, "Junk Yard", "Destroy a walker/sentry robot; internal combat_disable_sentry.", 40, 5401845);
        return out;
    }

    private static void addXP(ArrayList<Option> out, String name, String desc, int defaultValue, long... offsets) {
        out.add(new ShortOption(a(offsets), name, desc + " Range:0-65535", defaultValue));
    }

    private static String dimensionDesc(String what, int defaultValue) {
        return what + " in inventory tiles. Default: " + defaultValue + ". Range: 0–16. " + dimensionWarning();
    }

    private static String dimensionWarning() {
        return "Save-risk; a full warning is shown before apply.";
    }
}
