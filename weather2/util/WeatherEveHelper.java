package weather2.util;

import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.WeatherManagerBase;
import weather2.config.ConfigMisc;

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

    /**
     * Determines whether a tornado can rip/grab blocks.
     * Blocks are destructible ONLY on celestial planets with atmosphere:
     * Duna (16), Eve (18), Laythe (22), Tekto (24).
     * Overworld (0), Nether (-1), and all vacuum celestial bodies are strictly immune.
     */
    public static boolean canTornadoGrabBlocks(StormObject so) {
        if (!ConfigMisc.Storm_Tornado_grabBlocks) return false;
        if (so == null || so.manager == null) return false;
        int dim = so.manager.dim;
        return dim == DIM_DUNA || dim == DIM_EVE || dim == DIM_LAYTHE || dim == DIM_TEKTO;
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
}
