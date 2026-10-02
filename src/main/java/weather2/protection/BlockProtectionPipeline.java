package weather2.protection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import weather2.climate.ClimateEngine;
import weather2.config.ConfigMisc;
import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.protection.policy.HardnessThresholdPolicy;
import weather2.protection.policy.HbmOntologyPolicy;
import weather2.protection.policy.InstrumentsProtectionPolicy;
import weather2.protection.policy.PreflightBudgetPolicy;
import weather2.protection.policy.TerrainProtectionPolicy;
import weather2.protection.policy.TileEntityProtectionPolicy;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Deep Module: Feature-Based Block Protection & Entity Budgeting Pipeline.
 * Coordinates CompositeGrabPolicy decision chains and enforces the moving blocks entity budget.
 */
public class BlockProtectionPipeline {

    public static final int DEFAULT_MAX_MOVING_BLOCKS_PER_DIM = 200;

    private static Field configMaxBlocksPerDimensionField = null;
    private static boolean configMaxBlocksFieldInitialized = false;

    private static final CompositeGrabPolicy DEFAULT_POLICY = new CompositeGrabPolicy()
        .addPolicy(new PreflightBudgetPolicy())
        .addPolicy(new InstrumentsProtectionPolicy())
        .addPolicy(new TileEntityProtectionPolicy())
        .addPolicy(new HbmOntologyPolicy())
        .addPolicy(new TerrainProtectionPolicy())
        .addPolicy(new HardnessThresholdPolicy());

    private static final CompositeGrabPolicy STATIC_PROTECTION_POLICY = new CompositeGrabPolicy()
        .addPolicy(new InstrumentsProtectionPolicy())
        .addPolicy(new TileEntityProtectionPolicy())
        .addPolicy(new HbmOntologyPolicy())
        .addPolicy(new TerrainProtectionPolicy());

    public static CompositeGrabPolicy getDefaultPolicy() {
        return DEFAULT_POLICY;
    }

    public static CompositeGrabPolicy getStaticProtectionPolicy() {
        return STATIC_PROTECTION_POLICY;
    }

    public static int getMaxMovingBlocksPerDimension() {
        if (!configMaxBlocksFieldInitialized) {
            configMaxBlocksFieldInitialized = true;
            try {
                configMaxBlocksPerDimensionField = ConfigMisc.class.getField("Storm_Tornado_maxBlocksPerDimension");
            } catch (Throwable ignored) {}
        }
        if (configMaxBlocksPerDimensionField != null) {
            try {
                int val = configMaxBlocksPerDimensionField.getInt(null);
                if (val > 0) return val;
            } catch (Throwable ignored) {}
        }
        return DEFAULT_MAX_MOVING_BLOCKS_PER_DIM;
    }

    private static final java.util.concurrent.ConcurrentHashMap<Integer, java.util.concurrent.atomic.AtomicInteger> movingBlocksPerDim = new java.util.concurrent.ConcurrentHashMap<Integer, java.util.concurrent.atomic.AtomicInteger>();

    public static int getMovingBlocksCount(int dim) {
        java.util.concurrent.atomic.AtomicInteger counter = movingBlocksPerDim.get(dim);
        return counter != null ? Math.max(0, counter.get()) : 0;
    }

    public static void incrementMovingBlocks(int dim) {
        java.util.concurrent.atomic.AtomicInteger counter = movingBlocksPerDim.get(dim);
        if (counter == null) {
            counter = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger prev = movingBlocksPerDim.putIfAbsent(dim, counter);
            if (prev != null) counter = prev;
        }
        counter.incrementAndGet();
    }

    public static void decrementMovingBlocks(int dim) {
        java.util.concurrent.atomic.AtomicInteger counter = movingBlocksPerDim.get(dim);
        if (counter != null) {
            int val = counter.decrementAndGet();
            if (val < 0) {
                counter.set(0);
            }
        }
    }

