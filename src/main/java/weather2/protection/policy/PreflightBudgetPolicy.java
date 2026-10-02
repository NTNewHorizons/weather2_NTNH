package weather2.protection.policy;

import weather2.climate.ClimateEngine;
import weather2.config.ConfigMisc;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.BlockProtectionPipeline;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: Preflight & Entity Budget Grab Policy.
 * Validates global grab permission, planetary dimension whitelist,
 * and O(1) moving blocks per dimension entity budget.
 */
public class PreflightBudgetPolicy implements BlockGrabPolicy {

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        if (!ConfigMisc.Storm_Tornado_grabBlocks) {
            return GrabDecision.DENY;
        }

        int dim = ctx.getDimensionId();
        if (!ClimateEngine.isDimensionGrabAllowed(dim)) {
            return GrabDecision.DENY;
        }

        // Cap on EntityMovingBlock per dimension to protect server TPS
        int currentBlocks = BlockProtectionPipeline.getMovingBlocksCount(dim);
        int maxBlocks = BlockProtectionPipeline.getMaxMovingBlocksPerDimension();
        if (currentBlocks >= maxBlocks) {
            return GrabDecision.DENY;
        }

        return GrabDecision.PASS;
    }
}
