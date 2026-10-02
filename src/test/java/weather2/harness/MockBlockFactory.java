package weather2.harness;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import sun.misc.Unsafe;

/**
 * Headless factory for initializing vanilla Blocks in unit tests
 * without needing full FML/Forge GameData lifecycle.
 */
public final class MockBlockFactory {

    private static final Unsafe unsafe;
    private static Field materialField;
    private static Field hardnessField;

    static {
        Unsafe u = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = (Unsafe) f.get(null);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to access sun.misc.Unsafe", t);
        }
        unsafe = u;

        try {
            materialField = Block.class.getDeclaredField("blockMaterial");
            materialField.setAccessible(true);
        } catch (Throwable ignored) {}

        try {
            hardnessField = Block.class.getDeclaredField("blockHardness");
            hardnessField.setAccessible(true);
        } catch (Throwable ignored) {}
    }

    private MockBlockFactory() {}

    public static class MockLog extends BlockLog {

        public MockLog() {
            super();
            this.setHardness(2.0F);
        }

        @Override
        public net.minecraft.util.IIcon getIcon(int side, int meta) {
            return null;
        }
    }

    public static class MockLeaves extends BlockLeaves {

        public MockLeaves() {
            super();
            this.setHardness(0.2F);
        }

        @Override
        public net.minecraft.util.IIcon getIcon(int side, int meta) {
            return null;
        }

        @Override
        public String[] func_150125_e() {
            return new String[] { "oak" };
        }
    }

    public static class MockTorch extends BlockTorch {

        public MockTorch() {
            super();
            this.setHardness(0.0F);
        }
    }

    public static class MockFence extends BlockFence {

        public MockFence() {
            super("wood", Material.wood);
            this.setHardness(2.0F);
        }
    }

    public static class MockPlanks extends Block {

        public MockPlanks() {
            super(Material.wood);
            this.setHardness(2.0F);
        }
    }

    public static class MockStone extends Block {

        public MockStone() {
            super(Material.rock);
            this.setHardness(1.5F);
        }
    }

    public static class MockBedrock extends Block {

        public MockBedrock() {
            super(Material.rock);
            this.setHardness(-1.0F);
        }
    }

    public static class MockDirt extends Block {

        public MockDirt() {
            super(Material.ground);
            this.setHardness(0.5F);
        }
    }

    public static class MockGrass extends Block {

        public MockGrass() {
            super(Material.grass);
            this.setHardness(0.6F);
        }
    }

    public static class MockSand extends Block {

        public MockSand() {
            super(Material.sand);
            this.setHardness(0.5F);
        }
    }

    public static class MockGravel extends Block {

        public MockGravel() {
            super(Material.sand);
            this.setHardness(0.6F);
        }
    }

    public static void setStaticBlock(String fieldName, Block block) {
        try {
            Field f = Blocks.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            long offset = unsafe.staticFieldOffset(f);
            Object base = unsafe.staticFieldBase(f);
            unsafe.putObject(base, offset, block);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to inject mock block into Blocks." + fieldName, t);
        }
    }

    public static void initVanillaBlocks() {
        if (Blocks.dirt != null && Blocks.torch != null && Blocks.planks != null) {
            return;
        }

        Block bedrock = new MockBedrock();
        Block dirt = new MockDirt();
        Block grass = new MockGrass();
        Block sand = new MockSand();
        Block gravel = new MockGravel();
        Block stone = new MockStone();
        Block planks = new MockPlanks();
        Block log = new MockLog();
        Block torch = new MockTorch();
        Block leaves = new MockLeaves();
        Block fence = new MockFence();

        setStaticBlock("bedrock", bedrock);
        setStaticBlock("dirt", dirt);
        setStaticBlock("grass", grass);
        setStaticBlock("sand", sand);
        setStaticBlock("gravel", gravel);
        setStaticBlock("stone", stone);
        setStaticBlock("planks", planks);
        setStaticBlock("log", log);
        setStaticBlock("torch", torch);
        setStaticBlock("leaves", leaves);
        setStaticBlock("fence", fence);
    }
}
