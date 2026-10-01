package weather2.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import weather2.ClientTickHandler;
import weather2.weathersystem.storm.StormObject;

public class TileEntityWeatherForecast extends TileEntity {

    public float smoothAngle = 0.0F;
    public float smoothSpeed = 0.0F;
    public float smoothAngleRotationalVel = 0.0F;
    public float smoothAngleRotationalVelAccel = 0.0F;
    public float smoothAngleAdj = 0.1F;
    public float smoothSpeedAdj = 0.1F;
    public StormObject lastTickStormObject = null;
    public List storms = new ArrayList();

    public void updateEntity() {
        if (this.worldObj.isRemote && this.worldObj.getTotalWorldTime() % 200L == 0L) {
            this.lastTickStormObject = ClientTickHandler.weatherManager.getClosestStorm(
                Vec3.createVectorHelper(
                    (double) this.xCoord,
                    (double) ((Integer) StormObject.layers.get(0)).intValue(),
                    (double) this.zCoord),
                1024.0D,
                StormObject.STATE_THUNDER,
                true);
            this.storms = ClientTickHandler.weatherManager.getStormsAround(
                Vec3.createVectorHelper(
                    (double) this.xCoord,
                    (double) ((Integer) StormObject.layers.get(0)).intValue(),
                    (double) this.zCoord),
                1024.0D);
        }

    }

    public void writeToNBT(NBTTagCompound var1) {
        super.writeToNBT(var1);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);
    }
}
