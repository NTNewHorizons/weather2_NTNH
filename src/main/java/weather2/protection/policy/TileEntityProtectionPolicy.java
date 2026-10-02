package weather2.protection.policy;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLog;
import net.minecraft.block.ITileEntityProvider;

import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: Ontological TileEntity & Container Protection Policy.
 * Provides 100% loss-prevention immunity for all inventories, machines, pipes, cables,
 * and custom modded storage units. Evaluates both class ontology and real World TileEntity instances.
 */
public class TileEntityProtectionPolicy implements BlockGrabPolicy {

    private static Method blockHasTileEntityMeta = null;
    private static Method blockHasTileEntityNoArg = null;
    private static boolean methodReflectionInit = false;

    private static void initMethods(Class<?> blockClass) {
        if (methodReflectionInit) return;
        methodReflectionInit = true;
        try {
            blockHasTileEntityMeta = blockClass.getMethod("hasTileEntity", int.class);
        } catch (Throwable ignored) {}
        try {
            blockHasTileEntityNoArg = blockClass.getMethod("hasTileEntity");
        } catch (Throwable ignored) {}
    }

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        Block block = ctx.block;
        if (block == null) {
            return GrabDecision.DENY;
        }

        Class<?> clazz = block.getClass();

        // 1. Ontological class checks
        if (ITileEntityProvider.class.isAssignableFrom(clazz)) {
            return GrabDecision.DENY;
        }
        if (BlockContainer.class.isAssignableFrom(clazz)) {
            return GrabDecision.DENY;
        }
        if (BlockChest.class.isAssignableFrom(clazz) || BlockJukebox.class.isAssignableFrom(clazz)
            || BlockLog.class.isAssignableFrom(clazz)) {
            return GrabDecision.DENY;
        }

        // 2. Real World TileEntity presence check (at exact coordinates x, y, z)
        if (ctx.getTileEntity() != null) {
            return GrabDecision.DENY;
        }

        // 3. Forge hasTileEntity(metadata) check
        initMethods(Block.class);
        if (blockHasTileEntityMeta != null) {
            try {
                int meta = ctx.getMetadata();
                if (((Boolean) blockHasTileEntityMeta.invoke(block, meta)).booleanValue()) {
                    return GrabDecision.DENY;
                }
            } catch (Throwable ignored) {}
        }

        // 4. Parameterless hasTileEntity() check
        if (blockHasTileEntityNoArg != null) {
            try {
                if (((Boolean) blockHasTileEntityNoArg.invoke(block)).booleanValue()) {
                    return GrabDecision.DENY;
                }
            } catch (Throwable ignored) {}
        }

        return GrabDecision.PASS;
    }
}
