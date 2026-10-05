package weather2.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.deflector.DeflectorState;
import weather2.harness.MockDeflector;
import weather2.harness.MockStormFactory;
import weather2.harness.MockWeatherManager;
import weather2.harness.TestEnvironment;
import weather2.util.WeatherEveHelper;
import weather2.weathersystem.storm.StormObject;

public class DeflectorEscalationTest {

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
    public void testDissipationCostCurve() {
        assertEquals("Stage 0 cost mismatch", 100000L, WeatherEveHelper.getDeflectorDissipationCost(0));
        assertEquals("Stage 3 cost mismatch", 100000L, WeatherEveHelper.getDeflectorDissipationCost(3));
        assertEquals("Stage 4 cost mismatch", 500000L, WeatherEveHelper.getDeflectorDissipationCost(4));
        assertEquals("Stage 5 cost mismatch", 1500000L, WeatherEveHelper.getDeflectorDissipationCost(5));
        assertEquals("Stage 6 cost mismatch", 5000000L, WeatherEveHelper.getDeflectorDissipationCost(6));
        assertEquals("Stage 7 cost mismatch", 15000000L, WeatherEveHelper.getDeflectorDissipationCost(7));
        assertEquals("Stage 8 cost mismatch", 50000000L, WeatherEveHelper.getDeflectorDissipationCost(8));
        assertEquals(
            "Stage 9 cost mismatch (must be -1 for F5 overload)",
            -1L,
            WeatherEveHelper.getDeflectorDissipationCost(9));
    }

    @Test
    public void testIdlePowerDrain() {
        // Trickle power below hysteresis threshold (50k < 100k) must NOT activate OFFLINE deflector (anti-chatter)
        deflector.power = 50000L;
        boolean powered = WeatherEveHelper.tickDeflectorPower(deflector);
        assertFalse("Below 100k activation hysteresis, OFFLINE deflector must remain OFFLINE", powered);
        assertFalse("Field must remain inactive below hysteresis threshold", deflector.isFieldActive);
        assertEquals("Power must not be drained while OFFLINE", 50000L, deflector.power);

        // Once buffer reaches 100k HE threshold, deflector activates and drains 25k/t down to 25k HE
        deflector.power = 100000L;
        powered = WeatherEveHelper.tickDeflectorPower(deflector);
        assertTrue("tickDeflectorPower should be true at 100k threshold", powered);
        assertTrue("Field should be active", deflector.isFieldActive);
        assertEquals("Power should be 100k - 25k = 75k", 75000L, deflector.power);

        // While ACTIVE, 50k HE is sufficient to stay ACTIVE (hysteresis holds until < 25k)
        deflector.power = 50000L;
        powered = WeatherEveHelper.tickDeflectorPower(deflector);
        assertTrue("ACTIVE deflector stays on at 50k HE", powered);
        assertEquals(25000L, deflector.power);

        deflector.power = 10000L;
        powered = WeatherEveHelper.tickDeflectorPower(deflector);
        assertFalse("tickDeflectorPower should be false with insufficient power (< 25k)", powered);
        assertFalse("Field should be inactive", deflector.isFieldActive);
    }

    @Test
    public void testNormalDissipation() {
        deflector.power = 20000000L;
        deflector.isFieldActive = true;
        StormObject f2Storm = MockStormFactory.create(6, 12345L);

        boolean collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, f2Storm);
        assertFalse("F2 storm should not collapse deflector", collapsed);
        assertEquals("20M - 5M = 15M HE remaining", 15000000L, deflector.power);
        assertTrue("Field remains active", deflector.isFieldActive);
        assertEquals("Storm must be marked dead", true, f2Storm.isDead);
        assertEquals("Storm ID must match removed ID", 12345L, weatherManager.removedStormId);
        assertTrue("Sync packet must be sent", weatherManager.syncCalled);
    }

    @Test
    public void testInsufficientPowerBlackout() {
        deflector.power = 2000000L; // 2M HE
        deflector.isFieldActive = true;
        StormObject f2Storm = MockStormFactory.create(6, 22222L); // requires 5M HE

        boolean collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, f2Storm);
        assertTrue("Insufficient power must report collapse", collapsed);
        assertFalse("Field must be deactivated", deflector.isFieldActive);
        assertEquals("Power must be completely drained", 0L, deflector.power);
        assertEquals("Blackout cooldown must be set to 200 ticks", 200, deflector.blackoutCooldown);
        assertFalse("Storm must NOT be marked dead on blackout", f2Storm.isDead);
        assertEquals("Storm must NOT be removed on blackout", -1L, weatherManager.removedStormId);
    }

    @Test
    public void testF5OverloadCollapse() {
        deflector.power = 100000000L; // Full 100M HE buffer
        deflector.isFieldActive = true;
        StormObject f5Storm = MockStormFactory.create(9, 99999L); // F5 catastrophic storm

        boolean collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, f5Storm);
        assertTrue("F5 storm MUST trigger overload collapse", collapsed);
        assertFalse("Field must collapse immediately", deflector.isFieldActive);
        assertEquals("Overload blackout cooldown must be 300 ticks", 300, deflector.blackoutCooldown);
        assertFalse("F5 storm must NOT be removed", f5Storm.isDead);
        assertEquals("Storm must NOT be removed from manager", -1L, weatherManager.removedStormId);
    }

    @Test
    public void testActiveBlackoutRejection() {
        deflector.power = 50000000L;
        deflector.transitionTo(DeflectorState.BLACKOUT_DEPLETED, 150);
        StormObject f1Storm = MockStormFactory.create(5, 55555L);

        boolean collapsed = WeatherEveHelper.processDeflectorStorm(deflector, weatherManager, f1Storm);
        assertFalse("Inactive deflector during blackout does not process storm", collapsed);
        assertEquals("Power must not be drained during blackout", 50000000L, deflector.power);
        assertFalse("Storm remains untouched", f1Storm.isDead);
    }
}
