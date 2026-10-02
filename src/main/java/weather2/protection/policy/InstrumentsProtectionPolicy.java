package weather2.protection.policy;

import weather2.CommonProxy;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: Weather2 Instruments Protection Policy.
 * Guarantees absolute hardware immunity for weather stations, sensors, sirens, and deflectors.
 */
public class InstrumentsProtectionPolicy implements BlockGrabPolicy {

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        if (ctx.block == null) {
            return GrabDecision.DENY;
        }

        // Weather2 package check
        if (ctx.block.getClass()
            .getName()
            .startsWith("weather2.block.")) {
            return GrabDecision.DENY;
        }

        // CommonProxy block instances check
        if (ctx.block == CommonProxy.blockWeatherMachine || ctx.block == CommonProxy.blockWeatherDeflector) {
            return GrabDecision.DENY;
        }

        return GrabDecision.PASS;
    }
}
