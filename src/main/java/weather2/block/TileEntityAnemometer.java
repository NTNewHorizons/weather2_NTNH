package weather2.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.api.WindReader;
import weather2.util.WeatherUtilEntity;

public class TileEntityAnemometer extends TileEntity {

    public float smoothAngle = 0.0F;
    public float smoothAnglePrev = 0.0F;
    public float smoothSpeed = 0.0F;
    public float smoothAngleRotationalVel = 0.0F;
    public float smoothAngleRotationalVelAccel = 0.0F;
    public float smoothAngleAdj = 0.1F;
    public float smoothSpeedAdj = 0.1F;
    public boolean isOutsideCached = false;

    public void updateEntity() {
        if (this.worldObj.isRemote) {
            if (this.worldObj.getTotalWorldTime() % 40L == 0L) {
                this.isOutsideCached = WeatherUtilEntity.isPosOutside(
                    this.worldObj,
                    Vec3.createVectorHelper(
                        (double) ((float) this.xCoord + 0.5F),
                        (double) ((float) this.yCoord + 0.5F),
                        (double) ((float) this.zCoord + 0.5F)));
            }

            if (this.isOutsideCached) {
                WindReader.getWindAngle(
                    this.worldObj,
                    Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord));
                float windSpeed = WindReader.getWindSpeed(
                    this.worldObj,
                    Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord));
                this.smoothAngleRotationalVel += windSpeed * 1.0F;
                if (this.smoothAngleRotationalVel > 50.0F) {
                    this.smoothAngleRotationalVel = 50.0F;
                }

                if (this.smoothAngle >= 180.0F) {
                    this.smoothAngle -= 360.0F;
                }

                if (this.smoothAnglePrev >= 180.0F) {
                    this.smoothAnglePrev -= 360.0F;
                }
            }

            this.smoothAnglePrev = this.smoothAngle;
            this.smoothAngle += this.smoothAngleRotationalVel;
            this.smoothAngleRotationalVel -= 0.1F;
            this.smoothAngleRotationalVel *= 0.97F;
            if (this.smoothAngleRotationalVel <= 0.0F) {
                this.smoothAngleRotationalVel = 0.0F;
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(
            (double) this.xCoord,
            (double) this.yCoord,
            (double) this.zCoord,
            (double) (this.xCoord + 1),
            (double) (this.yCoord + 3),
            (double) (this.zCoord + 1));
    }

    public void writeToNBT(NBTTagCompound var1) {
        super.writeToNBT(var1);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);
    }
}
