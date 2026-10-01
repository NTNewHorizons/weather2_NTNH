package weather2;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;

import CoroUtil.util.CoroUtil;
import CoroUtil.util.CoroUtilEntity;
import weather2.volcano.VolcanoObject;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

public class CommandWeather2 extends CommandBase {

    public String getCommandName() {
        return "weather2";
    }

    public void processCommand(ICommandSender var1, String[] var2) {
        // NTNH start: command aliases normalization (/weather2 kill, /weather2 spawn, etc.)
        var2 = weather2.compat.WeatherNTNHHooks.normalizeCommandArgs(var2);
        // NTNH end
        String helpMsgStorm = "Syntax: storm create <rain/thunder/wind/spout/hail/F0/F1/F2/F3/F4/F5/C0/C1/C2/C3/C4/C5/hurricane> <Optional: alwaysProgress>... example: storm create F1 alwaysProgress ... eg2: storm killall";

        try {
            if (var1 instanceof EntityPlayerMP) {
                EntityPlayerMP ex = getCommandSenderAsPlayer(var1);
                WeatherManagerServer wm;
                if (var2[0].equals("volcano")) {
                    if (var2[1].equals("create")) {
                        if (ex.worldObj.provider.dimensionId == 0) {
                            wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan.get(Integer.valueOf(0));
                            VolcanoObject so = new VolcanoObject(wm);
                            so.pos = Vec3.createVectorHelper(ex.posX, ex.posY, ex.posZ);
                            so.initFirstTime();
                            wm.addVolcanoObject(so);
                            so.initPost();
                            wm.syncVolcanoNew(so);
                            CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, "volcano created");
                        } else {
                            CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, "can only make volcanos on main overworld");
                        }
                    }
                } else if (var2[0].equals("storm")) {
                    if (var2[1].equalsIgnoreCase("killAll")) {
                        wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                            .get(Integer.valueOf(ex.worldObj.provider.dimensionId));
                        CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, "killing all storms");
                        List var10 = wm.getStormObjects();

                        for (int i = 0; i < var10.size(); ++i) {
                            StormObject so1 = (StormObject) var10.get(i);
                            Weather.dbg("force killing storm ID: " + so1.ID);
                            so1.setDead();
                        }
                    } else if (!var2[1].equals("create") && !var2[1].equals("spawn")) {
                        if (var2[1].equals("help")) {
                            CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, helpMsgStorm);
                        } else {
                            CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, helpMsgStorm);
                        }
                    } else if (var2.length > 2) {
                        wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                            .get(Integer.valueOf(ex.worldObj.provider.dimensionId));
                        StormObject var11 = new StormObject(wm);
                        var11.layer = 0;
                        var11.userSpawnedFor = CoroUtilEntity.getName(ex);
                        var11.naturallySpawned = false;
                        var11.levelTemperature = 0.1F;
                        var11.pos = Vec3.createVectorHelper(
                            ex.posX,
                            (double) ((Integer) StormObject.layers.get(var11.layer)).intValue(),
                            ex.posZ);
                        var11.levelWater = var11.levelWaterStartRaining * 2;
                        var11.attrib_precipitation = true;
                        if (!var2[2].equals("rain")) {
                            var11.initRealStorm((EntityPlayer) null, (StormObject) null);
                        }

                        if (!var2[2].equals("rain")) {
                            if (!var2[2].equalsIgnoreCase("thunder") && !var2[2].equalsIgnoreCase("lightning")) {
                                if (var2[2].equalsIgnoreCase("wind")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
                                } else if (var2[2].equalsIgnoreCase("spout")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
                                    var11.attrib_waterSpout = true;
                                } else if (var2[2].equalsIgnoreCase("hail")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_HAIL;
                                } else if (var2[2].equalsIgnoreCase("F5")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE5;
                                } else if (var2[2].equalsIgnoreCase("F4")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE4;
                                } else if (var2[2].equalsIgnoreCase("F3")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE3;
                                } else if (var2[2].equalsIgnoreCase("F2")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE2;
                                } else if (var2[2].equalsIgnoreCase("F1")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE1;
                                } else if (var2[2].equalsIgnoreCase("F0")) {
                                    var11.levelCurIntensityStage = StormObject.STATE_FORMING;
                                } else if (var2[2].equalsIgnoreCase("C0")) {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_FORMING;
                                } else if (var2[2].equalsIgnoreCase("C1")) {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE1;
                                } else if (var2[2].equalsIgnoreCase("C2")) {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE2;
                                } else if (var2[2].equalsIgnoreCase("C3")) {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE3;
                                } else if (var2[2].equalsIgnoreCase("C4")) {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE4;
                                } else if (!var2[2].equalsIgnoreCase("C5") && !var2[2].equalsIgnoreCase("hurricane")) {
                                    if (var2[2].equalsIgnoreCase("hurricane")) {
                                        var11.stormType = StormObject.TYPE_WATER;
                                        var11.levelCurIntensityStage = StormObject.STATE_STAGE5;
                                    } else if (var2[2].equalsIgnoreCase("full")) {
                                        var11.levelCurIntensityStage = StormObject.STATE_THUNDER;
                                        var11.alwaysProgresses = true;
                                    } else if (var2[2].equalsIgnoreCase("test")) {
                                        var11.levelCurIntensityStage = StormObject.STATE_THUNDER;
                                    }
                                } else {
                                    var11.stormType = StormObject.TYPE_WATER;
                                    var11.levelCurIntensityStage = StormObject.STATE_STAGE5;
                                }
                            } else {
                                var11.levelCurIntensityStage = StormObject.STATE_THUNDER;
                            }
                        }

                        if (var2.length > 3 && (var2[3].contains("Progress") || var2[3].contains("progress"))) {
                            var11.alwaysProgresses = true;
                        }

                        var11.initFirstTime();
                        wm.addStormObject(var11);
                        wm.syncStormNew(var11);
                        CoroUtil.sendPlayerMsg(
                            (EntityPlayerMP) var1,
                            "storm " + var2[2]
                                + " created"
                                + (var11.alwaysProgresses ? ", flags: alwaysProgresses" : ""));
                    } else {
                        CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, helpMsgStorm);
                    }
                } else {
                    CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, helpMsgStorm);
                }
            }
        } catch (Exception var9) {
            System.out.println("Exception handling Weather2 command");
            CoroUtil.sendPlayerMsg((EntityPlayerMP) var1, helpMsgStorm);
            var9.printStackTrace();
        }

    }

    public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender) {
        return par1ICommandSender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }

    public String getCommandUsage(ICommandSender icommandsender) {
        return "";
    }
}