    public static void resetMovingBlocks(int dim) {
        java.util.concurrent.atomic.AtomicInteger counter = movingBlocksPerDim.get(dim);
        if (counter != null) {
            counter.set(0);
        }
    }

    public static void reconcileMovingBlocks(int dim, List storms) {
        if (storms == null) {
            resetMovingBlocks(dim);
            return;
        }
        int total = 0;
        for (int i = 0; i < storms.size(); i++) {
            Object obj = storms.get(i);
            if (obj instanceof StormObject) {
                StormObject s = (StormObject) obj;
                if (s.tornadoHelper != null) {
                    total += Math.max(0, s.tornadoHelper.blockCount);
                }
            }
        }
        java.util.concurrent.atomic.AtomicInteger counter = movingBlocksPerDim.get(dim);
        if (counter == null) {
            movingBlocksPerDim.put(dim, new java.util.concurrent.atomic.AtomicInteger(total));
        } else {
            counter.set(total);
        }
    }

    /**
     * Determines whether a storm is permitted to grab and rip blocks in its current dimension.
     * O(1) performance check: dimension whitelist + moving blocks atomic budget.
     */
    public static boolean canTornadoGrabBlocks(StormObject so) {
        if (!ConfigMisc.Storm_Tornado_grabBlocks) return false;
        if (so == null || so.manager == null) return false;
        int dim = so.manager.dim;
        if (!ClimateEngine.isDimensionGrabAllowed(dim)) {
            return false;
        }

        int currentBlocks = getMovingBlocksCount(dim);
        int maxBlocks = getMaxMovingBlocksPerDimension();
        if (currentBlocks >= maxBlocks) {
            return false;
        }

        return true;
    }

    private static Object fmlBlockRegistry = null;
    private static Method getNameForObjectMethod = null;
    private static boolean registryReflectionFailed = false;

    public static String getBlockRegistryName(Object blockObj) {
        if (blockObj == null || registryReflectionFailed) return null;
        try {
            if (fmlBlockRegistry == null) {
                Class<?> gd = Class.forName("cpw.mods.fml.common.registry.GameData");
                Method getRegistry = gd.getMethod("getBlockRegistry");
                fmlBlockRegistry = getRegistry.invoke(null);
                getNameForObjectMethod = fmlBlockRegistry.getClass()
                    .getMethod("getNameForObject", Object.class);
            }
            return (String) getNameForObjectMethod.invoke(fmlBlockRegistry, blockObj);
        } catch (Throwable t) {
            registryReflectionFailed = true;
            return null;
        }
    }

    public static boolean isHbmBlockWhitelisted(Object blockObj) {
        return HbmProtectionAdapter.isHbmBlockWhitelisted(blockObj);
    }

    /**
     * Context-aware evaluation of whether a block can be grabbed by a storm.
     */
    public static boolean canGrab(BlockContext ctx) {
        return DEFAULT_POLICY.canGrab(ctx);
    }

    public static boolean canGrab(World world, int x, int y, int z, Block block, StormObject storm) {
        BlockContext ctx = BlockContext.getPooled(world, x, y, z, block, storm);
        return DEFAULT_POLICY.canGrab(ctx);
    }

    public static boolean canGrab(World world, Block block) {
        return canGrab(world, 0, 0, 0, block, null);
    }

    /**
     * Replaces WeatherUtil.safetyCheck() with a feature-based ontology check.
     * Returns true if the block is PROTECTED (IMMUNE) and MUST NOT be ripped by tornadoes.
     */
    public static boolean isBlockProtected(Object blockObj) {
        if (blockObj == null) return true;
        BlockContext ctx = BlockContext.of(blockObj);
        GrabDecision decision = STATIC_PROTECTION_POLICY.evaluate(ctx);
        if (decision == GrabDecision.DENY) return true;
        if (decision == GrabDecision.ALLOW) return false;
        return false;
    }
}
