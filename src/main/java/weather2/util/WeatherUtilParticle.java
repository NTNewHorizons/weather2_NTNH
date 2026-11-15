package weather2.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.Vec3;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.EntityTexFX;

public class WeatherUtilParticle {

    public static List[] fxLayers;
    public static int effLeafID = 0;
    public static int effRainID = 1;
    public static int effWindID = 2;
    public static int effSnowID = 3;
    public static Random rand = new Random();
    public static int maxRainDrops = 80;
    public static Vec3[] rainPositions = new Vec3[maxRainDrops];

    private static Field particleAgeField;
    private static boolean particleAgeFieldResolved = false;

    private static Field getParticleAgeField() {
        if (!particleAgeFieldResolved) {
            particleAgeFieldResolved = true;
            try {
                particleAgeField = EntityFX.class.getDeclaredField("field_70546_d");
                particleAgeField.setAccessible(true);
            } catch (Throwable t1) {
                try {
                    particleAgeField = EntityFX.class.getDeclaredField("particleAge");
                    particleAgeField.setAccessible(true);
                } catch (Throwable ignored) {}
            }
        }
        return particleAgeField;
    }

    public static int getParticleAge(EntityFX ent) {
        if (ent instanceof EntityRotFX) {
            return ((EntityRotFX) ent).getAge();
        }
        Field f = getParticleAgeField();
        if (f != null && ent != null) {
            try {
                return f.getInt(ent);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static void setParticleAge(EntityFX ent, int val) {
        if (ent instanceof EntityRotFX) {
            ((EntityRotFX) ent).setAge(val);
            return;
        }
        Field f = getParticleAgeField();
        if (f != null && ent != null) {
            try {
                f.setInt(ent, val);
            } catch (Throwable ignored) {}
        }
    }

    @SideOnly(Side.CLIENT)
    public static void getFXLayers() {
        Field field = null;

        try {
            field = EffectRenderer.class.getDeclaredField("field_78876_b");
            field.setAccessible(true);
            fxLayers = (List[]) ((List[]) field.get(
                FMLClientHandler.instance()
                    .getClient().effectRenderer));
        } catch (Exception var4) {
            try {
                field = EffectRenderer.class.getDeclaredField("fxLayers");
                field.setAccessible(true);
                fxLayers = (List[]) ((List[]) field.get(
                    FMLClientHandler.instance()
                        .getClient().effectRenderer));
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public static float getParticleWeight(EntityRotFX entity1) {
        return entity1 instanceof EntityTexFX ? 5.0F + (float) entity1.getAge() / 200.0F
            : (entity1 instanceof EntityFX ? 5.0F + (float) entity1.getAge() / 200.0F : -1.0F);
    }

    static {
        float range = 20.0F;

        for (int i = 0; i < maxRainDrops; ++i) {
            rainPositions[i] = Vec3.createVectorHelper(
                (double) (rand.nextFloat() * range - range / 2.0F),
                (double) (rand.nextFloat() * range / 16.0F - range / 32.0F),
                (double) (rand.nextFloat() * range - range / 2.0F));
        }

    }
}
