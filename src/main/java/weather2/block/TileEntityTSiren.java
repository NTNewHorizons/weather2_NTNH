package weather2.block;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.ClientTickHandler;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilSound;
import weather2.weathersystem.storm.StormObject;

public class TileEntityTSiren extends TileEntity {

    public long lastPlayTime = 0L;
    public long lastVolUpdate = 0L;
    public int lineBeingEdited = -1;

    public void updateEntity() {
        if (this.worldObj.isRemote) {
            this.tickClient();
        }

    }

    @SideOnly(Side.CLIENT)
    public void tickClient() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        if (this.lastPlayTime < System.currentTimeMillis()) {
            StormObject so = ClientTickHandler.weatherManager.getClosestStorm(
                Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord),
                ConfigMisc.sirenActivateDistance,
                StormObject.STATE_FORMING);
            if (so != null) {
                this.lastPlayTime = System.currentTimeMillis() + 13000L;
                WeatherUtilSound.playNonMovingSound(
                    Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord),
                    Weather.modID + ":streaming.siren",
                    1.0F,
                    1.0F,
                    120.0F);
            }
        }

    }

    public void writeToNBT(NBTTagCompound var1) {
        super.writeToNBT(var1);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);
    }
}
