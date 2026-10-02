package weather2.integration.cofh;

import weather2.deflector.IDeflectorTE;

/**
 * NTNH Deep Module: CoFH Redstone Flux (RF) Energy Adapter.
 * Bridges Redstone Flux (RF) energy grid to HBM High Energy (HE) storage.
 * Enforces the standard NTNH conversion ratio: 1 HE = 4 RF.
 */
public class CoFHEnergyAdapter {

    public static final int RF_PER_HE = 4;

    /**
     * Converts HE energy amount to RF equivalent, capped at Integer.MAX_VALUE.
     */
    public static int heToRf(long he) {
        if (he <= 0) return 0;
        long rf = he * RF_PER_HE;
        return (int) Math.min(Integer.MAX_VALUE, rf);
    }

    /**
     * Converts RF energy amount to HE equivalent (truncated).
     */
    public static long rfToHe(int rf) {
        if (rf <= 0) return 0;
        return rf / RF_PER_HE;
    }

    /**
     * Calculates the amount of RF energy that can be accepted into an HE energy buffer.
     */
    public static int calculateAcceptedRF(long currentHE, long maxHE, int maxReceiveRF) {
        if (maxReceiveRF <= 0 || currentHE >= maxHE) return 0;
        long heEquivalent = maxReceiveRF / RF_PER_HE;
        long acceptedHE = Math.min(maxHE - currentHE, heEquivalent);
        return (int) (acceptedHE * RF_PER_HE);
    }

    /**
     * Receives RF energy and applies it directly to an IDeflectorTE instance.
     */
    public static int receiveEnergy(IDeflectorTE deflector, long maxHE, int maxReceiveRF, boolean simulate) {
        if (deflector == null || maxReceiveRF <= 0) return 0;
        long currentHE = deflector.getPower();
        int acceptedRF = calculateAcceptedRF(currentHE, maxHE, maxReceiveRF);
        if (!simulate && acceptedRF > 0) {
            deflector.setPower(currentHE + rfToHe(acceptedRF));
        }
        return acceptedRF;
    }

    /**
     * Returns the RF representation of currently stored HE power.
     */
    public static int getEnergyStored(long currentHE) {
        return heToRf(currentHE);
    }

    /**
     * Returns the RF representation of maximum HE power capacity.
     */
    public static int getMaxEnergyStored(long maxHE) {
        return heToRf(maxHE);
    }
}
