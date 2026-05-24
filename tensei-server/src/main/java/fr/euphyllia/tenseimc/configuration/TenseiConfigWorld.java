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

    public Mobs mobs;
    public class Mobs extends ConfigurationPart {
        public Bee bee;
        public class Bee extends ConfigurationPart {
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

    public Blocks blocks;
    public class Blocks extends ConfigurationPart {
        public Beacon beacon;
        public class Beacon extends ConfigurationPart {
            public int levelOne = 20;
            public int levelTwo = 30;
            public int levelThree = 40;
            public int levelFour = 50;
        }

        public Piston piston;
        public class Piston extends ConfigurationPart {
            public int pushLimit = 12;
        }

        public Chest chest;
        public class Chest extends ConfigurationPart {
            @Comment("Allow chests to output a comparator signal based on their contents.")
            public boolean comparatorOutputEnabled = true;

            @Comment("Allow chests to be waterlogged.")
            public boolean allowWaterlogging = true;
        }

        public Farmland farmland;
        public class Farmland extends ConfigurationPart {
            @Comment("""
                    Minimum fall distance (in blocks) required to trample farmland.
                    The actual threshold is fallDistance - this value > 0.
                    Default: 0.5""")
            public double trampleMinFallDistance = 0.5;
            @Comment("""
                    Minimum entity volume (width * width * height) required to trample farmland.
                    Entities smaller than this value cannot trample farmland.
                    Default: 0.512""")
            public double trampleMinEntityVolume = 0.512;

            @Comment("Horizontal radius (in blocks) to search for water sources around farmland.\n" +
                    "Default: 4")
            public int waterSearchRadius = 4;

            @Comment("Vertical range (in blocks above farmland level) to search for water sources.\n" +
                    "Default: 1")
            public int waterSearchHeight = 1;
        }

        public SugarCane sugarCane;
        public class SugarCane extends ConfigurationPart {
            @Comment("Probability (0.0 - 1.0) that bonemeal successfully grows the sugar cane by one block.")
            public float bonemealSuccessChance = 1.0F;
        }

        public NetherWart netherWart;
        public class NetherWart extends ConfigurationPart {
            @Comment("Probability (0.0 - 1.0) that bonemeal successfully grows the nether wart.")
            public float bonemealSuccessChance = 1.0F;

            @Comment("Number of growth stages advanced per bonemeal use. Set to -1 for random (1-3).")
            public int bonemealGrowAmount = -1;
        }

        public Beehive beehive;
        public class Beehive extends ConfigurationPart {
            @Comment("Minimum ticks before a bee can re-enter a hive after exiting.\nDefault: 400 (20 seconds)")
            public int minTicksBeforeReenteringHive = 400;

            @Comment("Minimum ticks a bee with nectar must spend in the hive.\nDefault: 2400 (2 minutes)")
            public int minOccupationTicksNectar = 2400;

            @Comment("Minimum ticks a bee without nectar must spend in the hive.\nDefault: 600 (30 seconds)")
            public int minOccupationTicksNectarless = 600;

            @Comment("Probability per tick that the hive plays its ambient work sound while occupied.\nDefault: 0.005")
            public double workSoundChance = 0.005;

            @Comment("Probability that a released bee inherits the hive's saved flower position.\nDefault: 0.9")
            public float flowerPosInheritChance = 0.9F;

            @Comment("Probability (1 in N) that a honey delivery increases the honey level by 2 instead of 1.\nDefault: 100")
            public int doubleHoneyIncreaseChance = 100;
        }
    }

    public Weather weather;
    public class Weather extends ConfigurationPart {
        @Comment("Enabled weather per region.\nDefault: false")
        public boolean perRegion = false;
        public String commandGetClear = "The weather is clear";
        public String commandGetRain = "The weather is rainy";
        public String commandGetThunder = "The weather is thundering";
    }
}
