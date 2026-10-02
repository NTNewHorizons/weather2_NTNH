package weather2.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.common.Loader;
import weather2.integration.cofh.CoFHEnergyAdapter;
import weather2.integration.dsurround.DSurroundWeatherAdapter;
import weather2.integration.hbm.HbmEnergyAdapter;
import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.integration.hbm.HbmSpaceAdapter;

/**
 * NTNH Deep Module: Central Integration Manager.
 * Provides centralized mod detection, soft-dependency discovery, diagnosable health status,
 * and circuit-breaker safety for external mod adapters.
 */
public class IntegrationManager {

    public static final String MODID_HBM = "hbm";
    public static final String MODID_COFH = "CoFHCore";
    public static final String MODID_DSURROUND = "dsurround";
    public static final String MODID_MINETWEAKER = "MineTweaker3";

    private static Boolean hbmLoaded = null;
    private static Boolean cofhLoaded = null;
    private static Boolean dsurroundLoaded = null;
    private static Boolean minetweakerLoaded = null;

    private static final List<DiagnosableAdapter> ADAPTERS = new ArrayList<DiagnosableAdapter>();
    private static final Map<String, String> CIRCUIT_BREAKER_FAILS = new ConcurrentHashMap<String, String>();
    private static boolean hasLoggedDiagnosticSummary = false;

    static {
        registerAdapter(HbmSpaceAdapter.INSTANCE);
        registerAdapter(HbmProtectionAdapter.INSTANCE);
        registerAdapter(HbmEnergyAdapter.INSTANCE);
        registerAdapter(CoFHEnergyAdapter.INSTANCE);
        registerAdapter(DSurroundWeatherAdapter.INSTANCE);
    }

    public static synchronized void registerAdapter(DiagnosableAdapter adapter) {
        if (adapter != null && !ADAPTERS.contains(adapter)) {
            ADAPTERS.add(adapter);
        }
    }

    public static boolean isModLoaded(String modid) {
        if (modid == null) return false;
        try {
            return Loader.isModLoaded(modid);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isHbmLoaded() {
        if (hbmLoaded == null) {
            hbmLoaded = isModLoaded(MODID_HBM);
        }
        return hbmLoaded;
    }

    public static boolean isCoFHCoreLoaded() {
        if (cofhLoaded == null) {
            cofhLoaded = isModLoaded(MODID_COFH);
        }
        return cofhLoaded;
    }

    public static boolean isDSurroundLoaded() {
        if (dsurroundLoaded == null) {
            dsurroundLoaded = isModLoaded(MODID_DSURROUND);
        }
        return dsurroundLoaded;
    }

    public static boolean isMineTweakerLoaded() {
        if (minetweakerLoaded == null) {
            minetweakerLoaded = isModLoaded(MODID_MINETWEAKER);
        }
        return minetweakerLoaded;
    }

    // Diagnostics & Health Reporting
    public static List<AdapterReport> diagnoseAll() {
        List<AdapterReport> reports = new ArrayList<AdapterReport>();
        for (DiagnosableAdapter adapter : ADAPTERS) {
            try {
                if (CIRCUIT_BREAKER_FAILS.containsKey(adapter.getAdapterName())) {
                    String reason = CIRCUIT_BREAKER_FAILS.get(adapter.getAdapterName());
                    reports.add(AdapterReport.failed(adapter.getAdapterName(), adapter.getTargetModId(), reason));
                } else {
                    reports.add(adapter.diagnose());
                }
            } catch (Throwable t) {
                reports.add(
                    AdapterReport.failed(
                        adapter.getAdapterName(),
                        adapter.getTargetModId(),
                        "Diagnosis threw " + t.getClass()
                            .getSimpleName() + ": " + t.getMessage()));
            }
        }
        return Collections.unmodifiableList(reports);
    }

    /**
     * Logs an ASCII summary of all integration adapter health statuses once at startup.
     */
    public static synchronized void logDiagnosticSummary() {
        if (hasLoggedDiagnosticSummary) return;
        hasLoggedDiagnosticSummary = true;

        List<AdapterReport> reports = diagnoseAll();
        System.out.println("[Weather2/NTNH] ==================== NTNH Integration Subsystems ====================");
        for (AdapterReport r : reports) {
            System.out.println("[Weather2/NTNH] " + r.toString());
        }
        System.out.println("[Weather2/NTNH] ======================================================================");
    }

    /**
     * Circuit breaker: records an unhandled runtime failure in an adapter to prevent server tick crashes.
     */
    public static void tripCircuitBreaker(String adapterName, Throwable t) {
        if (adapterName == null) return;
        String reason = (t != null ? t.getClass()
            .getSimpleName() + ": "
            + t.getMessage() : "Unknown runtime failure");
        if (CIRCUIT_BREAKER_FAILS.putIfAbsent(adapterName, reason) == null) {
            System.err.println(
                "[Weather2/NTNH] [WARN] Integration Circuit Breaker TRIPPED for " + adapterName
                    + "! Adapter degraded/disabled to prevent tick crashes. Details: "
                    + reason);
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    public static boolean isCircuitBreakerTripped(String adapterName) {
        return adapterName != null && CIRCUIT_BREAKER_FAILS.containsKey(adapterName);
    }

    public static void resetCircuitBreakers() {
        CIRCUIT_BREAKER_FAILS.clear();
    }

    // Testing and injection hooks
    public static void setHbmLoaded(Boolean loaded) {
        hbmLoaded = loaded;
    }

    public static void setCoFHCoreLoaded(Boolean loaded) {
        cofhLoaded = loaded;
    }

    public static void setDSurroundLoaded(Boolean loaded) {
        dsurroundLoaded = loaded;
    }

    public static void setMineTweakerLoaded(Boolean loaded) {
        minetweakerLoaded = loaded;
    }

    public static void resetCache() {
        hbmLoaded = null;
        cofhLoaded = null;
        dsurroundLoaded = null;
        minetweakerLoaded = null;
        hasLoggedDiagnosticSummary = false;
        CIRCUIT_BREAKER_FAILS.clear();
    }
}
