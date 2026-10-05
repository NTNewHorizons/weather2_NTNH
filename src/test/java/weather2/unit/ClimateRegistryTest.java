package weather2.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.climate.ClimateProfile;
import weather2.climate.ClimateProfileRegistry;
import weather2.harness.TestEnvironment;

public class ClimateRegistryTest {

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testClimateProfileBuilderRangeGuarding() {
        // Builder with Fujita normalization: stage 5 -> 9 (F5)
        ClimateProfile pF5 = ClimateProfile.builder("test_f5")
            .dim(101)
            .maxStage(5) // Fujita 5 -> engine stage 9
            .deadlyCooldown(-50) // clamped to 0
            .lightningMultiplier(0.0F) // clamped to 0.1
            .landSpawnOdds(-10) // clamped to 0
            .grabBlocks(true)
            .build();

        assertEquals(101, pF5.dim);
        assertEquals("Fujita 5 should normalize to engine stage 9", 9, pF5.maxStage);
        assertEquals("Deadly cooldown should clamp to 0", 0, pF5.deadlyCooldown);
        assertEquals(0.1F, pF5.lightningMultiplier, 0.001F);
        assertEquals("Land spawn odds should clamp to 0", 0, pF5.landSpawnOdds);
        assertTrue("Grab blocks should be true", pF5.grabBlocks);
    }

    @Test
    public void testExactStageSetting() {
        ClimateProfile pExact = ClimateProfile.builder("test_exact")
            .dim(102)
            .exactStage(4)
            .build();
        assertEquals("exactStage(4) should be 4", 4, pExact.maxStage);

        ClimateProfile pTokenF1 = ClimateProfile.builder("test_token_f1")
            .dim(103)
            .parseStage("F1")
            .build();
        assertEquals("parseStage('F1') should map to stage 5", 5, pTokenF1.maxStage);

        ClimateProfile pTokenNum = ClimateProfile.builder("test_token_num")
            .dim(104)
            .parseStage("5")
            .build();
        assertEquals("parseStage('5') should remain exact stage 5 (not shifted to 9)", 5, pTokenNum.maxStage);
    }

    @Test
    public void testDefaultProfiles() {
        weather2.climate.ClimateEngine.ensureProfilesLoaded();

        // Overworld (dim 0)
        ClimateProfile overworld = ClimateProfileRegistry.get(0);
        assertNotNull(overworld);
        assertEquals(
            "Overworld maxStage should be 5 (F1 localized threat) even after config load",
            5,
            overworld.maxStage);
        assertTrue("Overworld grabBlocks must be true for selective grab", overworld.grabBlocks);
        assertTrue(
            "Overworld grab should be allowed in registry for selective grab",
            ClimateProfileRegistry.isGrabAllowed(0));

        // Duna (dim 16)
        ClimateProfile duna = ClimateProfileRegistry.get(16);
        assertNotNull(duna);
        assertEquals(9, duna.maxStage);
        assertTrue(duna.grabBlocks);
        assertTrue(ClimateProfileRegistry.isGrabAllowed(16));

        // Eve (dim 18)
        ClimateProfile eve = ClimateProfileRegistry.get(18);
        assertNotNull(eve);
        assertEquals(9, eve.maxStage);
        assertTrue(eve.alwaysProgresses);
        assertEquals(800, eve.deadlyCooldown);
        assertEquals(6.0F, eve.lightningMultiplier, 0.001F);
        assertTrue(ClimateProfileRegistry.isGrabAllowed(18));

        // Vacuum bodies
        ClimateProfile moon = ClimateProfileRegistry.get(15);
        assertNotNull(moon);
        assertFalse(moon.weatherEnabled);
        assertFalse(ClimateProfileRegistry.isGrabAllowed(15));

        ClimateProfile thatmo = ClimateProfileRegistry.get(413025);
        assertNotNull(thatmo);
        assertFalse(thatmo.weatherEnabled);
        assertFalse(ClimateProfileRegistry.isGrabAllowed(413025));
    }

    @Test
    public void testNamedLookup() {
        ClimateProfile eve = ClimateProfileRegistry.get(18);
        ClimateProfile duna = ClimateProfileRegistry.get(16);

        assertSame("Named lookup for 'eve' failed", eve, ClimateProfileRegistry.get("eve"));
        assertSame("Case-insensitive lookup for 'EVE' failed", eve, ClimateProfileRegistry.get("EVE"));
        assertSame("Named lookup for 'duna' failed", duna, ClimateProfileRegistry.get("duna"));
    }

