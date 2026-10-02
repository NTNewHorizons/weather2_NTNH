package weather2.harness;

import weather2.deflector.DeflectorState;
import weather2.deflector.IDeflectorTE;

/**
 * Headless mock implementation of TileEntityWeatherDeflector for energy and escalation tests.
 */
public class MockDeflector implements IDeflectorTE {

    public long power = 0;
    public boolean isFieldActive = false;
    public int blackoutCooldown = 0;

    // Obfuscated Forge 1.7.10 TileEntity field names for reflection compatibility
    public Object field_145850_b = null; // worldObj
    public int field_145851_c = 100; // xCoord
    public int field_145848_d = 64; // yCoord
    public int field_145849_e = 200; // zCoord

    private DeflectorState state = DeflectorState.OFFLINE;
    private int stateTimer = 0;

    public MockDeflector() {
        this(0L, false);
    }

    public MockDeflector(long initialPower, boolean active) {
        this.power = initialPower;
        this.isFieldActive = active;
        this.state = active ? DeflectorState.ACTIVE : DeflectorState.OFFLINE;
    }

    @Override
    public DeflectorState getState() {
        // Harmonize with isFieldActive flag when set directly in tests
        if (isFieldActive && !state.isFieldActive()) {
            return DeflectorState.ACTIVE;
        }
        if (!isFieldActive && state.isFieldActive()) {
            return DeflectorState.OFFLINE;
        }
        return state;
    }

    @Override
    public int getStateTimer() {
        return stateTimer;
    }

    @Override
    public void setStateTimer(int timer) {
        this.stateTimer = timer;
        this.blackoutCooldown = timer;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public void transitionTo(DeflectorState newState, int timer) {
        if (newState == null) newState = DeflectorState.OFFLINE;
        this.state = newState;
        this.stateTimer = timer;
        this.isFieldActive = newState.isFieldActive();
        this.blackoutCooldown = (newState == DeflectorState.BLACKOUT_DEPLETED
            || newState == DeflectorState.OVERLOAD_COLLAPSED) ? timer : 0;
    }

    @Override
    public Object getDeflectorWorld() {
        return field_145850_b;
    }

    @Override
    public int getDeflectorX() {
        return field_145851_c;
    }

    @Override
    public int getDeflectorY() {
        return field_145848_d;
    }

    @Override
    public int getDeflectorZ() {
        return field_145849_e;
    }
}
