package weather2.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.harness.MockDeflector;
import weather2.harness.MockStormFactory;
import weather2.harness.MockWeatherManager;
import weather2.harness.TestEnvironment;
import weather2.util.WeatherEveHelper;
import weather2.weathersystem.storm.StormObject;

public class DeflectorSimulationTest {

    private MockDeflector deflector;
    private MockWeatherManager weatherManager;

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
        deflector = new MockDeflector();
        weatherManager = new MockWeatherManager();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testSequentialStormAssault() {
        // Deflector initialized with full buffer of 100M HE
        deflector.power = 100000000L;
        deflector.isFieldActive = true;

        // Wave 1: Rain storm (Stage 0, cost 100k HE)
        StormObject wave1 = MockStormFactory.create(0, 1L);
        boolean collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave1);
        assertFalse(collapsed);
        assertEquals(99900000L, deflector.power);
        assertTrue(wave1.isDead);

        // Wave 2: F1 storm (Stage 5, cost 1.5M HE)
        StormObject wave2 = MockStormFactory.create(5, 2L);
        collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave2);
        assertFalse(collapsed);
        assertEquals(98400000L, deflector.power); // 99.9M - 1.5M = 98.4M
        assertTrue(wave2.isDead);

        // Wave 3: F3 storm (Stage 7, cost 15.0M HE)
        StormObject wave3 = MockStormFactory.create(7, 3L);
        collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave3);
        assertFalse(collapsed);
        assertEquals(83400000L, deflector.power); // 98.4M - 15M = 83.4M
        assertTrue(wave3.isDead);

        // Wave 4: F4 storm (Stage 8, cost 50.0M HE)
        StormObject wave4 = MockStormFactory.create(8, 4L);
        collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave4);
        assertFalse(collapsed);
        assertEquals(33400000L, deflector.power); // 83.4M - 50M = 33.4M
        assertTrue(wave4.isDead);

        // Wave 5: Second F4 storm arrives (requires 50M HE, but only 33.4M available)
        StormObject wave5 = MockStormFactory.create(8, 5L);
        collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave5);
        assertTrue("Insufficient power must collapse deflector", collapsed);
        assertEquals(0L, deflector.power);
        assertFalse("Shield collapses", deflector.isFieldActive);
        assertEquals(200, deflector.blackoutCooldown);
        assertFalse("Storm penetrates shield on blackout", wave5.isDead);

        // Cooldown expires (simulate 200 ticks of recovery)
        deflector.blackoutCooldown = 0;
        deflector.power = 100000000L;
        deflector.isFieldActive = true;

        // Wave 6: Catastrophic F5 storm arrives (Stage 9, Unsuppressable)
        StormObject wave6 = MockStormFactory.create(9, 6L);
        collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, wave6);
        assertTrue("F5 must trigger overload collapse immediately", collapsed);
        assertFalse("Shield collapsed", deflector.isFieldActive);
        assertEquals(300, deflector.blackoutCooldown);
        assertFalse("F5 storm penetrates shield", wave6.isDead);
    }
}
