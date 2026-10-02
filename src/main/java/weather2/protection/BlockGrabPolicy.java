package weather2.protection;

/**
 * NTNH Deep Module: Block Grab Decision Policy.
 * Strategy pattern interface for modular block grab decision rules.
 */
public interface BlockGrabPolicy {

    /**
     * Evaluates a potential block grab attempt.
     * 
     * @param ctx Spatial, ontological, and thermodynamic context of the block and storm
     * @return DENY to veto immediately, ALLOW to permit immediately, or PASS to defer to next policy
     */
    GrabDecision evaluate(BlockContext ctx);
}
