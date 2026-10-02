package weather2.climate;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NTNH Typed Runtime Registry for Climate Profiles.
 * Provides thread-safe, zero-allocation O(1) profile resolution in hot-path simulation.
 * Supports layered registration:
 * 1. Hardcoded canonical defaults (Earth, Duna, Eve, Laythe, Tekto, Vacuum bodies).
 * 2. Configuration overrides (Dimension_Weather_Profiles in Misc.cfg).
 * 3. Runtime API overrides.
 */
public class ClimateProfileRegistry {

    private static final ClimateProfile[] fastProfiles = new ClimateProfile[256];
    private static final ConcurrentHashMap<Integer, ClimateProfile> extendedProfiles = new ConcurrentHashMap<Integer, ClimateProfile>();
    private static final ConcurrentHashMap<String, ClimateProfile> namedProfiles = new ConcurrentHashMap<String, ClimateProfile>();
    private static final Set<Integer> allowedGrabDims = Collections
        .newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    private static volatile boolean initialized = false;
    private static String lastLoadedConfig = null;

    /**
     * Initializes the registry with canonical NTNH defaults if not yet initialized.
     */
    public static synchronized void ensureInitialized() {
        if (!initialized) {
            registerDefaults();
            initialized = true;
        }
    }

    /**
     * Registers canonical built-in defaults for known NTNH dimensions.
     */
    public static synchronized void registerDefaults() {
        // Overworld (Earth, Dim 0): Localized early threat (F1 max, selective fragile grab only)
        register(
            ClimateProfile.builder("overworld")
                .dim(ClimateEngine.DIM_OVERWORLD)
                .weatherEnabled(true)
                .exactStage(5) // F1 max (STATE_STAGE1)
                .alwaysProgresses(false)
                .deadlyCooldown(3600)
                .lightningMultiplier(1.0F)
                .spawnOdds(25, 0)
                .grabBlocks(true)
                .build());

        // Duna (Mars-analog, Dim 16): Polar CO2 blizzards & equatorial dust devils
        register(
            ClimateProfile.builder("duna")
                .dim(ClimateEngine.DIM_DUNA)
                .weatherEnabled(true)
                .exactStage(9) // F5
                .alwaysProgresses(false)
                .deadlyCooldown(1800)
                .lightningMultiplier(1.0F)
                .spawnOdds(10, 0)
                .grabBlocks(true)
                .build());

        // Eve (Venus-analog, Dim 18): Supercell hell, instant F5/C5, 6x lightning, 40s cooldown
        register(
            ClimateProfile.builder("eve")
                .dim(ClimateEngine.DIM_EVE)
                .weatherEnabled(true)
                .exactStage(9) // F5/C5
                .alwaysProgresses(true)
                .deadlyCooldown(800)
                .lightningMultiplier(6.0F)
                .spawnOdds(3, 7)
                .grabBlocks(true)
                .build());

        // Laythe (Ocean moon, Dim 22): Severe ocean cyclones (C0..C5) & waterspouts
        register(
            ClimateProfile.builder("laythe")
                .dim(ClimateEngine.DIM_LAYTHE)
                .weatherEnabled(true)
                .exactStage(9) // C5/F5
                .alwaysProgresses(false)
                .deadlyCooldown(1800)
                .lightningMultiplier(1.0F)
                .spawnOdds(10, 20)
                .grabBlocks(true)
                .build());

        // Tekto (Titan-analog, Dim 24): Hydrocarbon storms capped at F4
        register(
            ClimateProfile.builder("tekto")
                .dim(ClimateEngine.DIM_TEKTO)
                .weatherEnabled(true)
                .exactStage(8) // F4 cap
                .alwaysProgresses(false)
                .deadlyCooldown(1800)
                .lightningMultiplier(2.0F)
                .spawnOdds(10, 0)
                .grabBlocks(true)
                .build());

        // Vacuum bodies: Moon (15), Ike (17), Dres (19), Moho (20), Minmus (21), Orbit (23), Thatmo (413025), Nether
        // (-1)
        registerVacuumBody("moon", 15);
        registerVacuumBody("ike", 17);
        registerVacuumBody("dres", 19);
        registerVacuumBody("moho", 20);
        registerVacuumBody("minmus", 21);
        registerVacuumBody("orbit", 23);
        registerVacuumBody("thatmo", 413025);
        registerVacuumBody("nether", -1);
    }

    private static void registerVacuumBody(String name, int fallbackDim) {
        int dim = ClimateEngine.resolveDimensionId(name);
        if (dim == -999) dim = fallbackDim;
        register(
            ClimateProfile.builder(dim, name)
                .weatherEnabled(false)
                .exactStage(0)
                .alwaysProgresses(false)
                .deadlyCooldown(0)
                .lightningMultiplier(1.0F)
                .spawnOdds(0, 0)
                .grabBlocks(false)
                .build());
    }

    /**
     * Registers or overrides a climate profile in the runtime registry.
     * O(1) thread-safe registration.
     */
    public static void register(ClimateProfile profile) {
        if (profile == null) return;
        int dim = profile.dim;

        // Resolve dim if needed
        if (dim == -999 && profile.name != null) {
            dim = ClimateEngine.resolveDimensionId(profile.name);
        }

        if (dim >= 0 && dim < 256) {
            fastProfiles[dim] = profile;
        } else {
            extendedProfiles.put(dim, profile);
        }

        if (profile.name != null && !profile.name.isEmpty()) {
            namedProfiles.put(profile.name.toLowerCase(), profile);
        }

        if (profile.grabBlocks) {
            allowedGrabDims.add(dim);
        } else {
            allowedGrabDims.remove(dim);
        }
    }

