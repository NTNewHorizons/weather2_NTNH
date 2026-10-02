package weather2.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.climate.ClimateEngine;
import weather2.compat.WeatherNTNHHooks;
import weather2.harness.TestEnvironment;
import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.BlockProtectionPipeline;
import weather2.protection.CompositeGrabPolicy;
import weather2.protection.GrabDecision;
import weather2.protection.policy.HardnessThresholdPolicy;
import weather2.protection.policy.InstrumentsProtectionPolicy;
import weather2.protection.policy.PreflightBudgetPolicy;
import weather2.protection.policy.TerrainProtectionPolicy;
import weather2.protection.policy.TileEntityProtectionPolicy;
import weather2.weathersystem.storm.StormObject;

public class BlockGrabPolicyTest {

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testBlockContextPooling() {
        BlockContext ctx1 = BlockContext.getPooled(null, 10, 20, 30, null, null);
        assertEquals(10, ctx1.x);
        assertEquals(20, ctx1.y);
        assertEquals(30, ctx1.z);

        BlockContext ctx2 = BlockContext.getPooled(null, 50, 60, 70, null, null);
        assertSame("ThreadLocal pooling must reuse the same instance (0 heap allocations)", ctx1, ctx2);
        assertEquals(50, ctx2.x);
        assertEquals(60, ctx2.y);
        assertEquals(70, ctx2.z);
    }

    @Test
    public void testCompositeGrabPolicyChain() {
        CompositeGrabPolicy chain = new CompositeGrabPolicy();

        // 1. Empty chain should deny by default
        BlockContext ctx = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertFalse("Empty chain must deny by default", chain.canGrab(ctx));

        // 2. Add policy returning PASS -> still denies
        chain.addPolicy(new BlockGrabPolicy() {

            @Override
            public GrabDecision evaluate(BlockContext c) {
                return GrabDecision.PASS;
            }
        });
        assertFalse("PASS only chain must deny by default", chain.canGrab(ctx));

        // 3. Add policy returning ALLOW -> passes
        chain.addPolicy(new BlockGrabPolicy() {

            @Override
            public GrabDecision evaluate(BlockContext c) {
                return GrabDecision.ALLOW;
            }
        });
        assertTrue("Chain with ALLOW must permit grab", chain.canGrab(ctx));

        // 4. Prepend policy returning DENY -> vetoes regardless of subsequent ALLOW
        CompositeGrabPolicy vetoChain = new CompositeGrabPolicy();
        vetoChain.addPolicy(new BlockGrabPolicy() {

            @Override
            public GrabDecision evaluate(BlockContext c) {
                return GrabDecision.DENY;
            }
        });
        vetoChain.addPolicy(new BlockGrabPolicy() {

            @Override
            public GrabDecision evaluate(BlockContext c) {
                return GrabDecision.ALLOW;
            }
        });
        assertFalse("DENY must immediately veto before subsequent ALLOW", vetoChain.canGrab(ctx));
    }

    @Test
    public void testPreflightBudgetPolicy() {
        PreflightBudgetPolicy policy = new PreflightBudgetPolicy();
        ClimateEngine.ensureProfilesLoaded();

        // Overworld (dim 0) -> grab is true in profile -> passes to selective policy
        BlockContext ctxDim0 = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertEquals("Overworld (dim 0) allows selective grabs", GrabDecision.PASS, policy.evaluate(ctxDim0));

        // Vacuum body (Moon, dim 15) -> grab is false -> must DENY
        BlockContext ctxMoon = BlockContext
            .getPooled(null, 0, 0, 0, null, weather2.harness.MockStormFactory.create(5, 1002L));
        if (ctxMoon.storm != null && ctxMoon.storm.manager != null) {
            ctxMoon.storm.manager.dim = 15;
        }
        assertEquals("Moon (dim 15) must be DENIED in preflight", GrabDecision.DENY, policy.evaluate(ctxMoon));

        // When moving blocks budget is exceeded in dim 18
        int dimEve = 18;
        int maxCap = WeatherNTNHHooks.getMaxMovingBlocksPerDimension();
        for (int i = 0; i < maxCap; i++) {
            WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        }
        assertEquals(maxCap, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
    }

    @Test
    public void testInstrumentsProtectionPolicy() {
        InstrumentsProtectionPolicy policy = new InstrumentsProtectionPolicy();

        // Null block -> DENY
        BlockContext ctxNull = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertEquals(GrabDecision.DENY, policy.evaluate(ctxNull));
    }

    @Test
    public void testTileEntityProtectionPolicy() {
        TileEntityProtectionPolicy policy = new TileEntityProtectionPolicy();

        // Null block -> DENY
        BlockContext ctxNull = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertEquals(GrabDecision.DENY, policy.evaluate(ctxNull));
    }

    @Test
    public void testHbmOntologyPolicy() {
        HbmProtectionAdapter.addWhitelistedBlock("hbm:test_rubble");
        assertTrue(HbmProtectionAdapter.isHbmBlockWhitelisted("hbm:test_rubble"));
    }

    @Test
    public void testTerrainProtectionPolicy() {
        TerrainProtectionPolicy policy = new TerrainProtectionPolicy();

        // 1. Bedrock/shield (hardness < 0) -> DENY
        BlockContext ctx = BlockContext.getPooled(null, 0, 0, 0, net.minecraft.init.Blocks.bedrock, null);
        assertEquals(GrabDecision.DENY, policy.evaluate(ctx));

        // 2. Tree trunks -> DENY
        BlockContext ctxLog = BlockContext.getPooled(null, 0, 0, 0, net.minecraft.init.Blocks.log, null);
        assertEquals("Logs must be denied by RefinedGrabRules", GrabDecision.DENY, policy.evaluate(ctxLog));

        // 3. Overworld terrain soil -> DENY
        BlockContext ctxOverworldDirt = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.dirt, null);
        assertEquals("Overworld terrain must always be DENIED", GrabDecision.DENY, policy.evaluate(ctxOverworldDirt));

        // 4. Planetary soil in low-tier storm (F2, stage 6) -> DENY
        StormObject eveF2 = weather2.harness.MockStormFactory.create(6, 2001L);
        eveF2.manager.dim = 18;
        BlockContext ctxEveF2 = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.dirt, eveF2);
        assertEquals("Planetary low-tier storm must not erode soil", GrabDecision.DENY, policy.evaluate(ctxEveF2));

