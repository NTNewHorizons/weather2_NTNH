package weather2.integration;

/**
 * NTNH Deep Module: Status of an external mod integration adapter.
 */
public enum AdapterStatus {
    /**
     * Target mod is not installed in the environment (expected normal behavior, quiet bypass).
     */
    NOT_INSTALLED,

    /**
     * Target mod is installed and all reflection/interfaces/registries are 100% verified and functional.
     */
    ACTIVE,

    /**
     * Target mod is installed, but some optional bindings failed (e.g. field renamed, method missing).
     * Safe fallback is active. Details contain actionable diagnostic info.
     */
    DEGRADED,

    /**
     * Target mod is installed, but critical interface or binding threw a fatal error.
     * Adapter disabled via circuit breaker to prevent server crash.
     */
    FAILED,

    /**
     * Adapter is client-side only and intentionally bypassed on a Dedicated Server.
     */
    CLIENT_ONLY
}
