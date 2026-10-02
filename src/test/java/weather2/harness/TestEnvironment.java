package weather2.harness;

import weather2.climate.ClimateProfileRegistry;
import weather2.compat.WeatherNTNHHooks;
import weather2.integration.IntegrationManager;
import weather2.integration.hbm.HbmSpaceAdapter;

/**
 * Headless test environment controller for Weather2 in NTNH.
 * Allows simulating vanilla, complete NTNH modpack, or degraded/failing states in tests.
 */
public final class TestEnvironment {

    static {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
    }

    private TestEnvironment() {}

    /**
     * Resets all internal caches, registries, and pipelines to clean defaults.
     */
    public static void resetAll() {
        MockBlockFactory.initVanillaBlocks();
        IntegrationManager.resetCache();
        ClimateProfileRegistry.reset();
        HbmSpaceAdapter.resetForTesting();

        int[] commonDims = { 0, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 413025 };
        for (int dim : commonDims) {
            WeatherNTNHHooks.resetMovingBlocks(dim);
        }
    }

    /**
     * Simulates pure Vanilla / standalone environment where HBM, CoFH, and DSurround are absent.
     */
    public static void setupVanilla() {
        resetAll();
        IntegrationManager.setHbmLoaded(false);
        IntegrationManager.setCoFHCoreLoaded(false);
        IntegrationManager.setDSurroundLoaded(false);
    }

    /**
     * Simulates nominal NTNH environment with HBM NTM, CoFH Core, and Dynamic Surroundings.
     */
    public static void setupNTNH() {
        resetAll();
        IntegrationManager.setHbmLoaded(true);
        IntegrationManager.setCoFHCoreLoaded(true);
        IntegrationManager.setDSurroundLoaded(true);
    }
}
