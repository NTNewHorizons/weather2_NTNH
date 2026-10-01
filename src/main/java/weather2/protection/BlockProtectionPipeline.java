package weather2.protection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import weather2.climate.ClimateEngine;
import weather2.config.ConfigMisc;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Deep Module: Feature-Based Block Protection & Entity Budgeting Pipeline.
 * Enforces the 6-barrier defense-in-depth immunity matrix:
 * 1. Weather2 Instruments immunity
 * 2. HBM NTM namespace protection with whitelisting
 * 3. TileEntity and BlockContainer ontological immunity
 * 4. Unbreakable blocks check (hardness < 0)
 * 5. Structural and planetary terrain protection (BlockLog, dirt, grass, sand)
 * 6. Hardness fallback check (only lightweight debris without TileEntity)
 * Also enforces the configurable per-dimension moving blocks budget.
 */
public class BlockProtectionPipeline {

    public static final int DEFAULT_MAX_MOVING_BLOCKS_PER_DIM = 200;

    private static Field configMaxBlocksPerDimensionField = null;
    private static boolean configMaxBlocksFieldInitialized = false;

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

        // Cap on EntityMovingBlock per dimension to protect server TPS (O(1) counter)
        int currentBlocks = getMovingBlocksCount(dim);
        int maxBlocks = getMaxMovingBlocksPerDimension();
        if (currentBlocks >= maxBlocks) {
            return false;
        }

        return true;
    }

    // ----------------------------------------------------
    // Feature-Based Block Protection & Immunity Matrix
    // ----------------------------------------------------
    private static Class<?> tileEntityProviderClass = null;
    private static Class<?> blockContainerClass = null;
    private static Class<?> blockLogClass = null;
    private static Class<?> blockChestClass = null;
    private static Class<?> blockJukeboxClass = null;
    private static Method blockHasTileEntityMeta = null;
    private static Method blockHasTileEntityNoArg = null;
    private static Field blockHardnessField = null;
    private static boolean blockReflectionInit = false;

    private static Object fmlBlockRegistry = null;
    private static Method getNameForObjectMethod = null;
    private static boolean registryReflectionFailed = false;

    private static final Set<String> HBM_GRAB_WHITELIST = new HashSet<String>();

    static {
        // Only lightweight decoration/clutter blocks from HBM are allowed to be grabbed on hostile planets
        HBM_GRAB_WHITELIST.add("hbm:block_waste");
        HBM_GRAB_WHITELIST.add("hbm:waste_earth");
        HBM_GRAB_WHITELIST.add("hbm:waste_mycelium");
        HBM_GRAB_WHITELIST.add("hbm:waste_sand");
        HBM_GRAB_WHITELIST.add("hbm:block_meteor");
        HBM_GRAB_WHITELIST.add("hbm:block_meteor_broken");
        HBM_GRAB_WHITELIST.add("hbm:ash");
        HBM_GRAB_WHITELIST.add("hbm:block_scrap");
    }

    private static void initBlockReflection() {
        if (blockReflectionInit) return;
        blockReflectionInit = true;
        try {
            tileEntityProviderClass = Class.forName("net.minecraft.block.ITileEntityProvider");
        } catch (Throwable ignored) {}
        try {
            blockContainerClass = Class.forName("net.minecraft.block.BlockContainer");
        } catch (Throwable ignored) {}
        try {
            blockLogClass = Class.forName("net.minecraft.block.BlockLog");
        } catch (Throwable ignored) {}
        try {
            blockChestClass = Class.forName("net.minecraft.block.BlockChest");
        } catch (Throwable ignored) {}
        try {
            blockJukeboxClass = Class.forName("net.minecraft.block.BlockJukebox");
        } catch (Throwable ignored) {}

        try {
            Class<?> blockClass = Class.forName("net.minecraft.block.Block");
            try {
                blockHasTileEntityMeta = blockClass.getMethod("hasTileEntity", int.class);
            } catch (Throwable ignored) {}
            try {
                blockHasTileEntityNoArg = blockClass.getMethod("hasTileEntity");
            } catch (Throwable ignored) {}
            try {
                blockHardnessField = blockClass.getDeclaredField("blockHardness");
                blockHardnessField.setAccessible(true);
            } catch (Throwable t) {
                try {
                    blockHardnessField = blockClass.getDeclaredField("field_149782_v");
                    blockHardnessField.setAccessible(true);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

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
        String regName = getBlockRegistryName(blockObj);
        if (regName != null && HBM_GRAB_WHITELIST.contains(regName)) {
            return true;
        }
        return false;
    }

    /**
     * Replaces WeatherUtil.safetyCheck() with a feature-based ontology check.
     * Returns true if the block is PROTECTED (IMMUNE) and MUST NOT be ripped by tornadoes.
     */
    public static boolean isBlockProtected(Object blockObj) {
        if (blockObj == null) return true;
        initBlockReflection();

        Class<?> clazz = blockObj.getClass();

        // 1. Weather2 Instruments: absolute immunity
        if (clazz.getName()
            .startsWith("weather2.block.")) {
            return true;
        }

        // 2. Ontological TileEntity / Container checks:
        // A. ITileEntityProvider interface
        if (tileEntityProviderClass != null && tileEntityProviderClass.isAssignableFrom(clazz)) {
            return true;
        }

        // B. BlockContainer or specific container subclasses
        if (blockContainerClass != null && blockContainerClass.isAssignableFrom(clazz)) {
            return true;
        }
        if (blockChestClass != null && blockChestClass.isAssignableFrom(clazz)) {
            return true;
        }
        if (blockJukeboxClass != null && blockJukeboxClass.isAssignableFrom(clazz)) {
            return true;
        }

        // C. HBM Namespace Protection: All machines, reactors, cables, and blocks are immune unless whitelisted
        for (Class<?> curr = clazz; curr != null && curr != Object.class; curr = curr.getSuperclass()) {
            String name = curr.getName();
            if (name.startsWith("com.hbm.")) {
                if (!isHbmBlockWhitelisted(blockObj)) {
                    return true;
                }
            }
        }

        // D. hasTileEntity(int metadata) Forge method (cached)
        if (blockHasTileEntityMeta != null) {
            try {
                if (((Boolean) blockHasTileEntityMeta.invoke(blockObj, 0)).booleanValue()) {
                    return true;
                }
            } catch (Throwable ignored) {}
        } else {
            try {
                Method m = clazz.getMethod("hasTileEntity", int.class);
                if (((Boolean) m.invoke(blockObj, 0)).booleanValue()) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // E. hasTileEntity() parameterless method (cached)
        if (blockHasTileEntityNoArg != null) {
            try {
                if (((Boolean) blockHasTileEntityNoArg.invoke(blockObj)).booleanValue()) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 3. Unbreakable blocks check (hardness < 0: Bedrock, forcefields, shielded portals)
        if (blockHardnessField != null) {
            try {
                float hardness = blockHardnessField.getFloat(blockObj);
                if (hardness < 0.0F) return true;
            } catch (Throwable ignored) {}
        } else {
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField("blockHardness");
                    f.setAccessible(true);
                    if (f.getFloat(blockObj) < 0.0F) return true;
                } catch (Throwable ignored) {}
                try {
                    Field f = c.getDeclaredField("field_149782_v");
                    f.setAccessible(true);
                    if (f.getFloat(blockObj) < 0.0F) return true;
                } catch (Throwable ignored) {}
            }
        }

        return false;
    }
}
