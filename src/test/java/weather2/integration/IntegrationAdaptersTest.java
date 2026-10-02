package weather2.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.climate.ClimateEngine;
import weather2.harness.MockDeflector;
import weather2.harness.TestEnvironment;
import weather2.integration.cofh.CoFHEnergyAdapter;
import weather2.integration.dsurround.DSurroundWeatherAdapter;
import weather2.integration.hbm.HbmEnergyAdapter;
import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.integration.hbm.HbmSpaceAdapter;

public class IntegrationAdaptersTest {

    private MockDeflector deflector;
    private final long maxPower = 100000000L; // 100M HE = 400M RF

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
        deflector = new MockDeflector();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testCoFHEnergyAdapter() {
        // RF conversion
        assertEquals(40, CoFHEnergyAdapter.heToRf(10));
        assertEquals(10, CoFHEnergyAdapter.rfToHe(40));
        assertEquals(0, CoFHEnergyAdapter.rfToHe(3));
        assertEquals(1, CoFHEnergyAdapter.rfToHe(7));

        // Energy storage queries
        deflector.setPower(25000L); // 25k HE
        assertEquals(100000, CoFHEnergyAdapter.getEnergyStored(deflector.getPower()));
        assertEquals(400000000, CoFHEnergyAdapter.getMaxEnergyStored(maxPower));

        // Simulation mode
        int accepted = CoFHEnergyAdapter.receiveEnergy(deflector, maxPower, 40000, true);
        assertEquals(40000, accepted);
        assertEquals(25000L, deflector.getPower());

        // Execution mode
        accepted = CoFHEnergyAdapter.receiveEnergy(deflector, maxPower, 40000, false);
        assertEquals(40000, accepted);
        assertEquals(35000L, deflector.getPower());

        // Buffer saturation
        deflector.setPower(maxPower - 100L); // only 100 HE capacity remaining
        accepted = CoFHEnergyAdapter.receiveEnergy(deflector, maxPower, 10000, false); // 2500 HE requested
        assertEquals(400, accepted); // Capped to 100 HE * 4 = 400 RF
        assertEquals(maxPower, deflector.getPower());
    }

    @Test
    public void testHbmEnergyAdapter() {
        // Power clamping
        assertEquals(500L, HbmEnergyAdapter.clampPower(500L, 1000L));
        assertEquals(1000L, HbmEnergyAdapter.clampPower(1500L, 1000L));
        assertEquals(0L, HbmEnergyAdapter.clampPower(-100L, 1000L));

        // Constants
        assertEquals(100000000L, HbmEnergyAdapter.MAX_DEFLECTOR_POWER);
        assertEquals(25000L, HbmEnergyAdapter.IDLE_DRAIN);
    }

    @Test
    public void testHbmProtectionAdapter() {
        // Class and registry checks
        assertTrue("HBM registry prefix should match", HbmProtectionAdapter.isHbmBlock(null, "hbm:ore_uranium"));
        assertTrue(
            "HBM registry machine should match",
            HbmProtectionAdapter.isHbmBlock(null, "hbm:machine_centrifuge"));
        assertFalse("Vanilla blocks should not match HBM", HbmProtectionAdapter.isHbmBlock(null, "minecraft:stone"));
        assertFalse("Null should return false", HbmProtectionAdapter.isHbmBlock(null, null));

        // Whitelisting
        assertFalse(HbmProtectionAdapter.isHbmBlockWhitelisted("hbm:rubble"));
        HbmProtectionAdapter.addWhitelistedBlock("hbm:rubble");
        assertTrue(HbmProtectionAdapter.isHbmBlockWhitelisted("hbm:rubble"));
        HbmProtectionAdapter.removeWhitelistedBlock("hbm:rubble");
        assertFalse(HbmProtectionAdapter.isHbmBlockWhitelisted("hbm:rubble"));
    }

    @Test
    public void testHbmSpaceAdapter() {
        // Dimension resolution through ClimateEngine
        int eveDim = ClimateEngine.resolveDimensionId("eve");
        assertTrue("Eve dim should be positive (18 or 413018)", eveDim == 18 || eveDim == 413018);

        int dunaDim = ClimateEngine.resolveDimensionId("duna");
        assertTrue("Duna dim should be positive (16 or 413016)", dunaDim == 16 || dunaDim == 413016);

        int laytheDim = ClimateEngine.resolveDimensionId("laythe");
        assertTrue("Laythe dim should be positive (22 or 413022)", laytheDim == 22 || laytheDim == 413022);

        int tektoDim = ClimateEngine.resolveDimensionId("tekto");
        assertTrue("Tekto dim should be positive (24 or 413024)", tektoDim == 24 || tektoDim == 413024);

        int thatmoDim = ClimateEngine.resolveDimensionId("thatmo");
        assertTrue("Thatmo dim should be positive", thatmoDim > 0);

        // Null world provider safe fallback
        assertFalse("Null world should return safe false", HbmSpaceAdapter.isCelestialWorldProvider(null));
    }

    @Test
    public void testDSurroundWeatherAdapter() {
        // Safe execution without crash
        DSurroundWeatherAdapter.INSTANCE.setIntensity(0.75F);
        DSurroundWeatherAdapter.INSTANCE.reset();
    }
}
