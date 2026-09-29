package weather2.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import weather2.config.ConfigMisc;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH helper for Eve (dim 18) and Tekto (dim 24) climate profiles,
 * audio calibration, and planetary tornado block destruction safety.
 */
public class WeatherEveHelper {

    public static final int DIM_OVERWORLD = 0;
    public static final int DIM_DUNA = 16;
    public static final int DIM_EVE = 18;
    public static final int DIM_LAYTHE = 22;
    public static final int DIM_TEKTO = 24;

    public static final int MAX_MOVING_BLOCKS_PER_DIM = 200;

    public static boolean isEve(StormObject so) {
        return so != null && so.manager != null && so.manager.dim == DIM_EVE;
    }

    public static boolean isTekto(StormObject so) {
        return so != null && so.manager != null && so.manager.dim == DIM_TEKTO;
    }

    public static void onInitRealStorm(StormObject so) {
        if (isEve(so)) {
            so.alwaysProgresses = true;
            so.maxIntensityStage = StormObject.STATE_STAGE5;
            if (so.levelCurIntensityStage < StormObject.STATE_HIGHWIND) {
                so.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
            }
        } else if (isTekto(so)) {
            // NTNH: Tekto storm intensity capped at Stage 4 (F4)
            if (so.maxIntensityStage > StormObject.STATE_STAGE4) {
                so.maxIntensityStage = StormObject.STATE_STAGE4;
            }
        }
    }

    public static int getAdjustedLightningOdds(int baseOdds, StormObject so) {
        if (isEve(so)) {
            // 6x more frequent lightning on Eve, with safe clamp to avoid network packet floods
            return Math.max(15, baseOdds / 6);
        } else if (isTekto(so)) {
            // 2x more frequent dry lightning on Tekto
            return Math.max(1, baseOdds / 2);
        }
        return Math.max(1, baseOdds);
    }

    public static int getDeadlyTimeBetween(int defaultTicks, StormObject so) {
        if (isEve(so)) {
            // 40 seconds (800 ticks) cooldown on Eve instead of 90 seconds (1800 ticks)
            return Math.min(defaultTicks, 800);
        }
        return defaultTicks;
    }

    public static int getLandStormOdds(int defaultOdds, StormObject so) {
        if (isEve(so)) {
            // 3x faster spontaneous land storm formation on Eve (1 in 3 when base is 10)
            return Math.max(1, defaultOdds / 3);
        }
        return defaultOdds;
    }

    public static int getOceanStormOdds(int defaultOdds, StormObject so) {
        if (isEve(so)) {
            // 3x faster ocean cyclones on Eve (1 in 7 when base is 20)
            return Math.max(1, defaultOdds / 3);
        }
        return defaultOdds;
    }

    // ----------------------------------------------------
    // Safe-by-Default Dimension Whitelist for Block Grabbing
    // ----------------------------------------------------
    private static String lastGrabDimsConfig = null;
    private static Set<Integer> allowedGrabDims = new HashSet<Integer>();

    /**
     * Safe-by-default check: tornado block grabbing is only permitted in dimensions
     * explicitly declared in ConfigMisc.Dimension_List_TornadoGrabBlocks.
     * All unlisted dimensions (Overworld, Nether, Twilight Forest, vacuum moons, etc.) are strictly immune.
     */
    public static boolean isDimensionGrabAllowed(int dim) {
        String cfg = null;
        try {
            Field f = ConfigMisc.class.getField("Dimension_List_TornadoGrabBlocks");
            cfg = (String) f.get(null);
        } catch (Throwable t) {
            cfg = "16, 18, 22, 24";
        }

        if (cfg == null || cfg.trim().isEmpty()) {
            return false;
        }

        if (!cfg.equals(lastGrabDimsConfig)) {
            Set<Integer> set = new HashSet<Integer>();
            String[] parts = cfg.replace(",", " ").trim().split("\\s+");
            for (String p : parts) {
                if (!p.isEmpty()) {
                    try {
                        set.add(Integer.parseInt(p));
                    } catch (NumberFormatException ignored) {}
                }
            }
            allowedGrabDims = set;
            lastGrabDimsConfig = cfg;
        }

        return allowedGrabDims.contains(dim);
    }

