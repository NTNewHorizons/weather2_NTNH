package weather2.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.api.WindReader;
import weather2.util.WeatherUtilEntity;

public class TileEntityWindVane extends TileEntity {

    public float smoothAngle = 0.0F;
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
                float targetAngle = WindReader.getWindAngle(
                    this.worldObj,
                    Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord));
                float windSpeed = WindReader.getWindSpeed(
                    this.worldObj,
                    Vec3.createVectorHelper((double) this.xCoord, (double) this.yCoord, (double) this.zCoord));
                if (this.smoothAngle > 180.0F) {
                    this.smoothAngle -= 360.0F;
                }

                if (this.smoothAngle < -180.0F) {
                    this.smoothAngle += 360.0F;
                }

                float bestMove = MathHelper.wrapAngleTo180_float(targetAngle - this.smoothAngle);
                float diff = targetAngle + 360.0F + 180.0F - (this.smoothAngle + 360.0F + 180.0F);
                this.smoothAngleAdj = windSpeed;
                if (Math.abs(bestMove) < 180.0F) {
                    float realAdj = this.smoothAngleAdj;
                    if (realAdj * 2.0F > windSpeed) {
                        if (bestMove > 0.0F) {
                            this.smoothAngleRotationalVelAccel -= realAdj;
                        }

                        if (bestMove < 0.0F) {
                            this.smoothAngleRotationalVelAccel += realAdj;
                        }
                    }

                    if ((double) this.smoothAngleRotationalVelAccel > 0.3D
                        || (double) this.smoothAngleRotationalVelAccel < -0.3D) {
                        this.smoothAngle += this.smoothAngleRotationalVelAccel;
                    }

                    this.smoothAngleRotationalVelAccel *= 0.8F;
                }
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
