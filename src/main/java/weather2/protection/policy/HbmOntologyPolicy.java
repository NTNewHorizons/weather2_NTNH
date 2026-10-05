package weather2.protection.policy;

import weather2.integration.hbm.HbmProtectionAdapter;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: HBM NTM Block Protection Policy.
 * Protects industrial machines, nuclear reactors, hazards, and cables from HBM NTM.
 * Explicitly allows grabbing for whitelisted planetary clutter (waste, ash, scrap, meteor rubble).
 */
public class HbmOntologyPolicy implements BlockGrabPolicy {

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        if (ctx.block == null) {
            return GrabDecision.DENY;
        }

        Class<?> clazz = ctx.block.getClass();
        String regName = ctx.getRegistryName();

        if (HbmProtectionAdapter.isHbmBlock(clazz, regName)) {
            if (ctx.getDimensionId() != weather2.climate.ClimateEngine.DIM_OVERWORLD
                && HbmProtectionAdapter.isHbmBlockWhitelisted(regName)) {
                return GrabDecision.ALLOW; // Whitelisted planetary debris can be grabbed on hostile planets only
            }
            return GrabDecision.DENY; // All other HBM blocks (and all HBM blocks in Overworld) are strictly protected
        }

        return GrabDecision.PASS;
    }
}