    /**
     * Determines whether a tornado can rip/grab blocks.
     * Safe-by-default: only dimensions explicitly declared in Dimension_List_TornadoGrabBlocks are allowed.
     * Overworld (0), Nether (-1), and all third-party/vacuum dimensions are strictly immune by default.
     * Also enforces a hard dimension cap (MAX_MOVING_BLOCKS_PER_DIM = 200) to protect server TPS.
     */
    public static boolean canTornadoGrabBlocks(StormObject so) {
        if (!ConfigMisc.Storm_Tornado_grabBlocks) return false;
        if (so == null || so.manager == null) return false;
        int dim = so.manager.dim;
        if (!isDimensionGrabAllowed(dim)) {
            return false;
        }

        // Global hard cap on EntityMovingBlock per dimension to protect server TPS
        if (so.manager.getStormObjects() != null) {
            int totalBlocksInDim = 0;
            List storms = so.manager.getStormObjects();
            for (int i = 0; i < storms.size(); i++) {
                Object obj = storms.get(i);
                if (obj instanceof StormObject) {
                    StormObject other = (StormObject) obj;
                    if (other.tornadoHelper != null) {
                        totalBlocksInDim += other.tornadoHelper.blockCount;
                    }
                }
            }
            if (totalBlocksInDim >= MAX_MOVING_BLOCKS_PER_DIM) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether a block is immune from being grabbed by a tornado.
     * Protects:
     * - Bedrock, tree logs, chests, jukeboxes (upstream defaults)
     * - Unbreakable blocks (hardness < 0)
     * - All TileEntities and BlockContainers (machines, mod chests, cables, conduits)
     * - All Weather2 instruments (sensor, siren, deflector, radar, anemometer, wind vane)
     */
    public static boolean isBlockProtected(Object blockObj) {
        if (blockObj == null) return true;

        Class<?> clazz = blockObj.getClass();
        while (clazz != null && clazz != Object.class) {
            String name = clazz.getName();
            // 1. All Weather2 instruments and machines
            if (name.startsWith("weather2.block.")) {
                return true;
            }
            // 2. Containers, logs, chests, jukebox
            if (name.equals("net.minecraft.block.BlockContainer") ||
                name.equals("net.minecraft.block.BlockLog") ||
                name.equals("net.minecraft.block.BlockChest") ||
                name.equals("net.minecraft.block.BlockJukebox")) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }

        // 3. TileEntity check (covers all modded machines, cables, pipes, and crates)
        try {
            Method m = blockObj.getClass().getMethod("hasTileEntity", int.class);
            if (((Boolean) m.invoke(blockObj, 0)).booleanValue()) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 4. Unbreakable blocks (hardness < 0)
        for (Class<?> c = blockObj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField("blockHardness");
                f.setAccessible(true);
                if (f.getFloat(blockObj) < 0.0F) return true;
            } catch (Throwable ignored) {}
            try {
                Field f = c.getDeclaredField("field_149782_v");
                f.setAccessible(true);
                if (f.getFloat(blockObj) < 0.0F) return true;
            } catch (Throwable ignored) {}
        }

        return false;
    }

    /**
     * Enables visible rotating funnels for cyclones as well as tornadoes.
     */
    public static boolean shouldSpawnFunnel(StormObject so) {
        if (so == null) return false;
        return so.isTornadoFormingOrGreater() || so.isCycloneFormingOrGreater() || so.attrib_waterSpout;
    }

    /**
     * Normalizes user commands, adding aliases for /weather2 kill and /weather2 spawn.
     */
    public static String[] normalizeCommandArgs(String[] args) {
        if (args == null || args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return new String[] { "help" };
        }
        if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("killall") || args[0].equalsIgnoreCase("clear")) {
            return new String[] { "storm", "killall" };
        }
        if ((args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("create")) && args.length > 1) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "storm";
            newArgs[1] = "create";
            System.arraycopy(args, 1, newArgs, 2, args.length - 1);
            return newArgs;
        }
        return args;
    }

