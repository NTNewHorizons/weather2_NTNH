package weather2.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystem;
import weather2.client.sound.MovingSoundStreamingSource;
import weather2.weathersystem.storm.StormObject;

public class WeatherUtilSound {

    @SideOnly(Side.CLIENT)
    public static SoundSystem sndSystem;
    public static String[] snd_dmg_close = new String[3];
    public static String[] snd_wind_close = new String[3];
    public static String[] snd_wind_far = new String[3];
    public static Map soundToLength = new HashMap();
    public static int[] snd_rand = new int[3];
    public static long[] soundTimer = new long[3];

    public static void init() {
        Random rand = new Random();
        snd_dmg_close[0] = "destruction_0_";
        snd_dmg_close[1] = "destruction_1_";
        snd_dmg_close[2] = "destruction_2_";
        snd_wind_close[0] = "wind_close_0_";
        snd_wind_close[1] = "wind_close_1_";
        snd_wind_close[2] = "wind_close_2_";
        snd_wind_far[0] = "wind_far_0_";
        snd_wind_far[1] = "wind_far_1_";
        snd_wind_far[2] = "wind_far_2_";
        snd_rand[0] = rand.nextInt(3);
        snd_rand[1] = rand.nextInt(3);
        snd_rand[2] = rand.nextInt(3);
        soundToLength.put(snd_dmg_close[0], Integer.valueOf(2515));
        soundToLength.put(snd_dmg_close[1], Integer.valueOf(2580));
        soundToLength.put(snd_dmg_close[2], Integer.valueOf(2741));
        soundToLength.put(snd_wind_close[0], Integer.valueOf(4698));
        soundToLength.put(snd_wind_close[1], Integer.valueOf(7324));
        soundToLength.put(snd_wind_close[2], Integer.valueOf(6426));
        soundToLength.put(snd_wind_far[0], Integer.valueOf(12892));
        soundToLength.put(snd_wind_far[1], Integer.valueOf(9653));
        soundToLength.put(snd_wind_far[2], Integer.valueOf(12003));
    }

    @SideOnly(Side.CLIENT)
    public static void playNonMovingSound(Vec3 parPos, String var1, float var5, float var6, float parCutOffRange) {
        String affix = ".ogg";
        ResourceLocation res = new ResourceLocation(var1);
        MovingSoundStreamingSource sound = new MovingSoundStreamingSource(parPos, res, var5, var6, parCutOffRange);
        FMLClientHandler.instance()
            .getClient()
            .getSoundHandler()
            .playSound(sound);
    }

    @SideOnly(Side.CLIENT)
    public static void playMovingSound(StormObject parStorm, String var1, float var5, float var6,
        float parCutOffRange) {
        String affix = ".ogg";
        ResourceLocation res = new ResourceLocation(var1);
        MovingSoundStreamingSource sound = new MovingSoundStreamingSource(parStorm, res, var5, var6, parCutOffRange);
        FMLClientHandler.instance()
            .getClient()
            .getSoundHandler()
            .playSound(sound);
    }

    private static URL getURLForSoundResource(final ResourceLocation p_148612_0_) {
        String s = String.format(
            "%s:%s:%s",
            new Object[] { "mcsounddomain", p_148612_0_.getResourceDomain(), p_148612_0_.getResourcePath() });
        URLStreamHandler urlstreamhandler = new URLStreamHandler() {

            protected URLConnection openConnection(final URL par1URL) {
                return new URLConnection(par1URL) {

                    public void connect() {}

                    public InputStream getInputStream() throws IOException {
                        return Minecraft.getMinecraft()
                            .getResourceManager()
                            .getResource(p_148612_0_)
                            .getInputStream();
                    }
                };
            }
        };

        try {
            return new URL((URL) null, s, urlstreamhandler);
        } catch (MalformedURLException var4) {
            throw new Error("TODO: Sanely handle url exception! :D");
        }
    }

}
