package weather2.climate;

import java.lang.reflect.Field;

import weather2.config.ConfigMisc;
import weather2.integration.hbm.HbmSpaceAdapter;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Deep Module: Planetary Climate Engine & Dimension Space Resolution.
 * Handles declarative per-dimension weather profiles, dynamic ID resolution from NTM SpaceConfig,
 * vacuum world auto-detection, and storm progression lifecycle parameters.
 */
public class ClimateEngine {

    public static final int DIM_OVERWORLD = 0;
    public static final int DIM_DUNA = 16;
    public static final int DIM_EVE = 18;
    public static final int DIM_LAYTHE = 22;
    public static final int DIM_TEKTO = 24;

    public static final String DEFAULT_WEATHER_PROFILES = "0, 5, false, 3600, 1.0, 25, 0, true; "
        + "duna, 9, false, 1800, 1.0, 10, 0, true; "
        + "eve, 9, true, 800, 6.0, 3, 7, true; "
        + "laythe, 9, false, 1800, 1.0, 10, 20, true; "
        + "tekto, 8, false, 1800, 2.0, 10, 0, true";

    /**
     * Resolves dimension IDs dynamically from NTM SpaceConfig or safe fallback constants.
     */
    public static int resolveDimensionId(String token) {
        if (token == null) return -999;
        token = token.trim()
            .toLowerCase();
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ignored) {}

        // Dynamic NTM SpaceConfig lookup via HbmSpaceAdapter
        Integer spaceDim = HbmSpaceAdapter.resolveSpaceDimension(token);
        if (spaceDim != null) return spaceDim.intValue();

        // Safe NTNH fallbacks if HBM is not loaded or reflection fails
        if (token.equals("overworld")) return 0;
        if (token.equals("nether")) return -1;
        if (token.equals("duna")) return DIM_DUNA;
        if (token.equals("eve")) return DIM_EVE;
        if (token.equals("laythe")) return DIM_LAYTHE;
        if (token.equals("tekto")) return DIM_TEKTO;
        if (token.equals("moon") || token.equals("mun")) return 15;
        if (token.equals("ike")) return 17;
        if (token.equals("dres")) return 19;
        if (token.equals("moho")) return 20;
        if (token.equals("minmus")) return 21;
        if (token.equals("orbit")) return 23;
        if (token.equals("thatmo")) return 413025;

        return -999;
    }

    /**
     * Checks if a world has an atmosphere based on NTM's WorldProviderCelestial.hasWeatherCycle().
     * On vacuum worlds (Moon, Minmus, Ike, Dres, Moho, Thatmo), returns false.
     */
    public static boolean isAtmosphericWorld(Object worldObj) {
        return HbmSpaceAdapter.isAtmosphericWorld(worldObj);
    }

    public static synchronized void ensureProfilesLoaded() {
        String cfg = null;
        try {
            Field f = ConfigMisc.class.getField("Dimension_Weather_Profiles");
            cfg = (String) f.get(null);
        } catch (Throwable ignored) {}
        ClimateProfileRegistry.loadFromConfig(cfg);
    }

    public static ClimateProfile getProfile(int dim) {
        ensureProfilesLoaded();
        return ClimateProfileRegistry.get(dim);
    }

    public static ClimateProfile getProfile(StormObject so) {
        if (so == null || so.manager == null) return ClimateProfileRegistry.get(0);
        return getProfile(so.manager.dim);
    }

    public static boolean isEve(StormObject so) {
        if (so == null || so.manager == null) return false;
        return so.manager.dim == resolveDimensionId("eve");
    }

    public static boolean isTekto(StormObject so) {
        if (so == null || so.manager == null) return false;
        return so.manager.dim == resolveDimensionId("tekto");
    }

    public static void onInitRealStorm(StormObject so) {
        if (so == null) return;
        ClimateProfile p = getProfile(so);
        if (p.alwaysProgresses) {
            so.alwaysProgresses = true;
            if (so.levelCurIntensityStage < StormObject.STATE_HIGHWIND) {
                so.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
            }
        }
        if (p.maxStage >= 0 && so.maxIntensityStage > p.maxStage) {
            so.maxIntensityStage = p.maxStage;
        }
    }

    public static int getAdjustedLightningOdds(int baseOdds, StormObject so) {
        ClimateProfile p = getProfile(so);
        if (p.lightningMultiplier > 1.0F) {
            int adjusted = (int) (baseOdds / p.lightningMultiplier);
            return Math.max(15, adjusted);
        }
        return Math.max(1, baseOdds);
    }

    public static int getDeadlyTimeBetween(int defaultTicks, StormObject so) {
        ClimateProfile p = getProfile(so);
        if (p.deadlyCooldown == 0) {
            return Integer.MAX_VALUE; // disabled
        }
        if (p.deadlyCooldown > 0) {
            return p.deadlyCooldown;
        }
        return defaultTicks;
    }

    public static int getLandStormOdds(int defaultOdds, StormObject so) {
        ClimateProfile p = getProfile(so);
        if (p.landSpawnOdds == 0) {
            return Integer.MAX_VALUE; // disabled
        }
        if (p.landSpawnOdds > 0) {
            return p.landSpawnOdds;
        }
        return defaultOdds;
    }

    public static int getOceanStormOdds(int defaultOdds, StormObject so) {
        ClimateProfile p = getProfile(so);
        if (p.oceanSpawnOdds == 0) {
            return Integer.MAX_VALUE; // disabled
        }
        if (p.oceanSpawnOdds > 0) {
            return p.oceanSpawnOdds;
        }
        return defaultOdds;
    }

    public static boolean isDimensionGrabAllowed(int dim) {
        ensureProfilesLoaded();
        return ClimateProfileRegistry.isGrabAllowed(dim);
    }

    public static boolean shouldSpawnFunnel(StormObject so) {
        if (so == null) return false;
        return so.isTornadoFormingOrGreater() || so.isCycloneFormingOrGreater() || so.attrib_waterSpout;
    }
}