    /**
     * Scales thunder volume for voice chat compatibility (~70%).
     */
    public static float getAdjustedThunderVolume(float vol) {
        return vol * 0.70F;
    }

    /**
     * Scales tornado wind sound volume (~75%).
     */
    public static float getAdjustedWindVolume(float vol) {
        return vol * 0.75F;
    }

    // ----------------------------------------------------
    // Dynamic Surroundings Synergy & Rain Synchronization
    // ----------------------------------------------------
    private static boolean worldRainInit = false;
    private static Method worldSetRainStrength = null;
    private static Field worldRainingStrength = null;
    private static Field worldPrevRainingStrength = null;

    private static boolean dsurroundInit = false;
    private static Method dsurroundSetIntensity = null;

    public static void setWorldRainStrength(Object worldObj, float strength) {
        if (worldObj == null) return;
        if (!worldRainInit) {
            worldRainInit = true;
            Class<?> wc = worldObj.getClass();
            try {
                worldSetRainStrength = wc.getMethod("setRainStrength", float.class);
            } catch (Throwable t1) {
                try {
                    worldSetRainStrength = wc.getMethod("func_72885_k", float.class);
                } catch (Throwable t2) {
                    worldSetRainStrength = null;
                }
            }
            if (worldSetRainStrength == null) {
                for (Class<?> c = wc; c != null && c != Object.class; c = c.getSuperclass()) {
                    try {
                        Field f = c.getDeclaredField("rainingStrength");
                        f.setAccessible(true);
                        worldRainingStrength = f;
                    } catch (Throwable ignored) {}
                    try {
                        Field f = c.getDeclaredField("field_73004_o");
                        f.setAccessible(true);
                        worldRainingStrength = f;
                    } catch (Throwable ignored) {}

                    try {
                        Field f = c.getDeclaredField("prevRainingStrength");
                        f.setAccessible(true);
                        worldPrevRainingStrength = f;
                    } catch (Throwable ignored) {}
                    try {
                        Field f = c.getDeclaredField("field_73003_n");
                        f.setAccessible(true);
                        worldPrevRainingStrength = f;
                    } catch (Throwable ignored) {}

                    if (worldRainingStrength != null && worldPrevRainingStrength != null) break;
                }
            }
        }

        if (worldSetRainStrength != null) {
            try {
                worldSetRainStrength.invoke(worldObj, strength);
                return;
            } catch (Throwable ignored) {}
        }

        if (worldRainingStrength != null && worldPrevRainingStrength != null) {
            try {
                worldRainingStrength.setFloat(worldObj, strength);
                worldPrevRainingStrength.setFloat(worldObj, strength);
            } catch (Throwable ignored) {}
        }
    }

    public static void updateDSurroundIntensity(float intensity) {
        if (!dsurroundInit) {
            dsurroundInit = true;
            try {
                Class<?> clazz = Class.forName("org.blockartistry.mod.DynSurround.client.weather.Weather");
                dsurroundSetIntensity = clazz.getMethod("setIntensity", float.class);
            } catch (Throwable t) {
                dsurroundSetIntensity = null;
            }
        }
        if (dsurroundSetIntensity != null) {
            try {
                dsurroundSetIntensity.invoke(null, intensity);
            } catch (Throwable ignored) {}
        }
    }

    public static void onPrecipitationTick(float curPrecipStr) {
        try {
            Object handler = Class.forName("cpw.mods.fml.client.FMLClientHandler").getMethod("instance").invoke(null);
            if (handler != null) {
                Object mc = handler.getClass().getMethod("getClient").invoke(handler);
                if (mc != null) {
                    Object world = null;
                    try {
                        world = mc.getClass().getField("theWorld").get(mc);
                    } catch (Throwable t) {
                        try {
                            world = mc.getClass().getField("field_71441_e").get(mc);
                        } catch (Throwable ignored) {}
                    }
                    if (world != null) {
                        float strength = Math.abs(curPrecipStr);
                        setWorldRainStrength(world, strength);
                        updateDSurroundIntensity(strength);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
