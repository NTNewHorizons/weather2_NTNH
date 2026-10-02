package weather2.integration;

import cpw.mods.fml.common.Loader;

/**
 * NTNH Deep Module: Central Integration Manager.
 * Provides centralized mod detection, soft-dependency discovery, and mocking handles for unit tests.
 */
public class IntegrationManager {

    public static final String MODID_HBM = "hbm";
    public static final String MODID_COFH = "CoFHCore";
    public static final String MODID_DSURROUND = "dsurround";
    public static final String MODID_MINETWEAKER = "MineTweaker3";

    private static Boolean hbmLoaded = null;
    private static Boolean cofhLoaded = null;
    private static Boolean dsurroundLoaded = null;
    private static Boolean minetweakerLoaded = null;

    public static boolean isModLoaded(String modid) {
        if (modid == null) return false;
        try {
            return Loader.isModLoaded(modid);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isHbmLoaded() {
        if (hbmLoaded == null) {
            hbmLoaded = isModLoaded(MODID_HBM);
        }
        return hbmLoaded;
    }

    public static boolean isCoFHCoreLoaded() {
        if (cofhLoaded == null) {
            cofhLoaded = isModLoaded(MODID_COFH);
        }
        return cofhLoaded;
    }

    public static boolean isDSurroundLoaded() {
        if (dsurroundLoaded == null) {
            dsurroundLoaded = isModLoaded(MODID_DSURROUND);
        }
        return dsurroundLoaded;
    }

    public static boolean isMineTweakerLoaded() {
        if (minetweakerLoaded == null) {
            minetweakerLoaded = isModLoaded(MODID_MINETWEAKER);
        }
        return minetweakerLoaded;
    }

    // Testing and injection hooks
    public static void setHbmLoaded(Boolean loaded) {
        hbmLoaded = loaded;
    }

    public static void setCoFHCoreLoaded(Boolean loaded) {
        cofhLoaded = loaded;
    }

    public static void setDSurroundLoaded(Boolean loaded) {
        dsurroundLoaded = loaded;
    }

    public static void setMineTweakerLoaded(Boolean loaded) {
        minetweakerLoaded = loaded;
    }

    public static void resetCache() {
        hbmLoaded = null;
        cofhLoaded = null;
        dsurroundLoaded = null;
        minetweakerLoaded = null;
    }
}
