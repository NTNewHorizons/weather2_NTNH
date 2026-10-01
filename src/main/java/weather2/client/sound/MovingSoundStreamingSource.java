package weather2.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import cpw.mods.fml.client.FMLClientHandler;
import weather2.weathersystem.storm.StormObject;

public class MovingSoundStreamingSource extends MovingSound {

    private StormObject storm = null;
    public float cutOffRange = 128.0F;
    public Vec3 realSource = null;

    public MovingSoundStreamingSource(Vec3 parPos, ResourceLocation parRes, float parVolume, float parPitch,
        float parCutOffRange) {
        super(parRes);
        this.repeat = false;
        this.volume = parVolume;
        this.field_147663_c = parPitch;
        this.cutOffRange = parCutOffRange;
        this.realSource = parPos;
        this.update();
    }

    public MovingSoundStreamingSource(StormObject parStorm, ResourceLocation parRes, float parVolume, float parPitch,
        float parCutOffRange) {
        super(parRes);
        this.storm = parStorm;
        this.repeat = false;
        this.volume = parVolume;
        this.field_147663_c = parPitch;
        this.cutOffRange = parCutOffRange;
        this.update();
    }

    public void update() {
        EntityClientPlayerMP entP = FMLClientHandler.instance()
            .getClient().thePlayer;
        if (entP != null) {
            this.xPosF = (float) entP.posX;
            this.yPosF = (float) entP.posY;
            this.zPosF = (float) entP.posZ;
        }

        if (this.storm != null) {
            this.realSource = Vec3.createVectorHelper(
                this.storm.posGround.xCoord,
                this.storm.posGround.yCoord,
                this.storm.posGround.zCoord);
        }

        float var3 = (float) (((double) this.cutOffRange - (double) MathHelper.sqrt_double(
            this.getDistanceFrom(this.realSource, Vec3.createVectorHelper(entP.posX, entP.posY, entP.posZ))))
            / (double) this.cutOffRange);
        if (var3 < 0.0F) {
            var3 = 0.0F;
        }

        this.volume = var3;
    }

    public double getDistanceFrom(Vec3 source, Vec3 targ) {
        double d3 = source.xCoord - targ.xCoord;
        double d4 = source.yCoord - targ.yCoord;
        double d5 = source.zCoord - targ.zCoord;
        return d3 * d3 + d4 * d4 + d5 * d5;
    }
}
