package weather2.compat;

import weather2.climate.ClimateEngine;
import weather2.deflector.DeflectorPowerEngine;
import weather2.integration.DSurroundBridge;
import weather2.protection.BlockProtectionPipeline;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Architectural Facade (Unified Seam).
 * Provides clean static entry points for all ASM bytecode patches and game engine hooks.
 * Internally delegates all operations to deep domain modules:
 * - ClimateEngine
 * - BlockProtectionPipeline
 * - DeflectorPowerEngine
 * - DSurroundBridge
 */
public class WeatherNTNHHooks {

    // ----------------------------------------------------
    // Climate & Planetary Hooks
    // ----------------------------------------------------
    public static int resolveDimensionId(String token) {
        return ClimateEngine.resolveDimensionId(token);
    }

    public static boolean isAtmosphericWorld(Object worldObj) {
        return ClimateEngine.isAtmosphericWorld(worldObj);
    }

    public static void ensureProfilesLoaded() {
        ClimateEngine.ensureProfilesLoaded();
    }

    public static ClimateEngine.ClimateProfile getProfile(int dim) {
        return ClimateEngine.getProfile(dim);
    }

    public static ClimateEngine.ClimateProfile getProfile(StormObject so) {
        return ClimateEngine.getProfile(so);
    }

    public static boolean isEve(StormObject so) {
        return ClimateEngine.isEve(so);
    }

    public static boolean isTekto(StormObject so) {
        return ClimateEngine.isTekto(so);
    }

    public static void onInitRealStorm(StormObject so) {
        ClimateEngine.onInitRealStorm(so);
    }

    public static int getAdjustedLightningOdds(int baseOdds, StormObject so) {
        return ClimateEngine.getAdjustedLightningOdds(baseOdds, so);
    }

    public static int getDeadlyTimeBetween(int defaultTicks, StormObject so) {
        return ClimateEngine.getDeadlyTimeBetween(defaultTicks, so);
    }

    public static int getLandStormOdds(int defaultOdds, StormObject so) {
        return ClimateEngine.getLandStormOdds(defaultOdds, so);
    }

    public static int getOceanStormOdds(int defaultOdds, StormObject so) {
        return ClimateEngine.getOceanStormOdds(defaultOdds, so);
    }

    public static boolean isDimensionGrabAllowed(int dim) {
        return ClimateEngine.isDimensionGrabAllowed(dim);
    }

    public static boolean shouldSpawnFunnel(StormObject so) {
        return ClimateEngine.shouldSpawnFunnel(so);
    }

    // ----------------------------------------------------
    // Block Protection & Budgeting Hooks
    // ----------------------------------------------------
    public static int getMaxMovingBlocksPerDimension() {
        return BlockProtectionPipeline.getMaxMovingBlocksPerDimension();
    }

    public static boolean canTornadoGrabBlocks(StormObject so) {
        return BlockProtectionPipeline.canTornadoGrabBlocks(so);
    }

    public static int getMovingBlocksCount(int dim) {
        return BlockProtectionPipeline.getMovingBlocksCount(dim);
    }

    public static void incrementMovingBlocks(int dim) {
        BlockProtectionPipeline.incrementMovingBlocks(dim);
    }

    public static void decrementMovingBlocks(int dim) {
        BlockProtectionPipeline.decrementMovingBlocks(dim);
    }

    public static void resetMovingBlocks(int dim) {
        BlockProtectionPipeline.resetMovingBlocks(dim);
    }

    public static void reconcileMovingBlocks(int dim, java.util.List storms) {
        BlockProtectionPipeline.reconcileMovingBlocks(dim, storms);
    }

    public static boolean isBlockProtected(Object blockObj) {
        return BlockProtectionPipeline.isBlockProtected(blockObj);
    }

    public static String getBlockRegistryName(Object blockObj) {
        return BlockProtectionPipeline.getBlockRegistryName(blockObj);
    }

    public static boolean isHbmBlockWhitelisted(Object blockObj) {
        return BlockProtectionPipeline.isHbmBlockWhitelisted(blockObj);
    }

    // ----------------------------------------------------
    // Deflector & Energy Hooks
    // ----------------------------------------------------
    public static long getDeflectorDissipationCost(int stage) {
        return DeflectorPowerEngine.getDeflectorDissipationCost(stage);
    }

    public static boolean tickDeflectorPower(Object deflectorObj) {
        return DeflectorPowerEngine.tickDeflectorPower(deflectorObj);
    }

    public static boolean processDeflectorStorm(Object deflectorObj, Object wmObj, Object stormObj) {
        return DeflectorPowerEngine.processDeflectorStorm(deflectorObj, wmObj, stormObj);
    }

    // ----------------------------------------------------
    // Audio, Command & Dynamic Surroundings Hooks
    // ----------------------------------------------------
    public static void setWorldRainStrength(Object world, float strength) {
        DSurroundBridge.setWorldRainStrength(world, strength);
    }

    public static void updateDSurroundIntensity(float strength) {
        DSurroundBridge.updateDSurroundIntensity(strength);
    }

    public static void onPrecipitationTick(float curPrecipStr) {
        DSurroundBridge.onPrecipitationTick(curPrecipStr);
    }

    public static void resetPrecipitation() {
        DSurroundBridge.resetPrecipitation();
    }

    public static float getAdjustedThunderVolume(float baseVol) {
        return DSurroundBridge.getAdjustedThunderVolume(baseVol);
    }

    public static float getAdjustedWindVolume(float baseVol) {
        return DSurroundBridge.getAdjustedWindVolume(baseVol);
    }

    public static String[] normalizeCommandArgs(String[] args) {
        return DSurroundBridge.normalizeCommandArgs(args);
    }
}
