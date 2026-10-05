package weather2.weathersystem;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.ClientTickHandler;
import weather2.Weather;
import weather2.entity.EntityLightningBolt;
import weather2.volcano.VolcanoObject;
import weather2.weathersystem.storm.StormObject;

@SideOnly(Side.CLIENT)
public class WeatherManagerClient extends WeatherManagerBase {

    public WeatherManagerClient(int parDim) {
        super(parDim);
    }

    public World getWorld() {
        return FMLClientHandler.instance()
            .getClient().theWorld;
    }

    public void tick() {
        super.tick();
    }

    public void nbtSyncFromServer(NBTTagCompound parNBT) {
        String command = parNBT.getString("command");
        NBTTagCompound nbt;
        if (command.equals("syncStormNew")) {
            nbt = parNBT.getCompoundTag("data");
            StormObject posXS = new StormObject(ClientTickHandler.weatherManager);
            posXS.nbtSyncFromServer(nbt);
            this.addStormObject(posXS);
        } else {
            long var15;
            if (command.equals("syncStormRemove")) {
                nbt = parNBT.getCompoundTag("data");
                var15 = nbt.getLong("ID");
                StormObject posZS = (StormObject) this.lookupStormObjectsByID.get(Long.valueOf(var15));
                if (posZS != null) {
                    this.removeStormObject(var15);
                } else {
                    Weather.dbg("error removing storm, cant find by ID: " + var15);
                }
            } else if (command.equals("syncStormUpdate")) {
                for (int var14 = 0; var14 < parNBT.getInteger("stormCount"); ++var14) {
                    NBTTagCompound var16 = parNBT.getCompoundTag("storm" + var14);
                    long posYS = var16.getLong("ID");
                    StormObject posX = (StormObject) this.lookupStormObjectsByID.get(Long.valueOf(posYS));
                    if (posX != null) {
                        posX.nbtSyncFromServer(var16);
                    } else {
                        Weather.dbg("error syncing storm, cant find by ID: " + posYS);
                    }
                }
            } else if (command.equals("syncVolcanoNew")) {
                Weather.dbg("creating client side volcano");
                nbt = parNBT.getCompoundTag("data");
                VolcanoObject var17 = new VolcanoObject(ClientTickHandler.weatherManager);
                var17.nbtSyncFromServer(nbt);
                this.addVolcanoObject(var17);
            } else {
                VolcanoObject var20;
                if (command.equals("syncVolcanoRemove")) {
                    Weather.dbg("removing client side volcano");
                    nbt = parNBT.getCompoundTag("data");
                    var15 = nbt.getLong("ID");
                    var20 = (VolcanoObject) this.lookupVolcanoes.get(Long.valueOf(var15));
                    if (var20 != null) {
                        this.removeVolcanoObject(var15);
                    }
                } else if (command.equals("syncVolcanoUpdate")) {
                    Weather.dbg("updating client side volcano");
                    nbt = parNBT.getCompoundTag("data");
                    var15 = nbt.getLong("ID");
                    var20 = (VolcanoObject) this.lookupVolcanoes.get(Long.valueOf(var15));
                    if (var20 != null) {
                        var20.nbtSyncFromServer(nbt);
                    } else {
                        Weather.dbg("error syncing volcano, cant find by ID: " + var15);
                    }
                } else if (command.equals("syncWindUpdate")) {
                    nbt = parNBT.getCompoundTag("data");
                    this.windMan.nbtSyncFromServer(nbt);
                } else if (command.equals("syncLightningNew")) {
                    nbt = parNBT.getCompoundTag("data");
                    int var18 = nbt.getInteger("posX");
                    int var19 = nbt.getInteger("posY");
                    int var21 = nbt.getInteger("posZ");
                    double var22 = (double) var18;
                    double posY = (double) var19;
                    double posZ = (double) var21;
                    EntityLightningBolt ent = new EntityLightningBolt(this.getWorld(), var22, posY, posZ);
                    ent.serverPosX = var18;
                    ent.serverPosY = var19;
                    ent.serverPosZ = var21;
                    ent.rotationYaw = 0.0F;
                    ent.rotationPitch = 0.0F;
                    ent.setEntityId(nbt.getInteger("entityID"));
                    this.getWorld()
                        .addWeatherEffect(ent);
                } else if (command.equals("syncWeatherUpdate")) {
                    // NTNH start: sync both rain and thunder server weather state
                    NBTTagCompound var14 = parNBT.hasKey("data") ? parNBT.getCompoundTag("data") : parNBT;
                    this.isVanillaRainActiveOnServer = var14.getBoolean("isVanillaRainActiveOnServer");
                    this.isVanillaThunderActiveOnServer = var14.getBoolean("isVanillaThunderActiveOnServer");
                    // NTNH end
                }
            }
        }

    }
}
