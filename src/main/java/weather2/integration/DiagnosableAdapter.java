package weather2.integration;

/**
 * NTNH Deep Module: Contract for integration adapters that support self-diagnostics and health reporting.
 */
public interface DiagnosableAdapter {

    /**
     * Human-readable name of this integration adapter.
     */
    String getAdapterName();

    /**
     * Target mod ID this adapter interfaces with.
     */
    String getTargetModId();

    /**
     * Performs self-diagnosis and returns a structured health report.
     */
    AdapterReport diagnose();
}
