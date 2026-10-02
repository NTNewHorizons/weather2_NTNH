package weather2.protection.policy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;

import weather2.config.ConfigMisc;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: Terrain Landscape & Bedrock Protection Policy.
 * Prevents tornadoes from eroding bedrock (hardness < 0) and natural terrain (dirt, grass, sand, tree logs)
 * when RefinedGrabRules is enabled.
 */
public class TerrainProtectionPolicy implements BlockGrabPolicy {

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        Block block = ctx.block;
        if (block == null) {
            return GrabDecision.DENY;
        }

        // Unbreakable blocks (Bedrock, forcefields, shielded blocks)
        if (ctx.getHardness() < 0.0F || block == Blocks.bedrock) {
            return GrabDecision.DENY;
        }

        // RefinedGrabRules protects planetary landscape and tree trunks
        if (ConfigMisc.Storm_Tornado_RefinedGrabRules) {
            if (block == Blocks.dirt || block == Blocks.grass || block == Blocks.sand || block instanceof BlockLog) {
                return GrabDecision.DENY;
            }
        }

        return GrabDecision.PASS;
    }
}