    /**
     * Retrieves the climate profile for a dimension.
     * Guaranteed O(1) without memory allocations in hot path.
     */
    public static ClimateProfile get(int dim) {
        ensureInitialized();
        if (dim >= 0 && dim < 256) {
            ClimateProfile p = fastProfiles[dim];
            if (p != null) return p;
        }
        ClimateProfile p = extendedProfiles.get(dim);
        if (p != null) return p;
        return ClimateProfile.createDefault(dim);
    }

    /**
     * Retrieves profile by symbolic planet name (e.g. "eve", "duna").
     */
    public static ClimateProfile get(String name) {
        ensureInitialized();
        if (name == null) return null;
        return namedProfiles.get(
            name.trim()
                .toLowerCase());
    }

    public static boolean has(int dim) {
        ensureInitialized();
        if (dim >= 0 && dim < 256) {
            return fastProfiles[dim] != null;
        }
        return extendedProfiles.containsKey(dim);
    }

    public static boolean isGrabAllowed(int dim) {
        ensureInitialized();
        return allowedGrabDims.contains(dim);
    }

    public static Collection<ClimateProfile> getAll() {
        ensureInitialized();
        java.util.List<ClimateProfile> list = new java.util.ArrayList<ClimateProfile>();
        for (int i = 0; i < 256; i++) {
            if (fastProfiles[i] != null) list.add(fastProfiles[i]);
        }
        list.addAll(extendedProfiles.values());
        return Collections.unmodifiableCollection(list);
    }

    /**
     * Parses and applies configuration overrides from Misc.cfg string.
     * Retains 100% backward compatibility with legacy 8-tuple and 6-tuple formats.
     */
    public static synchronized void loadFromConfig(String cfg) {
        ensureInitialized();
        if (cfg == null || cfg.trim()
            .isEmpty()) {
            cfg = ClimateEngine.DEFAULT_WEATHER_PROFILES;
        }
        if (cfg.equals(lastLoadedConfig)) {
            return;
        }

        String[] lines = cfg.split("[;\\r\\n]+");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",");
            if (parts.length < 6) continue;

            String dimToken = parts[0].trim();
            int dim = ClimateEngine.resolveDimensionId(dimToken);
            if (dim == -999) {
                ClimateProfile existing = namedProfiles.get(dimToken.toLowerCase());
                if (existing != null) {
                    dim = existing.dim;
                }
            }
            if (dim == -999) continue;

            int maxStage = 0;
            try {
                maxStage = Integer.parseInt(parts[1].trim());
            } catch (Throwable ignored) {}

            boolean alwaysProgresses = false;
            int deadlyCooldown = 1800;
            float lightningMul = 1.0F;
            int landSpawnOdds = 10;
            int oceanSpawnOdds = 0;
            boolean grabBlocks = false;

            if (parts.length >= 8) {
                try {
                    alwaysProgresses = Boolean.parseBoolean(parts[2].trim());
                } catch (Throwable ignored) {}
                try {
                    deadlyCooldown = Integer.parseInt(parts[3].trim());
                } catch (Throwable ignored) {}
                try {
                    lightningMul = Float.parseFloat(parts[4].trim());
                } catch (Throwable ignored) {}
                try {
                    landSpawnOdds = Integer.parseInt(parts[5].trim());
                } catch (Throwable ignored) {}
                try {
                    oceanSpawnOdds = Integer.parseInt(parts[6].trim());
                } catch (Throwable ignored) {}
                try {
                    grabBlocks = Boolean.parseBoolean(parts[7].trim());
                } catch (Throwable ignored) {}
            } else if (parts.length >= 6) {
                try {
                    deadlyCooldown = Integer.parseInt(parts[2].trim());
                } catch (Throwable ignored) {}
                try {
                    lightningMul = Float.parseFloat(parts[3].trim());
                } catch (Throwable ignored) {}
                try {
                    landSpawnOdds = Integer.parseInt(parts[4].trim());
                } catch (Throwable ignored) {}
                try {
                    grabBlocks = Boolean.parseBoolean(parts[5].trim());
                } catch (Throwable ignored) {}
                alwaysProgresses = (maxStage >= 9 && deadlyCooldown <= 1000);
                if (dimToken.equalsIgnoreCase("laythe") || dimToken.equalsIgnoreCase("eve")) {
                    oceanSpawnOdds = landSpawnOdds * 2;
                }
            }

            ClimateProfile.Builder b = ClimateProfile.builder(dim, dimToken)
                .weatherEnabled(true)
                .maxStage(maxStage)
                .alwaysProgresses(alwaysProgresses)
                .deadlyCooldown(deadlyCooldown)
                .lightningMultiplier(lightningMul)
                .spawnOdds(landSpawnOdds, oceanSpawnOdds)
                .grabBlocks(grabBlocks);

            // Special-case Overworld stage 0 in config
            if (maxStage == 0 && (dim == 0 || dimToken.equalsIgnoreCase("overworld") || dimToken.equals("0"))) {
                b.exactStage(5);
            }

            register(b.build());
        }

        lastLoadedConfig = cfg;
    }

    /**
     * Clears all profiles and reloads defaults.
     */
    public static synchronized void reset() {
        for (int i = 0; i < 256; i++) {
            fastProfiles[i] = null;
        }
        extendedProfiles.clear();
        namedProfiles.clear();
        allowedGrabDims.clear();
        lastLoadedConfig = null;
        initialized = false;
        ensureInitialized();
    }
}
