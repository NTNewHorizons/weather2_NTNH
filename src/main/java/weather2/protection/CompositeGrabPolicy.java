package weather2.protection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NTNH Deep Module: Composite Block Grab Policy.
 * Executes a Chain of Responsibility over registered BlockGrabPolicy rules.
 * Preserves the Deny-by-Default invariant: if no policy explicitly ALLOWs, returns false.
 */
public class CompositeGrabPolicy implements BlockGrabPolicy {

    private final List<BlockGrabPolicy> policies = new ArrayList<BlockGrabPolicy>();

    public CompositeGrabPolicy addPolicy(BlockGrabPolicy policy) {
        if (policy != null) {
            policies.add(policy);
        }
        return this;
    }

    public List<BlockGrabPolicy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        if (ctx == null) return GrabDecision.DENY;
        for (int i = 0; i < policies.size(); i++) {
            BlockGrabPolicy policy = policies.get(i);
            GrabDecision decision = policy.evaluate(ctx);
            if (decision == GrabDecision.DENY) {
                return GrabDecision.DENY;
            }
            if (decision == GrabDecision.ALLOW) {
                return GrabDecision.ALLOW;
            }
            // PASS -> continue evaluating subsequent policies
        }
        return GrabDecision.PASS;
    }

    /**
     * Evaluates the policy chain and returns a boolean result:
     * true if ALLOWed, false if DENYed or default PASS (Deny-by-Default).
     */
    public boolean canGrab(BlockContext ctx) {
        if (ctx == null) return false;
        GrabDecision decision = evaluate(ctx);
        return decision == GrabDecision.ALLOW;
    }
}
