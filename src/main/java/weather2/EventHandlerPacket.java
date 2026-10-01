package weather2;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;

import CoroUtil.packet.PacketHelper;
import CoroUtil.util.CoroUtilEntity;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.util.WeatherUtilConfig;

public class EventHandlerPacket {

    @SubscribeEvent
    public void onPacketFromServer(ClientCustomPacketEvent event) {
        try {
            NBTTagCompound ex = PacketHelper.readNBTTagCompound(event.packet.payload());
            String packetCommand = ex.getString("packetCommand");
            if (packetCommand.equals("WeatherData")) {
                ClientTickHandler.checkClientWeather();
                ClientTickHandler.weatherManager.nbtSyncFromServer(ex);
            } else if (packetCommand.equals("EZGuiData")) {
                String command = ex.getString("command");
                Weather.dbg("receiving GUI data for client, command: " + command);
                if (command.equals("syncUpdate")) {
                    WeatherUtilConfig.nbtReceiveServerDataForCache(ex);
                }
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }

    }

    @SubscribeEvent
    public void onPacketFromClient(ServerCustomPacketEvent event) {
        EntityPlayerMP entP = ((NetHandlerPlayServer) event.handler).playerEntity;

        try {
            NBTTagCompound ex = PacketHelper.readNBTTagCompound(event.packet.payload());
            String packetCommand = ex.getString("packetCommand");
            Weather.dbg("Weather2 packet command from client: " + packetCommand);
            if (packetCommand.equals("EZGuiData")) {
                String command = ex.getString("command");
                Weather.dbg("packet handling command: " + command);
                if (command.equals("syncRequest")) {
                    Weather.dbg("EZGUI syncRequest");
                    NBTTagCompound sendNBT = new NBTTagCompound();
                    sendNBT.setString("packetCommand", "EZGuiData");
                    sendNBT.setString("command", "syncUpdate");
                    sendNBT.setBoolean("markUpdated", true);
                    sendNBT.setBoolean(
                        "isPlayerOP",
                        MinecraftServer.getServer()
                            .isSinglePlayer()
                            || MinecraftServer.getServer()
                                .getConfigurationManager()
                                .func_152596_g(entP.getGameProfile()));
                    sendNBT.setTag("data", WeatherUtilConfig.nbtServerData);
                    sendNBT.setTag("dimListing", WeatherUtilConfig.createNBTDimensionListing());
                    Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(sendNBT, Weather.eventChannelName), entP);
                } else if (command.equals("applySettings") && (MinecraftServer.getServer()
                    .isSinglePlayer()
                    || MinecraftServer.getServer()
                        .getConfigurationManager()
                        .func_152596_g(entP.getGameProfile()))) {
                            WeatherUtilConfig.nbtReceiveClientData(ex.getCompoundTag("guiData"));
                        }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    @SideOnly(Side.CLIENT)
    public String getSelfUsername() {
        return CoroUtilEntity.getName(Minecraft.getMinecraft().thePlayer);
    }
}
