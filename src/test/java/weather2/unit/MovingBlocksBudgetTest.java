package weather2.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.compat.WeatherNTNHHooks;
import weather2.harness.TestEnvironment;

public class MovingBlocksBudgetTest {

    private final int dimEve = 18;
    private final int dimDuna = 16;
    private final int dimOverworld = 0;

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testBudgetIncrementsAndDecrements() {
        assertEquals("Initial count must be 0", 0, WeatherNTNHHooks.getMovingBlocksCount(dimEve));

        for (int i = 0; i < 50; i++) {
            WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        }
        assertEquals("Expected 50 moving blocks", 50, WeatherNTNHHooks.getMovingBlocksCount(dimEve));

        for (int i = 0; i < 20; i++) {
            WeatherNTNHHooks.decrementMovingBlocks(dimEve);
        }
        assertEquals("Expected 30 moving blocks", 30, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
    }

    @Test
    public void testUnderflowProtection() {
        for (int i = 0; i < 100; i++) {
            WeatherNTNHHooks.decrementMovingBlocks(dimEve);
        }
        assertEquals("Counter below 0 must clamp to 0", 0, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
    }

    @Test
    public void testDimensionIsolation() {
        WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        WeatherNTNHHooks.incrementMovingBlocks(dimDuna);

        assertEquals(2, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
        assertEquals(1, WeatherNTNHHooks.getMovingBlocksCount(dimDuna));
        assertEquals(0, WeatherNTNHHooks.getMovingBlocksCount(dimOverworld));
    }

    @Test
    public void testCapEnforcement() {
        int maxCap = WeatherNTNHHooks.getMaxMovingBlocksPerDimension();
        assertTrue("Max cap must be positive", maxCap > 0);
        assertEquals(200, maxCap);
    }

    @Test
    public void testReconcile() {
        WeatherNTNHHooks.incrementMovingBlocks(dimEve);
        WeatherNTNHHooks.reconcileMovingBlocks(dimEve, null);
        assertEquals("Reconcile null should reset to 0", 0, WeatherNTNHHooks.getMovingBlocksCount(dimEve));
    }
}