        // 5. Planetary soil in catastrophic storm (F5, stage 9):
        // Find coordinates matching spatial hash modulo 0: (x * 31 + z * 17 + y) % 6 == 0
        StormObject eveF5 = weather2.harness.MockStormFactory.create(9, 2002L);
        eveF5.manager.dim = 18;
        // x=0, z=0, y=60: (0 + 0 + 60) % 6 == 0
        BlockContext ctxErosionPass = BlockContext.getPooled(null, 0, 60, 0, net.minecraft.init.Blocks.dirt, eveF5);
        assertEquals(
            "Surface soil exposed to F5 with hash % 6 == 0 must PASS to hardness policy",
            GrabDecision.PASS,
            policy.evaluate(ctxErosionPass));

        // x=0, z=0, y=61: (0 + 0 + 61) % 6 == 1 != 0
        BlockContext ctxErosionDenied = BlockContext.getPooled(null, 0, 61, 0, net.minecraft.init.Blocks.dirt, eveF5);
        assertEquals(
            "Surface soil with hash % 6 != 0 must be DENIED (throttled)",
            GrabDecision.DENY,
            policy.evaluate(ctxErosionDenied));
    }

    @Test
    public void testHardnessThresholdPolicy() {
        HardnessThresholdPolicy policy = new HardnessThresholdPolicy();

        // 1. Null block -> DENY
        BlockContext ctxNull = BlockContext.getPooled(null, 0, 0, 0, null, null);
        assertEquals(GrabDecision.DENY, policy.evaluate(ctxNull));

        // 2. Overworld solid building block (planks, stone) -> DENY
        BlockContext ctxPlanks = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.planks, null);
        assertEquals(
            "Overworld wood planks must be DENIED (solid immunity)",
            GrabDecision.DENY,
            policy.evaluate(ctxPlanks));

        // 3. Overworld fragile blocks (torch, leaves, fence) -> ALLOW
        BlockContext ctxTorch = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.torch, null);
        assertEquals("Overworld torch must be ALLOWED", GrabDecision.ALLOW, policy.evaluate(ctxTorch));

        BlockContext ctxLeaves = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.leaves, null);
        assertEquals("Overworld leaves must be ALLOWED", GrabDecision.ALLOW, policy.evaluate(ctxLeaves));

        BlockContext ctxFence = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.fence, null);
        assertEquals("Overworld fence must be ALLOWED", GrabDecision.ALLOW, policy.evaluate(ctxFence));

        // 4. Planetary wood planks (Eve dim 18) -> ALLOW
        StormObject eveStorm = weather2.harness.MockStormFactory.create(8, 3001L);
        eveStorm.manager.dim = 18;
        BlockContext ctxEvePlanks = BlockContext.getPooled(null, 0, 64, 0, net.minecraft.init.Blocks.planks, eveStorm);
        assertEquals(
            "Planetary wooden structures must be vulnerable to storm",
            GrabDecision.ALLOW,
            policy.evaluate(ctxEvePlanks));
    }

    @Test
    public void testPipelineIntegration() {
        assertNotNull(BlockProtectionPipeline.getDefaultPolicy());
        assertEquals(
            "Default policy chain must have 6 policies",
            6,
            BlockProtectionPipeline.getDefaultPolicy()
                .getPolicies()
                .size());

        assertNotNull(BlockProtectionPipeline.getStaticProtectionPolicy());
        assertEquals(
            "Static protection policy chain must have 4 policies",
            4,
            BlockProtectionPipeline.getStaticProtectionPolicy()
                .getPolicies()
                .size());

        assertTrue("Null block must be protected", BlockProtectionPipeline.isBlockProtected(null));
    }
}
