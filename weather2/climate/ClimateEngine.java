package weather2.climate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import weather2.config.ConfigMisc;
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

    public static class ClimateProfile {
        public final int dim;
        public final String name;
        public final boolean weatherEnabled;
        public final int maxStage;
        public final boolean alwaysProgresses;
        public final int deadlyCooldown;
        public final float lightningMultiplier;
        public final int landSpawnOdds;
        public final int oceanSpawnOdds;
        public final boolean grabBlocks;

        public ClimateProfile(int dim, String name, boolean weatherEnabled, int maxStage,
                              boolean alwaysProgresses, int deadlyCooldown,
                              float lightningMultiplier, int landSpawnOdds,
                              int oceanSpawnOdds, boolean grabBlocks) {
            this.dim = dim;
            this.name = name;
            this.weatherEnabled = weatherEnabled;
            this.maxStage = maxStage;
            this.alwaysProgresses = alwaysProgresses;
            this.deadlyCooldown = deadlyCooldown;
            this.lightningMultiplier = lightningMultiplier;
            this.landSpawnOdds = landSpawnOdds;
            this.oceanSpawnOdds = oceanSpawnOdds;
            this.grabBlocks = grabBlocks;
        }

        public static ClimateProfile createDefault(int dim) {
            return new ClimateProfile(dim, "default_" + dim, false, 0, false, 0, 1.0F, 0, 0, false);
        }

        @Override
        public String toString() {
            return String.format("Profile[%s/dim=%d, maxStage=%d, alwaysProgress=%b, cooldown=%d, lightning=%.1fx, landOdds=%d, oceanOdds=%d, grab=%b]",
                    name, dim, maxStage, alwaysProgresses, deadlyCooldown, lightningMultiplier, landSpawnOdds, oceanSpawnOdds, grabBlocks);
        }
    }

    public static final String DEFAULT_WEATHER_PROFILES =
            "0, 3, false, 0, 1.0, 0, 0, false; " +
            "duna, 9, false, 1800, 1.0, 10, 0, true; " +
            "eve, 9, true, 800, 6.0, 3, 7, true; " +
            "laythe, 9, false, 1800, 1.0, 10, 20, true; " +
            "tekto, 8, false, 1800, 2.0, 10, 0, true";

    private static String lastProfilesConfig = null;
    private static Map<Integer, ClimateProfile> profiles = new HashMap<Integer, ClimateProfile>();
    private static Set<Integer> allowedGrabDims = new HashSet<Integer>();

    /**
     * Resolves dimension IDs dynamically from NTM SpaceConfig or safe fallback constants.
     */
    public static int resolveDimensionId(String token) {
        if (token == null) return -999;
        token = token.trim().toLowerCase();
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ignored) {}

        // Reflective NTM SpaceConfig lookup
        try {
            Class<?> sc = Class.forName("com.hbm.config.SpaceConfig");
            if (token.equals("duna")) return sc.getField("dunaDimension").getInt(null);
            if (token.equals("eve")) return sc.getField("eveDimension").getInt(null);
            if (token.equals("laythe")) return sc.getField("laytheDimension").getInt(null);
            if (token.equals("tekto")) return sc.getField("tektoDimension").getInt(null);
            if (token.equals("moon") || token.equals("mun")) return sc.getField("moonDimension").getInt(null);
            if (token.equals("minmus")) return sc.getField("minmusDimension").getInt(null);
            if (token.equals("ike")) return sc.getField("ikeDimension").getInt(null);
            if (token.equals("dres")) return sc.getField("dresDimension").getInt(null);
            if (token.equals("moho")) return sc.getField("mohoDimension").getInt(null);
            if (token.equals("orbit")) return sc.getField("orbitDimension").getInt(null);
            if (token.equals("thatmo")) return sc.getField("thatmoDimension").getInt(null);
        } catch (Throwable ignored) {}

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

    private static Field worldProviderField = null;
    private static Field providerDimensionIdField = null;
    private static Class<?> celestialProviderClass = null;
    private static Method celestialHasWeatherCycleMethod = null;
    private static boolean celestialReflectionInit = false;

    private static void initCelestialReflection(Class<?> worldClass) {
        if (celestialReflectionInit) return;
        celestialReflectionInit = true;
        try {
            worldProviderField = worldClass.getField("provider");
        } catch (Throwable t) {
            try { worldProviderField = worldClass.getField("field_73011_v"); } catch (Throwable ignored) {}
        }
        try {
            celestialProviderClass = Class.forName("com.hbm.dim.WorldProviderCelestial");
            celestialHasWeatherCycleMethod = celestialProviderClass.getMethod("hasWeatherCycle");
        } catch (Throwable ignored) {}
    }

    /**
     * Checks if a world has an atmosphere based on NTM's WorldProviderCelestial.hasWeatherCycle().
     * On vacuum worlds (Moon, Minmus, Ike, Dres, Moho, Thatmo), returns false.
     */
    public static boolean isAtmosphericWorld(Object worldObj) {
        if (worldObj == null) return false;
        try {
            initCelestialReflection(worldObj.getClass());
            if (worldProviderField == null) return true;
            Object provider = worldProviderField.get(worldObj);
            if (provider == null) return false;

            if (providerDimensionIdField == null) {
                try {
                    providerDimensionIdField = provider.getClass().getField("dimensionId");
                } catch (Throwable t) {
                    try { providerDimensionIdField = provider.getClass().getField("field_76574_g"); } catch (Throwable ignored) {}
                }
            }
            if (providerDimensionIdField != null) {
                int dim = providerDimensionIdField.getInt(provider);
                if (dim == 0) return true; // Overworld always atmospheric
            }

            if (celestialProviderClass != null && celestialHasWeatherCycleMethod != null && celestialProviderClass.isInstance(provider)) {
                return ((Boolean) celestialHasWeatherCycleMethod.invoke(provider)).booleanValue();
            }
        } catch (Throwable ignored) {}
        return true;
    }

    public static synchronized void ensureProfilesLoaded() {
        String cfg = null;
        try {
            Field f = ConfigMisc.class.getField("Dimension_Weather_Profiles");
            cfg = (String) f.get(null);
        } catch (Throwable ignored) {}

        if (cfg == null || cfg.trim().isEmpty()) {
            cfg = DEFAULT_WEATHER_PROFILES;
        }

        if (cfg.equals(lastProfilesConfig)) {
            return;
        }

        Map<Integer, ClimateProfile> map = new HashMap<Integer, ClimateProfile>();
        Set<Integer> grabDims = new HashSet<Integer>();

        String[] lines = cfg.split("[;\\r\\n]+");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",");
            if (parts.length < 6) continue;

            String dimToken = parts[0].trim();
            int dim = resolveDimensionId(dimToken);
            if (dim == -999) continue;

            int maxStage = 0;
            try { maxStage = Integer.parseInt(parts[1].trim()); } catch (Throwable ignored) {}
            // Normalize Fujita 1..5 to engine stages 5..9 (STATE_STAGE1..STATE_STAGE5)
            if (maxStage > 0 && maxStage <= 5) {
                maxStage = maxStage + 4;
            } else if (maxStage == 0 && (dim == 0 || dimToken.equalsIgnoreCase("overworld") || dimToken.equals("0"))) {
                maxStage = 3; // STATE_HAIL (precipitation/thunder/wind/hail only, zero tornadoes)
            }

            boolean alwaysProgresses = false;
            int deadlyCooldown = 1800;
            float lightningMul = 1.0F;
            int landSpawnOdds = 10;
            int oceanSpawnOdds = 0;
            boolean grabBlocks = false;

            if (parts.length >= 8) {
                try { alwaysProgresses = Boolean.parseBoolean(parts[2].trim()); } catch (Throwable ignored) {}
                try { deadlyCooldown = Integer.parseInt(parts[3].trim()); } catch (Throwable ignored) {}
                try { lightningMul = Float.parseFloat(parts[4].trim()); } catch (Throwable ignored) {}
                try { landSpawnOdds = Integer.parseInt(parts[5].trim()); } catch (Throwable ignored) {}
                try { oceanSpawnOdds = Integer.parseInt(parts[6].trim()); } catch (Throwable ignored) {}
                try { grabBlocks = Boolean.parseBoolean(parts[7].trim()); } catch (Throwable ignored) {}
            } else if (parts.length >= 6) {
                try { deadlyCooldown = Integer.parseInt(parts[2].trim()); } catch (Throwable ignored) {}
                try { lightningMul = Float.parseFloat(parts[3].trim()); } catch (Throwable ignored) {}
                try { landSpawnOdds = Integer.parseInt(parts[4].trim()); } catch (Throwable ignored) {}
                try { grabBlocks = Boolean.parseBoolean(parts[5].trim()); } catch (Throwable ignored) {}
                alwaysProgresses = (maxStage >= 9 && deadlyCooldown <= 1000);
                if (dimToken.equalsIgnoreCase("laythe") || dimToken.equalsIgnoreCase("eve")) {
                    oceanSpawnOdds = landSpawnOdds * 2;
                }
            }

            ClimateProfile prof = new ClimateProfile(
                    dim, dimToken, true, maxStage, alwaysProgresses,
                    deadlyCooldown, lightningMul, landSpawnOdds, oceanSpawnOdds, grabBlocks
            );
            map.put(dim, prof);
            if (grabBlocks) {
                grabDims.add(dim);
            }
        }

        profiles = map;
        allowedGrabDims = grabDims;
        lastProfilesConfig = cfg;
    }

    public static ClimateProfile getProfile(int dim) {
        ensureProfilesLoaded();
        ClimateProfile p = profiles.get(dim);
        if (p != null) return p;
        return ClimateProfile.createDefault(dim);
    }

    public static ClimateProfile getProfile(StormObject so) {
        if (so == null || so.manager == null) return ClimateProfile.createDefault(0);
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
            int adjusted = (int)(baseOdds / p.lightningMultiplier);
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
        ClimateProfile p = getProfile(dim);
        return p.grabBlocks;
    }

    public static boolean shouldSpawnFunnel(StormObject so) {
        if (so == null) return false;
        return so.isTornadoFormingOrGreater() || so.isCycloneFormingOrGreater() || so.attrib_waterSpout;
    }
}
