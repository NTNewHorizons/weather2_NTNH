package weather2.protection.policy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;

import weather2.climate.ClimateEngine;
import weather2.config.ConfigMisc;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;

/**
 * NTNH Deep Module: Terrain Landscape & Bedrock Protection Policy.
 * Prevents tornadoes from eroding bedrock (hardness < 0) and natural terrain (dirt, grass, sand, tree logs)
 * when RefinedGrabRules is enabled.
 *
 * Planetary High-Tier Erosion:
 * On hostile alien planets (dim != 0) during catastrophic storms (F4/F5, stage >= 8),
 * surface soil blocks (dirt, grass, sand, gravel) exposed to the sky (isAirBlock(x, y+1, z))
 * can experience slight erosion (~16.6% chance, throttled via spatial hash (x * 31 + z * 17 + y) % 6 == 0).
 * Buried terrain, tree trunks (BlockLog), and Overworld terrain remain 100% immune.
 */
public class TerrainProtectionPolicy implements BlockGrabPolicy {

    public static final int EROSION_MIN_STAGE = 8; // F4 (STATE_STAGE4 = 8, STATE_STAGE5 = 9)
    public static final int EROSION_MODULO = 6; // 1 in 6 (~16.6% erosion probability)

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
            if (block instanceof BlockLog) {
                return GrabDecision.DENY;
            }

            if (isSoilBlock(block)) {
                int dim = ctx.getDimensionId();
                // Overworld terrain is 100% immune
                if (dim == ClimateEngine.DIM_OVERWORLD) {
                    return GrabDecision.DENY;
                }

                // Planetary high-tier storms (F4/F5, stage >= 8): surface-only erosion with spatial hash throttle
                if (ctx.getStormStage() >= EROSION_MIN_STAGE) {
                    // Must be exposed to air on top (surface-only erosion, never underground drilling)
                    boolean isSurface = (ctx.world == null || ctx.world.isAirBlock(ctx.x, ctx.y + 1, ctx.z));
                    if (isSurface) {
                        int spatialHash = Math.abs(ctx.x * 31 + ctx.z * 17 + ctx.y);
                        if (spatialHash % EROSION_MODULO == 0) {
                            return GrabDecision.PASS;
                        }
                    }
                }

                // Buried soil, lower tier storms (< F4), or untargeted hash cells remain protected
                return GrabDecision.DENY;
            }
        }

        return GrabDecision.PASS;
    }

    public static boolean isSoilBlock(Block block) {
        return block == Blocks.dirt || block == Blocks.grass || block == Blocks.sand || block == Blocks.gravel;
    }
}
