package weather2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.client.SceneEnhancer;

public class EventHandlerFML {

    public static World lastWorld = null;

    @SubscribeEvent
    public void tickWorld(WorldTickEvent event) {
        if (event.phase == Phase.START) {
            ;
        }

    }

    @SubscribeEvent
    public void tickServer(ServerTickEvent event) {
        if (event.phase == Phase.START) {
            ServerTickHandler.onTickInGame();
        }

    }

    @SubscribeEvent
    public void tickClient(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            ClientProxy.clientTickHandler.onTickInGame();
        }

    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void tickRenderScreen(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            ClientProxy.clientTickHandler.onRenderScreenTick();
        } else if (event.phase == Phase.START) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null && mc.theWorld != null) {
                EntityClientPlayerMP entP = mc.thePlayer;
                float curRainStr = SceneEnhancer.getRainStrengthAndControlVisuals(entP, true);
                curRainStr = Math.abs(curRainStr);
                mc.theWorld.setRainStrength(curRainStr);
            }
        }

    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerLoggedInEvent event) {
        ServerTickHandler.playerJoinedServerSyncFull((EntityPlayerMP) event.player);
    }

}
