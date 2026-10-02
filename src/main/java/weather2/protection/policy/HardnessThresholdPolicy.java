package weather2.protection.policy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import weather2.config.ConfigMisc;
import weather2.protection.BlockContext;
import weather2.protection.BlockGrabPolicy;
import weather2.protection.GrabDecision;
import weather2.util.WeatherUtil;

/**
 * NTNH Deep Module: Hardness & Material Threshold Grab Policy.
 * Evaluates structural hardness and material vulnerability against storm forces.
 * Lightweight materials (wood, cloth, plants, glass, hardness <= 0.74F) are permitted to be grabbed.
 * Heavy building materials (stone, iron, concrete, steel, hardness > 0.74F) are denied.
 */
public class HardnessThresholdPolicy implements BlockGrabPolicy {

    public static final float MIN_HARDNESS = 0.0F;
    public static final float MAX_EFFECTIVE_STRENGTH = 0.74F;

    private static ItemStack getAxeRef() {
        try {
            if (Items.diamond_axe != null) {
                return new ItemStack(Items.diamond_axe);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public GrabDecision evaluate(BlockContext ctx) {
        Block block = ctx.block;
        if (block == null) {
            return GrabDecision.DENY;
        }

        // 1. Configured Grab List mode
        if (ConfigMisc.Storm_Tornado_GrabCond_List) {
            try {
                Boolean inList = (Boolean) WeatherUtil.blockIDToUseMapping.get(block);
                boolean matches = inList != null && inList.booleanValue();
                if (!ConfigMisc.Storm_Tornado_GrabListBlacklistMode) {
                    return matches ? GrabDecision.ALLOW : GrabDecision.DENY;
                } else {
                    return matches ? GrabDecision.DENY : GrabDecision.ALLOW;
                }
            } catch (Throwable t) {
                return GrabDecision.DENY;
            }
        }

        // 2. Strength & Material Grabbing mode
        if (ConfigMisc.Storm_Tornado_GrabCond_StrengthGrabbing) {
            Material mat = block.getMaterial();
            if (mat == Material.wood || mat == Material.cloth
                || mat == Material.plants
                || block instanceof BlockTallGrass) {
                return GrabDecision.ALLOW;
            }

            float hardness = ctx.getHardness();
            float axeSpeed = 1.0F;
            ItemStack axe = getAxeRef();
            if (axe != null) {
                try {
                    axeSpeed = axe.func_150997_a(block);
                } catch (Throwable ignored) {}
            }

            float strVsBlock = hardness - (axeSpeed - 1.0F) / 4.0F;
            if (strVsBlock >= MIN_HARDNESS && strVsBlock <= MAX_EFFECTIVE_STRENGTH) {
                return GrabDecision.ALLOW;
            } else {
                return GrabDecision.DENY;
            }
        }

        // Deny-by-default
        return GrabDecision.DENY;
    }
}
