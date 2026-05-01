package fr.euphyllia.tenseimc.configuration;

import io.papermc.paper.configuration.Configuration;
import io.papermc.paper.configuration.ConfigurationPart;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "NotNullFieldNotInitialized", "InnerClassMayBeStatic"})
public class TenseiConfigWorld extends ConfigurationPart {

    static final int CURRENT_VERSION = 1;

    @Setting(Configuration.VERSION_FIELD)
    public int version = CURRENT_VERSION;


    public BeaconConfig beaconConfig;
    public class BeaconConfig extends ConfigurationPart {
        public int levelOne = 20;
        public int levelTwo = 30;
        public int levelThree = 40;
        public int levelFour = 50;
    }

    public PistonConfig pistonConfig;
    public class PistonConfig extends ConfigurationPart {
        public int pushLimit = 12;
    }

    public MobsConfig mobsConfig;
    public class MobsConfig extends ConfigurationPart {
        public BeeConfig beeConfig;
        public class BeeConfig extends ConfigurationPart {
            @Comment("Allow bees to leave their hive and work at night.")
            public boolean workAtNight = false;

            @Comment("Allow bees to leave their hive and work during rain.")
            public boolean workInRain = false;

            @Comment("Minimum ticks a bee must spend pollinating a flower to obtain nectar.\nDefault: 400 (20 seconds)")
            public int minPollinationTicks = 400;

            @Comment("Maximum ticks a bee will spend pollinating before giving up.\nDefault: 600 (30 seconds)")
            public int maxPollinationTicks = 600;

            @Comment("Radius (in blocks) around the bee used to search for nearby flowers.\nDefault: 5")
            public int flowerSearchRadius = 5;

            @Comment("Ticks without nectar before the bee tries to return to its known flower position.\nDefault: 600 (30 seconds)")
            public int ticksBeforeGoingToKnownFlower = 600;

            @Comment("Ticks without nectar before the bee gives up searching and returns to its hive.\nDefault: 3600 (3 minutes)")
            public int ticksWithoutNectarBeforeGoingHome = 3600;

            @Comment("Maximum number of crops a bee can grow per nectar load before needing to refill.\nDefault: 10")
            public int maxCropsGrowable = 10;

            @Comment("Inverse probability (1 in N) per tick that the bee attempts to grow a crop below it.\nHigher values = slower growth.\nDefault: 30")
            public int cropGrowChance = 30;

            @Comment("Probability per attempt that the bee skips growing a crop even when conditions are met.\nDefault: 0.3 (30%)")
            public float cropGrowSkipChance = 0.3F;

            @Comment("Radius (in blocks) used by the bee to locate a new hive via the POI manager.\nDefault: 20")
            public int hiveSearchDistance = 20;

            @Comment("Maximum distance (in blocks) before the bee considers its hive or flower too far away and forgets it.\nDefault: 48")
            public int tooFarDistance = 48;

            @Comment("Distance (in blocks) at which the bee is considered close enough to enter its hive.\nDefault: 2")
            public int hiveCloseEnoughDistance = 2;

            @Comment("If the bee is closer than this distance to its hive, it uses direct pathfinding instead of random movement.\nDefault: 16")
            public int pathfindToHiveWhenCloserThan = 16;

            @Comment("Cooldown (in ticks) before the bee tries to locate a new hive after losing one.\nDefault: 200 (10 seconds)")
            public int cooldownBeforeLocatingNewHive = 200;

            @Comment("Cooldown (in ticks) before the bee tries to locate a new flower after pollinating or failing.\nDefault: 200 (10 seconds)")
            public int cooldownBeforeLocatingNewFlower = 200;

            @Comment("Minimum cooldown (in ticks) between two flower-search retries when no flower is found.\nDefault: 20 (1 second)")
            public int minFindFlowerRetryCooldown = 20;

            @Comment("Maximum cooldown (in ticks) between two flower-search retries when no flower is found.\nDefault: 60 (3 seconds)")
            public int maxFindFlowerRetryCooldown = 60;

            @Comment("Maximum ticks a bee will spend traveling to its hive before giving up and blacklisting it.\nDefault: 2400 (2 minutes)")
            public int maxTravellingTicksToHive = 2400;

            @Comment("Maximum ticks a bee will spend traveling to its known flower before giving up.\nDefault: 2400 (2 minutes)")
            public int maxTravellingTicksToFlower = 2400;

            @Comment("Ticks the bee can be stuck on the same path before dropping its hive target.\nDefault: 60 (3 seconds)")
            public int ticksBeforeHiveDrop = 60;

            @Comment("Maximum number of blacklisted (unreachable) hives the bee will remember.\nDefault: 3")
            public int maxBlacklistedHives = 3;

            @Comment("Ticks before a bee dies after stinging an entity.\nDefault: 1200 (1 minute)")
            public int stingDeathCountdown = 1200;

            @Comment("Duration (in seconds) of the poison effect applied on sting in NORMAL difficulty.\nSet to 0 to disable.\nDefault: 10")
            public int poisonSecondsNormal = 10;

            @Comment("Duration (in seconds) of the poison effect applied on sting in HARD difficulty.\nSet to 0 to disable.\nDefault: 18")
            public int poisonSecondsHard = 18;

            @Comment("Distance (in blocks, squared internally) under which an angry bee will attempt to sting its target.\nDefault: 4")
            public int minAttackDistance = 4;

            @Comment("Minimum duration (in seconds) of a bee's persistent anger after being provoked.\nDefault: 20")
            public int angerTimeMin = 20;

            @Comment("Maximum duration (in seconds) of a bee's persistent anger after being provoked.\nDefault: 39")
            public int angerTimeMax = 39;

            @Comment("Ticks a bee can stay underwater before taking drowning damage.\nDefault: 20 (1 second)")
            public int underWaterTicksBeforeDamage = 20;

        }
    }
}
