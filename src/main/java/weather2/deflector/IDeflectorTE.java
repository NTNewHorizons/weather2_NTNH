package weather2.deflector;

/**
 * Decoupled interface for Weather Deflector TileEntity.
 * Eliminates soft-dependency classloading collisions with @Optional HBM/CoFH interfaces.
 */
public interface IDeflectorTE {

    DeflectorState getState();

    int getStateTimer();

    void setStateTimer(int timer);

    long getPower();

    void setPower(long power);

    void transitionTo(DeflectorState newState, int timer);

    Object getDeflectorWorld();

    int getDeflectorX();

    int getDeflectorY();

    int getDeflectorZ();
}
