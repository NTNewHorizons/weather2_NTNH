package weather2.deflector;

/**
 * NTNH Finite State Machine: Weather Deflector Operational States.
 *
 * Defines explicit deterministic states for the planetary deflector:
 * - OFFLINE (0): Depleted or unpowered (< 25 kHE). Field collapsed.
 * - ACTIVE (15): Nominal operation (25 kHE/t idle drain). Field active (150 blocks radius).
 * - DISSIPATING (12): High-energy EMP pulse neutralizing storm F0-F4 (100k..50M HE).
 * - BLACKOUT_DEPLETED (6): Insufficient energy during defense. 200 ticks (10s) lockout.
 * - OVERLOAD_COLLAPSED (1): Catastrophic failure against F5/C5 storm. 300 ticks (15s) lockout.
 */
public enum DeflectorState {

    OFFLINE(0, 0, false, "Offline"),
    ACTIVE(1, 15, true, "Active"),
    DISSIPATING(2, 12, true, "Dissipating"),
    BLACKOUT_DEPLETED(3, 6, false, "Blackout (Depleted)"),
    OVERLOAD_COLLAPSED(4, 1, false, "Overload (Collapsed)");

    private final int id;
    private final int comparatorSignal;
    private final boolean fieldActive;
    private final String displayName;

    DeflectorState(int id, int comparatorSignal, boolean fieldActive, String displayName) {
        this.id = id;
        this.comparatorSignal = comparatorSignal;
        this.fieldActive = fieldActive;
        this.displayName = displayName;
    }

    public int getId() {
        return this.id;
    }

    public int getComparatorSignal() {
        return this.comparatorSignal;
    }

    public boolean isFieldActive() {
        return this.fieldActive;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static DeflectorState fromId(int id) {
        for (DeflectorState s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        return OFFLINE;
    }
}
