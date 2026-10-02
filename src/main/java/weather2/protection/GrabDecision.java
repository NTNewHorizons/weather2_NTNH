package weather2.protection;

/**
 * NTNH Deep Module: Tri-state decision enum for BlockGrabPolicy.
 * - DENY: Vetoes block grabbing (protected, TileEntity, terrain, bedrock). Immediate rejection.
 * - ALLOW: Explicitly authorizes block grabbing (whitelisted debris, light clutter).
 * - PASS: Neutral evaluation, passes decision to the next policy in the chain.
 */
public enum GrabDecision {
    DENY,
    ALLOW,
    PASS
}
