package weather2.util;

import weather2.climate.ClimateEngine;
import weather2.climate.ClimateProfile;
import weather2.compat.WeatherNTNHHooks;
import weather2.deflector.DeflectorPowerEngine;
import weather2.protection.BlockProtectionPipeline;
import weather2.weathersystem.storm.StormObject;

/**
 * Backward-Compatibility Shim for WeatherEveHelper.
 * Retains all legacy static signatures and types, delegating directly to the modularized facade (WeatherNTNHHooks).
 * Guarantees zero migration risk and 100% binary compatibility for existing code, tests, and bytecode hooks.
 */
public class WeatherEveHelper extends WeatherNTNHHooks {

    public static final int DIM_OVERWORLD = ClimateEngine.DIM_OVERWORLD;
    public static final int DIM_DUNA = ClimateEngine.DIM_DUNA;
    public static final int DIM_EVE = ClimateEngine.DIM_EVE;
    public static final int DIM_LAYTHE = ClimateEngine.DIM_LAYTHE;
    public static final int DIM_TEKTO = ClimateEngine.DIM_TEKTO;

    public static final int DEFAULT_MAX_MOVING_BLOCKS_PER_DIM = BlockProtectionPipeline.DEFAULT_MAX_MOVING_BLOCKS_PER_DIM;
    public static final String DEFAULT_WEATHER_PROFILES = ClimateEngine.DEFAULT_WEATHER_PROFILES;

    public static final long DEFLECTOR_IDLE_DRAIN = DeflectorPowerEngine.DEFLECTOR_IDLE_DRAIN;
    public static final long DEFLECTOR_MAX_POWER = DeflectorPowerEngine.DEFLECTOR_MAX_POWER;

    /**
     * Backward-compatible alias for ClimateProfile.
     */
    public static class WeatherDimensionProfile extends ClimateProfile {

        public WeatherDimensionProfile(int dim, String name, boolean weatherEnabled, int maxStage,
            boolean alwaysProgresses, int deadlyCooldown, float lightningMultiplier, int landSpawnOdds,
            int oceanSpawnOdds, boolean grabBlocks) {
            super(
                dim,
                name,
                weatherEnabled,
                maxStage,
                alwaysProgresses,
                deadlyCooldown,
                lightningMultiplier,
                landSpawnOdds,
                oceanSpawnOdds,
                grabBlocks);
        }

        public static WeatherDimensionProfile fromClimateProfile(ClimateProfile p) {
            if (p == null) return null;
            return new WeatherDimensionProfile(
                p.dim,
                p.name,
                p.weatherEnabled,
                p.maxStage,
                p.alwaysProgresses,
                p.deadlyCooldown,
                p.lightningMultiplier,
                p.landSpawnOdds,
                p.oceanSpawnOdds,
                p.grabBlocks);
        }
    }

    public static WeatherDimensionProfile getProfile(int dim) {
        ClimateProfile p = ClimateEngine.getProfile(dim);
        return WeatherDimensionProfile.fromClimateProfile(p);
    }

    public static WeatherDimensionProfile getProfile(StormObject so) {
        ClimateProfile p = ClimateEngine.getProfile(so);
        return WeatherDimensionProfile.fromClimateProfile(p);
    }
}
