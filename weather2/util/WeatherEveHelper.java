package weather2.util;

import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.WeatherManagerBase;
import weather2.config.ConfigMisc;

/**
 * NTNH helper for Eve (Dimension 18) hardcore climate profile
 * and planetary tornado destruction management.
 */
public class WeatherEveHelper {
    public static boolean isEve(StormObject so) {
        return so != null && so.manager != null && so.manager.dim == 18;
    }
    
    public static void onInitRealStorm(StormObject so) {
        if (isEve(so)) {
            so.alwaysProgresses = true;
            so.maxIntensityStage = StormObject.STATE_STAGE5;
            if (so.levelCurIntensityStage < StormObject.STATE_HIGHWIND) {
                so.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
            }
        }
    }
    
    public static int getAdjustedLightningOdds(int baseOdds, StormObject so) {
        if (isEve(so)) {
            // 6x more frequent lightning on Eve, with safe clamp to avoid network packet floods
            return Math.max(15, baseOdds / 6);
        }
        return Math.max(1, baseOdds);
    }
    
    public static int getDeadlyTimeBetween(int defaultTicks, StormObject so) {
        if (isEve(so)) {
            // 40 seconds (800 ticks) cooldown on Eve instead of 2 minutes (2400 ticks)
            return Math.min(defaultTicks, 800);
        }
        return defaultTicks;
    }
    
    public static int getLandStormOdds(int defaultOdds, StormObject so) {
        if (isEve(so)) {
            // 3x faster spontaneous land storm formation on Eve
            return Math.max(5, defaultOdds / 3);
        }
        return defaultOdds;
    }
    
    public static int getOceanStormOdds(int defaultOdds, StormObject so) {
        if (isEve(so)) {
            // 3x faster ocean cyclones on Eve
            return Math.max(5, defaultOdds / 3);
        }
        return defaultOdds;
    }

    /**
     * Determines whether a tornado can rip/grab blocks.
     * Blocks are destructible ONLY on celestial planets (Duna, Eve, Laythe, Tekto).
     * Overworld (dim 0) is unconditionally protected and immune.
     */
    public static boolean canTornadoGrabBlocks(StormObject so) {
        if (!ConfigMisc.Storm_Tornado_grabBlocks) return false;
        if (so == null || so.manager == null) return false;
        // Overworld (Earth, dim 0) is strictly immune
        if (so.manager.dim == 0) return false;
        return true;
    }
}
