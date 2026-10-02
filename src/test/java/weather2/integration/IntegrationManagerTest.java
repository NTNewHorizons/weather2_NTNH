package weather2.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import weather2.climate.ClimateEngine;
import weather2.harness.TestEnvironment;
import weather2.integration.hbm.HbmEnergyAdapter;
import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.integration.hbm.HbmSpaceAdapter;

public class IntegrationManagerTest {

    public static class MockFullSpaceConfig {

        public static int dunaDimension = 16;
        public static int eveDimension = 18;
        public static int laytheDimension = 22;
        public static int tektoDimension = 24;
        public static int moonDimension = 15;
        public static int minmusDimension = 21;
        public static int ikeDimension = 17;
        public static int dresDimension = 19;
        public static int mohoDimension = 20;
        public static int orbitDimension = 23;
        public static int thatmoDimension = 413025;
    }

    public static class MockDegradedSpaceConfig {

        public static int dunaDimension = 16;
        // Missing eveDimension, laytheDimension, etc.
    }

    @Before
    public void setUp() {
        TestEnvironment.resetAll();
    }

    @After
    public void tearDown() {
        TestEnvironment.resetAll();
    }

    @Test
    public void testAdapterReportModel() {
        AdapterReport repActive = AdapterReport.active("TestAdapter", "testmod", "Everything OK");
        assertEquals(AdapterStatus.ACTIVE, repActive.getStatus());
        assertFalse("Fallback must not be active", repActive.isFallbackActive());

        AdapterReport repDegraded = AdapterReport
            .degraded("TestAdapter", "testmod", "Field missing", "Static defaults");
        assertEquals(AdapterStatus.DEGRADED, repDegraded.getStatus());
        assertTrue("Fallback must be active", repDegraded.isFallbackActive());
        assertEquals("Static defaults", repDegraded.getFallbackDescription());

        AdapterReport repFailed = AdapterReport.failed("TestAdapter", "testmod", "NPE on link");
        assertEquals(AdapterStatus.FAILED, repFailed.getStatus());

        AdapterReport repNotInstalled = AdapterReport.notInstalled("TestAdapter", "testmod");
        assertEquals(AdapterStatus.NOT_INSTALLED, repNotInstalled.getStatus());
    }

    @Test
    public void testNotInstalledState() {
        TestEnvironment.setupVanilla();

        List<AdapterReport> reports = IntegrationManager.diagnoseAll();
        assertTrue("Must have at least 5 default adapters registered", reports.size() >= 5);

        for (AdapterReport report : reports) {
            assertEquals(
                "Adapter " + report.getAdapterName() + " must be NOT_INSTALLED when mod absent",
                AdapterStatus.NOT_INSTALLED,
                report.getStatus());
        }
    }

    @Test
    public void testHbmActiveState() {
        IntegrationManager.resetCache();
        HbmSpaceAdapter.resetForTesting();
        HbmSpaceAdapter.setSpaceConfigClassForTesting(MockFullSpaceConfig.class);
        IntegrationManager.setHbmLoaded(true);

        AdapterReport spaceReport = HbmSpaceAdapter.INSTANCE.diagnose();
        assertEquals(
            "SpaceAdapter must be ACTIVE with complete SpaceConfig",
            AdapterStatus.ACTIVE,
            spaceReport.getStatus());

        AdapterReport protReport = HbmProtectionAdapter.INSTANCE.diagnose();
        assertEquals(
            "ProtectionAdapter must be ACTIVE with real NTM in classpath",
            AdapterStatus.ACTIVE,
            protReport.getStatus());

        AdapterReport energyReport = HbmEnergyAdapter.INSTANCE.diagnose();
        assertEquals(
            "EnergyAdapter must be ACTIVE with real NTM in classpath",
            AdapterStatus.ACTIVE,
            energyReport.getStatus());
    }

    @Test
    public void testHbmDegradedState() {
        IntegrationManager.resetCache();
        IntegrationManager.setHbmLoaded(true);

        HbmSpaceAdapter.resetForTesting();
        HbmSpaceAdapter.setSpaceConfigClassForTesting(MockDegradedSpaceConfig.class);

        AdapterReport report = HbmSpaceAdapter.INSTANCE.diagnose();
        assertEquals("Must report DEGRADED status when fields are missing", AdapterStatus.DEGRADED, report.getStatus());
        assertTrue(
            "Fallback description must be present",
            report.getFallbackDescription()
                .contains("static defaults"));

        // Verify fallback resolution still returns static IDs
        assertEquals(18, ClimateEngine.resolveDimensionId("eve"));
        assertEquals(22, ClimateEngine.resolveDimensionId("laythe"));
    }

    @Test
    public void testCircuitBreakerTripping() {
        IntegrationManager.resetCache();
        IntegrationManager.setHbmLoaded(true);

        assertFalse(
            "Circuit breaker must be closed initially",
            IntegrationManager.isCircuitBreakerTripped("HBM Space Adapter"));

        // Trip circuit breaker
        IntegrationManager.tripCircuitBreaker("HBM Space Adapter", new RuntimeException("Simulated fatal corruption"));
        assertTrue("Circuit breaker must be tripped", IntegrationManager.isCircuitBreakerTripped("HBM Space Adapter"));

        // When diagnoseAll is called, the tripped adapter must report FAILED
        List<AdapterReport> reports = IntegrationManager.diagnoseAll();
        boolean foundFailed = false;
        for (AdapterReport r : reports) {
            if ("HBM Space Adapter".equals(r.getAdapterName())) {
                assertEquals(AdapterStatus.FAILED, r.getStatus());
                assertTrue(
                    r.getDetails()
                        .contains("Simulated fatal corruption"));
                foundFailed = true;
            }
        }
        assertTrue("Tripped adapter must be reported in diagnoseAll", foundFailed);
    }

    @Test
    public void testDiagnosticSummaryLogging() {
        IntegrationManager.resetCache();
        IntegrationManager.setHbmLoaded(true);
        IntegrationManager.setCoFHCoreLoaded(false);
        IntegrationManager.setDSurroundLoaded(false);

        List<AdapterReport> reports = IntegrationManager.diagnoseAll();
        for (AdapterReport r : reports) {
            String line = String.format(
                "  [%-13s] %-25s (%-10s) : %s",
                r.getStatus(),
                r.getAdapterName(),
                r.getTargetModId(),
                r.getDetails());
            assertTrue(line.length() > 0);
        }
    }
}
