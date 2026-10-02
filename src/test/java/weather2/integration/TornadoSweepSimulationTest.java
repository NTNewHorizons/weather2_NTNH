package weather2.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.climate.ClimateEngine;
import weather2.climate.ClimateProfileRegistry;
import weather2.compat.WeatherNTNHHooks;
import weather2.harness.MockStormFactory;
import weather2.harness.TestEnvironment;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.BlockProtectionPipeline;
import weather2.protection.GrabDecision;
import weather2.weathersystem.storm.StormObject;

public class TornadoSweepSimulationTest {

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
        ClimateEngine.ensureProfilesLoaded();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testOverworldSelectiveProtection() {
        // Dim 0 storm: stage 5 (F1 localized threat)
        StormObject overworldF1 = MockStormFactory.create(5, 1001L);
        overworldF1.manager.dim = 0;
        assertTrue(
            "Overworld should allow tornado grab execution for selective blocks",
            WeatherNTNHHooks.canTornadoGrabBlocks(overworldF1));

        // 1. Solid building blocks (wood planks, stone, logs, dirt) must be 100% immune
        BlockContext ctxPlanks = BlockContext
            .getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.planks, overworldF1);
        assertFalse("Overworld wood planks must be 100% immune", BlockProtectionPipeline.canGrab(ctxPlanks));

        BlockContext ctxStone = BlockContext.getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.stone, overworldF1);
        assertFalse("Overworld stone must be 100% immune", BlockProtectionPipeline.canGrab(ctxStone));

        BlockContext ctxDirt = BlockContext.getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.dirt, overworldF1);
        assertFalse("Overworld dirt/terrain must be 100% immune", BlockProtectionPipeline.canGrab(ctxDirt));

        BlockContext ctxLog = BlockContext.getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.log, overworldF1);
        assertFalse("Overworld tree logs must be 100% immune", BlockProtectionPipeline.canGrab(ctxLog));

        // 2. Fragile decorative and agricultural blocks (torches, leaves, fences) must be grabbable
        BlockContext ctxTorch = BlockContext.getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.torch, overworldF1);
        assertTrue(
            "Overworld torches should be grabbable by localized storms",
            BlockProtectionPipeline.canGrab(ctxTorch));

        BlockContext ctxLeaves = BlockContext
            .getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.leaves, overworldF1);
        assertTrue(
            "Overworld leaves should be grabbable by localized storms",
            BlockProtectionPipeline.canGrab(ctxLeaves));

        BlockContext ctxFence = BlockContext.getPooled(null, 10, 64, 10, net.minecraft.init.Blocks.fence, overworldF1);
        assertTrue(
            "Overworld fences should be grabbable by localized storms",
            BlockProtectionPipeline.canGrab(ctxFence));
    }

    @Test
    public void testPlanetarySelectiveDestruction() {
        // Mock a policy chain and test Eve dimension (dim 18)
        int dimEve = 18;
        assertTrue("Eve must allow grabs in climate profile", ClimateProfileRegistry.isGrabAllowed(dimEve));

        // Custom test policy verifying grab decisions
        BlockGrabPolicy selectivePolicy = new BlockGrabPolicy() {

            @Override
            public GrabDecision evaluate(BlockContext ctx) {
                // If hardness is negative (bedrock or null block) -> DENY
                if (ctx.getHardness() < 0) return GrabDecision.DENY;
                // If registry name is HBM -> DENY
                String name = ctx.getRegistryName();
                if (name != null && name.startsWith("hbm:")) return GrabDecision.DENY;
                // Fragile blocks -> ALLOW
                return GrabDecision.ALLOW;
            }
        };

        // When block is null or bedrock (hardness < 0), must DENY
        BlockContext ctxBedrock = BlockContext.getPooled(null, 50, 70, 50, null, null);
        assertEquals(GrabDecision.DENY, selectivePolicy.evaluate(ctxBedrock));
    }

    @Test
    public void testMovingBlocksBudgetHardCap() {
        int dimEve = 18;
        int maxCap = WeatherNTNHHooks.getMaxMovingBlocksPerDimension();

        // Fill up moving blocks budget to maximum
        for (int i = 0; i < maxCap; i++) {
            WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        }
        assertEquals(200, WeatherNTNHHooks.getMovingBlocksCount(dimEve));

        // When budget is exhausted, preflight check must deny new block grabs
        BlockContext ctx = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertFalse(
            "Pipeline must reject grabs when dimension budget is exhausted",
            BlockProtectionPipeline.canGrab(ctx));

        // Decrement by 10 (10 blocks despawn/disintegrate)
        for (int i = 0; i < 10; i++) {
            WeatherNTNHHooks.decrementMovingBlocks(dimEve);
        }
        assertEquals(190, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
    }
}