    @Test
    public void testDynamicProgrammaticRegistration() {
        ClimateProfile customPlanet = ClimateProfile.builder(77, "ares")
            .weatherEnabled(true)
            .exactStage(8)
            .deadlyCooldown(1200)
            .lightningMultiplier(3.5F)
            .spawnOdds(5, 0)
            .grabBlocks(true)
            .build();

        ClimateProfileRegistry.register(customPlanet);
        assertTrue("Registry should have dim 77", ClimateProfileRegistry.has(77));
        assertSame("get(77) mismatch", customPlanet, ClimateProfileRegistry.get(77));
        assertSame("get('ares') mismatch", customPlanet, ClimateProfileRegistry.get("ares"));
        assertTrue("Grab should be allowed on Ares", ClimateProfileRegistry.isGrabAllowed(77));
    }

    @Test
    public void testConfigLayeringOverrides() {
        ClimateProfile customPlanet = ClimateProfile.builder(77, "ares")
            .weatherEnabled(true)
            .exactStage(4)
            .build();
        ClimateProfileRegistry.register(customPlanet);

        String customConfig = "ares, 9, true, 400, 8.0, 2, 0, true";
        ClimateProfileRegistry.loadFromConfig(customConfig);

        ClimateProfile overriddenAres = ClimateProfileRegistry.get(77);
        assertEquals("Overridden maxStage should be 9", 9, overriddenAres.maxStage);
        assertTrue("Overridden alwaysProgresses should be true", overriddenAres.alwaysProgresses);
        assertEquals("Overridden cooldown should be 400", 400, overriddenAres.deadlyCooldown);
        assertEquals(8.0F, overriddenAres.lightningMultiplier, 0.001F);
    }

    @Test
    public void testVanillaWeatherSyncAndThunderStageRestoration() throws Exception {
        weather2.weathersystem.WeatherManagerClient wmClient = new weather2.weathersystem.WeatherManagerClient(0);
        assertFalse(wmClient.isVanillaRainActiveOnServer);
        assertFalse(wmClient.isVanillaThunderActiveOnServer);

        net.minecraft.nbt.NBTTagCompound pkt = new net.minecraft.nbt.NBTTagCompound();
        pkt.setString("command", "syncWeatherUpdate");
        net.minecraft.nbt.NBTTagCompound inner = new net.minecraft.nbt.NBTTagCompound();
        inner.setBoolean("isVanillaRainActiveOnServer", true);
        inner.setBoolean("isVanillaThunderActiveOnServer", true);
        pkt.setTag("data", inner);

        wmClient.nbtSyncFromServer(pkt);
        assertTrue("Rain flag must sync from data compound", wmClient.isVanillaRainActiveOnServer);
        assertTrue("Thunder flag must sync from data compound", wmClient.isVanillaThunderActiveOnServer);

        // Verify that a StormObject capped to STATE_THUNDER by vanilla /weather thunder restores maxStage on
        // initRealStorm
        weather2.weathersystem.storm.StormObject so = weather2.harness.MockStormFactory.create(1, 1L, 16);
        so.maxIntensityStage = weather2.weathersystem.storm.StormObject.STATE_THUNDER;
        weather2.climate.ClimateEngine.onInitRealStorm(so);
        assertEquals("Duna storm must restore maxStage=9 on initRealStorm", 9, so.maxIntensityStage);

        // Verify per-dimension lightning frequency differentiation (Overworld 0.25x vs Duna 1.0x vs Tekto 2.0x vs Eve
        // 6.0x)
        weather2.weathersystem.storm.StormObject overworldStorm = weather2.harness.MockStormFactory.create(1, 1L, 0);
        weather2.weathersystem.storm.StormObject dunaStorm = weather2.harness.MockStormFactory.create(1, 1L, 16);
        weather2.weathersystem.storm.StormObject tektoStorm = weather2.harness.MockStormFactory.create(1, 1L, 24);
        weather2.weathersystem.storm.StormObject eveStorm = weather2.harness.MockStormFactory.create(1, 1L, 18);

        assertEquals(
            "Overworld lightning odds (0.25x) must be 4x rarer (800)",
            800,
            weather2.climate.ClimateEngine.getAdjustedLightningOdds(200, overworldStorm));
        assertEquals(
            "Duna lightning odds (1.0x) must be 200",
            200,
            weather2.climate.ClimateEngine.getAdjustedLightningOdds(200, dunaStorm));
        assertEquals(
            "Tekto lightning odds (2.0x) must be 100",
            100,
            weather2.climate.ClimateEngine.getAdjustedLightningOdds(200, tektoStorm));
        assertEquals(
            "Eve lightning odds (6.0x) must be 33",
            33,
            weather2.climate.ClimateEngine.getAdjustedLightningOdds(200, eveStorm));
    }
}
