package weather2.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.client.FMLClientHandler;
import weather2.CommonProxy;
import weather2.config.ConfigMisc;

public class WeatherUtil {

    public static HashMap blockIDToUseMapping = new HashMap();

    public static boolean isPaused() {
        return FMLClientHandler.instance()
            .getClient()
            .isGamePaused();
    }

    public static boolean shouldGrabBlock(World parWorld, Block id) {
        try {
            ItemStack ex = new ItemStack(Items.diamond_axe);
            boolean result = true;
            if (ConfigMisc.Storm_Tornado_GrabCond_List) {
                try {
                    if (!ConfigMisc.Storm_Tornado_GrabListBlacklistMode) {
                        if (!((Boolean) blockIDToUseMapping.get(id)).booleanValue()) {
                            result = false;
                        }
                    } else if (((Boolean) blockIDToUseMapping.get(id)).booleanValue()) {
                        result = false;
                    }
                } catch (Exception var8) {
                    result = false;
                }
            } else {
                if (ConfigMisc.Storm_Tornado_GrabCond_StrengthGrabbing) {
                    float strMin = 0.0F;
                    float strMax = 0.74F;
                    if (id == null) {
                        result = false;
                        return result;
                    }

                    float strVsBlock = id.getBlockHardness(parWorld, 0, 0, 0) - (ex.func_150997_a(id) - 1.0F) / 4.0F;
                    if ((strVsBlock > strMax || strVsBlock < strMin) && id.getMaterial() != Material.wood
                        && id.getMaterial() != Material.cloth
                        && id.getMaterial() != Material.plants
                        && !(id instanceof BlockTallGrass)) {
                        result = false;
                    } else if (!safetyCheck(id)) {
                        result = false;
                    }
                }

                if (ConfigMisc.Storm_Tornado_RefinedGrabRules
                    && (id == Blocks.dirt || id == Blocks.grass || id == Blocks.sand || id instanceof BlockLog)) {
                    result = false;
                }
            }

            // NTNH start: immune machines
            if (id == CommonProxy.blockWeatherMachine || id == CommonProxy.blockWeatherDeflector) {
                result = false;
            }
            // NTNH end

            return result;
        } catch (Exception var9) {
            var9.printStackTrace();
            return false;
        }
    }

    public static boolean safetyCheck(Block id) {
        // NTNH start: feature-based block protection pipeline
        if (weather2.compat.WeatherNTNHHooks.isBlockProtected(id)) {
            return false;
        }
        // NTNH end
        return id != Blocks.bedrock && id != Blocks.log && id != Blocks.chest && id != Blocks.jukebox;
    }

    public static boolean shouldRemoveBlock(Block blockID) {
        return blockID.getMaterial() != Material.water;
    }

    public static boolean isOceanBlock(Block blockID) {
        return false;
    }

    public static boolean isSolidBlock(Block id) {
        return id == Blocks.stone || id == Blocks.cobblestone || id == Blocks.sandstone;
    }

    public static void doBlockList() {
        blockIDToUseMapping.clear();
        String[] splEnts = ConfigMisc.Storm_Tornado_GrabList.split(",");
        if (splEnts.length > 0) {
            for (int dbgShow = 0; dbgShow < splEnts.length; ++dbgShow) {
                splEnts[dbgShow] = splEnts[dbgShow].trim();
            }
        }

        boolean var10 = false;
        String dbg = "block list: ";
        blockIDToUseMapping.put(Blocks.air, Boolean.valueOf(false));
        Set set = Block.blockRegistry.getKeys();
        Iterator it = set.iterator();

        while (it.hasNext()) {
            String tagName = (String) it.next();
            Block block = (Block) Block.blockRegistry.getObject(tagName);
            if (var10) {
                System.out.println("??? " + Block.blockRegistry.getNameForObject(block));
            }

            if (block != null) {
                boolean foundEnt = false;
                int j = 0;

                while (true) {
                    if (j < splEnts.length) {
                        label38: {
                            if (ConfigMisc.Storm_Tornado_GrabCond_List_PartialMatches) {
                                if (tagName.contains(splEnts[j])) {
                                    dbg = dbg + Block.blockRegistry.getNameForObject(block) + ", ";
                                    foundEnt = true;
                                    break label38;
                                }
                            } else {
                                Block blockEntry = (Block) Block.blockRegistry.getObject(splEnts[j]);
                                if (blockEntry != null && block == blockEntry) {
                                    foundEnt = true;
                                    dbg = dbg + Block.blockRegistry.getNameForObject(block) + ", ";
                                    break label38;
                                }
                            }

                            ++j;
                            continue;
                        }
                    }

                    blockIDToUseMapping.put(block, Boolean.valueOf(foundEnt));
                    break;
                }
            }
        }

        if (var10) {
            System.out.println(dbg);
        }

    }

}
