package weather2.weathersystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManager;

import weather2.config.ConfigMisc;

public class EntityRendererProxyWeather2Mini extends EntityRenderer {

    public EntityRendererProxyWeather2Mini(Minecraft var1, IResourceManager resMan) {
        super(var1, resMan);
    }

    protected void renderRainSnow(float par1) {
        boolean overrideOn = ConfigMisc.Misc_proxyRenderOverrideEnabled;
        if (!overrideOn) {
            super.renderRainSnow(par1);
        } else {
            if (!ConfigMisc.Particle_RainSnow) {
                super.renderRainSnow(par1);
            }

        }
    }
}
