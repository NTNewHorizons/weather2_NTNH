package weather2.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import weather2.ServerTickHandler;
import weather2.config.ConfigMisc;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

public class BlockTSensor extends Block {

    public BlockTSensor(int var1) {
        super(Material.clay);
        this.setTickRandomly(true);
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    public void randomDisplayTick(World var1, int var2, int var3, int var4, Random var5) {
        this.updateTick(var1, var2, var3, var4, var5);
    }

    public void updateTick(World var1, int var2, int var3, int var4, Random var5) {
        if (!var1.isRemote) {
            boolean enable = false;
            WeatherManagerServer wms = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                .get(Integer.valueOf(var1.provider.dimensionId));
            if (wms != null) {
                StormObject so = wms.getClosestStorm(
                    Vec3.createVectorHelper((double) var2, (double) var3, (double) var4),
                    ConfigMisc.sensorActivateDistance,
                    StormObject.STATE_FORMING);
                if (so != null) {
                    enable = true;
                }
            }

            if (enable) {
                var1.setBlockMetadataWithNotify(var2, var3, var4, 15, 2);
            } else {
                var1.setBlockMetadataWithNotify(var2, var3, var4, 0, 2);
            }

            var1.notifyBlocksOfNeighborChange(var2, var3 - 1, var4, this);
            var1.notifyBlocksOfNeighborChange(var2, var3 + 1, var4, this);
            var1.notifyBlocksOfNeighborChange(var2, var3, var4, this);
            var1.markBlockRangeForRenderUpdate(var2, var3, var4, var2, var3, var4);
            var1.scheduleBlockUpdate(var2, var3, var4, this, this.tickRate(var1));
        }
    }

    public int isProvidingStrongPower(IBlockAccess var1, int var2, int var3, int var4, int var5) {
        return var1.getBlockMetadata(var2, var3, var4) == 0 ? 0 : 15;
    }

    public int isProvidingWeakPower(IBlockAccess var1, int var2, int var3, int var4, int var5) {
        return var1.getBlockMetadata(var2, var3, var4) == 0 ? 0 : 15;
    }

    public boolean canProvidePower() {
        return true;
    }

    public boolean onBlockActivated(World p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_,
        EntityPlayer p_149727_5_, int p_149727_6_, float p_149727_7_, float p_149727_8_, float p_149727_9_) {
        p_149727_5_.setPosition(
            (double) ((float) p_149727_2_ + 0.5F),
            (double) ((float) p_149727_3_ + 1.5F),
            (double) ((float) p_149727_4_ + 0.5F));
        p_149727_5_.getEntityData()
            .setBoolean("inBedCustom", true);
        return super.onBlockActivated(
            p_149727_1_,
            p_149727_2_,
            p_149727_3_,
            p_149727_4_,
            p_149727_5_,
            p_149727_6_,
            p_149727_7_,
            p_149727_8_,
            p_149727_9_);
    }
}
