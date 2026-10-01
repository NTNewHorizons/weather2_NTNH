package weather2.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import CoroUtil.api.weather.IWindHandler;
import CoroUtil.componentAI.ICoroAI;
import CoroUtil.entity.EntityThrowableUsefull;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntityIceBall extends EntityThrowableUsefull implements IWindHandler {

    public int ticksInAir;
    @SideOnly(Side.CLIENT)
    public boolean hasDeathTicked;

    public EntityIceBall(World world) {
        super(world);
    }

    public EntityIceBall(World world, EntityLivingBase entityliving) {
        super(world, entityliving);
        float speed = 0.7F;
        float f = 0.4F;
        this.motionX = (double) (-MathHelper.sin(-this.rotationYaw / 180.0F * 3.1415927F)
            * MathHelper.cos(-this.rotationPitch / 180.0F * 3.1415927F)
            * f);
        this.motionZ = (double) (MathHelper.cos(-this.rotationYaw / 180.0F * 3.1415927F)
            * MathHelper.cos(-this.rotationPitch / 180.0F * 3.1415927F)
            * f);
        this.motionY = (double) (-MathHelper.sin((-this.rotationPitch + this.func_70183_g()) / 180.0F * 3.1415927F)
            * f);
        this.setThrowableHeading(this.motionX, this.motionY, this.motionZ, speed, 1.0F);
    }

    public EntityIceBall(World world, double d, double d1, double d2) {
        super(world, d, d1, d2);
    }

    public void onUpdate() {
        super.onUpdate();
        this.motionY -= 0.10000000149011612D;
        if (this.motionY <= -3.0D) {
            this.motionY = -3.0D;
        }

        if (!this.worldObj.isRemote) {
            ++this.ticksInAir;
            if (this.isCollided) {
                this.setDead();
            }

            if (this.ticksInAir > 120) {
                this.setDead();
            }

            if (this.worldObj.getClosestPlayer(this.posX, 50.0D, this.posZ, 80.0D) == null) {
                this.setDead();
            }
        } else {
            this.tickAnimate();
        }

    }

    protected float getGravityVelocity() {
        return 0.0F;
    }

    public MovingObjectPosition tickEntityCollision(Vec3 vec3, Vec3 vec31) {
        MovingObjectPosition movingobjectposition = null;
        Entity entity = null;
        List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(
            this,
            this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ)
                .expand(0.5D, 1.0D, 0.5D));
        double d0 = 0.0D;
        EntityLivingBase entityliving = this.getThrower();

        for (int j = 0; j < list.size(); ++j) {
            Entity entity1 = (Entity) list.get(j);
            if (entity1.canBeCollidedWith() && entity1 != entityliving && this.ticksInAir >= 4) {
                entity = entity1;
                break;
            }
        }

        if (entity != null) {
            movingobjectposition = new MovingObjectPosition(entity);
        }

        return movingobjectposition;
    }

    protected void onImpact(MovingObjectPosition movingobjectposition) {
        if (movingobjectposition.entityHit != null && !this.worldObj.isRemote) {
            byte damage = 5;
            if (movingobjectposition.entityHit instanceof ICoroAI && this.getThrower() instanceof ICoroAI) {
                if (((ICoroAI) this.getThrower()).getAIAgent().dipl_info
                    != ((ICoroAI) movingobjectposition.entityHit).getAIAgent().dipl_info) {
                    movingobjectposition.entityHit
                        .attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), (float) damage);
                }
            } else {
                movingobjectposition.entityHit.attackEntityFrom(DamageSource.fallingBlock, (float) damage);
            }

            if (!this.worldObj.isRemote) {
                this.setDead();
            }
        }

        if (!this.worldObj.isRemote) {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "step.stone", 3.0F, 5.0F);
            this.setDead();
        } else {
            this.tickDeath();
        }

    }

    public void setDead() {
        if (this.worldObj.isRemote) {
            this.tickDeath();
        }

        super.setDead();
    }

    @SideOnly(Side.CLIENT)
    public void tickAnimate() {}

    @SideOnly(Side.CLIENT)
    public void tickDeath() {
        if (!this.hasDeathTicked) {
            this.hasDeathTicked = true;
        }

    }

    public float getWindWeight() {
        return 4.0F;
    }

    public int getParticleDecayExtra() {
        return 0;
    }
}
