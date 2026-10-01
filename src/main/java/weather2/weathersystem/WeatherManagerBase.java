package weather2.weathersystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.apache.commons.io.FileUtils;

import CoroUtil.util.CoroUtilFile;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.volcano.VolcanoObject;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.wind.WindManager;

public class WeatherManagerBase {

    public int dim;
    private List listStormObjects = new ArrayList();
    public HashMap lookupStormObjectsByID = new HashMap();
    public HashMap lookupStormObjectsByLayer = new HashMap();
    private List listVolcanoes = new ArrayList();
    public HashMap lookupVolcanoes = new HashMap();
    public WindManager windMan = new WindManager(this);
    public boolean isVanillaRainActiveOnServer = false;
    public long lastStormFormed = 0L;

    public WeatherManagerBase(int parDim) {
        this.dim = parDim;
        this.lookupStormObjectsByLayer.put(Integer.valueOf(0), new ArrayList());
        this.lookupStormObjectsByLayer.put(Integer.valueOf(1), new ArrayList());
        this.lookupStormObjectsByLayer.put(Integer.valueOf(2), new ArrayList());
    }

    public void reset() {
        int i;
        for (i = 0; i < this.getStormObjects()
            .size(); ++i) {
            StormObject vo = (StormObject) this.getStormObjects()
                .get(i);
            vo.reset();
        }

        this.getStormObjects()
            .clear();
        this.lookupStormObjectsByID.clear();

        try {
            ((ArrayList) this.lookupStormObjectsByLayer.get(Integer.valueOf(0))).clear();
            ((ArrayList) this.lookupStormObjectsByLayer.get(Integer.valueOf(1))).clear();
            ((ArrayList) this.lookupStormObjectsByLayer.get(Integer.valueOf(2))).clear();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        for (i = 0; i < this.getVolcanoObjects()
            .size(); ++i) {
            VolcanoObject var4 = (VolcanoObject) this.getVolcanoObjects()
                .get(i);
            var4.reset();
        }

        this.getVolcanoObjects()
            .clear();
        this.lookupVolcanoes.clear();
        this.windMan.reset();
        StormObject.lastUsedStormID = 0L;
    }

    public World getWorld() {
        return null;
    }

    public void tick() {
        World world = this.getWorld();
        if (world != null) {
            List list = this.getStormObjects();

            int i;
            for (i = 0; i < list.size(); ++i) {
                StormObject so = (StormObject) list.get(i);
                if (this instanceof WeatherManagerServer && so.isDead) {
                    this.removeStormObject(so.ID);
                    ((WeatherManagerServer) this).syncStormRemove(so);
                } else if (!so.isDead) {
                    so.tick();
                } else if (this.getWorld().isRemote) {
                    Weather.dbg(
                        "WARNING!!! - detected isDead storm object still in client side list, had to remove storm object with ID "
                            + so.ID
                            + " from client side, wasnt properly removed via main channels");
                    this.removeStormObject(so.ID);
                }
            }

            for (i = 0; i < this.getVolcanoObjects()
                .size(); ++i) {
                ((VolcanoObject) this.getVolcanoObjects()
                    .get(i)).tick();
            }

            this.windMan.tick();
        }

    }

    public void tickRender(float partialTick) {
        World world = this.getWorld();
        if (world != null) {
            for (int i = 0; i < this.getStormObjects()
                .size(); ++i) {
                ((StormObject) this.getStormObjects()
                    .get(i)).tickRender(partialTick);
            }
        }

    }

    public List getStormObjects() {
        return this.listStormObjects;
    }

    public List getStormObjectsByLayer(int layer) {
        return (List) this.lookupStormObjectsByLayer.get(Integer.valueOf(layer));
    }

    public void addStormObject(StormObject so) {
        if (!this.lookupStormObjectsByID.containsKey(Long.valueOf(so.ID))) {
            this.listStormObjects.add(so);
            this.lookupStormObjectsByID.put(Long.valueOf(so.ID), so);
            ((ArrayList) this.lookupStormObjectsByLayer.get(Integer.valueOf(so.layer))).add(so);
        } else {
            Weather.dbg(
                "Weather2 WARNING!!! Client received new storm create for an ID that is already active! design bug");
        }

    }

    public void removeStormObject(long ID) {
        StormObject so = (StormObject) this.lookupStormObjectsByID.get(Long.valueOf(ID));
        if (so != null) {
            so.setDead();
            this.listStormObjects.remove(so);
            this.lookupStormObjectsByID.remove(Long.valueOf(ID));
            ((ArrayList) this.lookupStormObjectsByLayer.get(Integer.valueOf(so.layer))).remove(so);
        } else {
            Weather.dbg(
                "error looking up storm ID on server for removal: " + ID
                    + " - lookup count: "
                    + this.lookupStormObjectsByID.size()
                    + " - last used ID: "
                    + StormObject.lastUsedStormID);
        }

    }

    public List getVolcanoObjects() {
        return this.listVolcanoes;
    }

    public void addVolcanoObject(VolcanoObject so) {
        if (!this.lookupVolcanoes.containsKey(Long.valueOf(so.ID))) {
            this.listVolcanoes.add(so);
            this.lookupVolcanoes.put(Long.valueOf(so.ID), so);
        } else {
            Weather.dbg(
                "Weather2 WARNING!!! Client received new volcano create for an ID that is already active! design bug");
        }

    }

    public void removeVolcanoObject(long ID) {
        VolcanoObject vo = (VolcanoObject) this.lookupVolcanoes.get(Long.valueOf(ID));
        if (vo != null) {
            vo.setDead();
            this.listVolcanoes.remove(vo);
            this.lookupVolcanoes.remove(Long.valueOf(ID));
            Weather.dbg("removing volcano");
        }

    }

    public StormObject getClosestStormAny(Vec3 parPos, double maxDist) {
        return this.getClosestStorm(parPos, maxDist, -1, true);
    }

    public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin) {
        return this.getClosestStorm(parPos, maxDist, severityFlagMin, false);
    }

    public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin, boolean orRain) {
        StormObject closestStorm = null;
        double closestDist = 9999999.0D;

        for (int i = 0; i < this.getStormObjects()
            .size(); ++i) {
            StormObject storm = (StormObject) this.getStormObjects()
                .get(i);
            if (!storm.isDead) {
                double dist = storm.pos.distanceTo(parPos);
                if (dist < closestDist && dist <= maxDist
                    && (storm.attrib_precipitation && orRain || severityFlagMin == -1
                        || storm.levelCurIntensityStage >= severityFlagMin)) {
                    closestStorm = storm;
                    closestDist = dist;
                }
            }
        }

        return closestStorm;
    }

    public List getStormsAround(Vec3 parPos, double maxDist) {
        ArrayList storms = new ArrayList();

        for (int i = 0; i < this.getStormObjects()
            .size(); ++i) {
            StormObject storm = (StormObject) this.getStormObjects()
                .get(i);
            if (!storm.isDead && storm.pos.distanceTo(parPos) < maxDist
                && (storm.attrib_precipitation || storm.levelCurIntensityStage > StormObject.STATE_NORMAL)) {
                storms.add(storm);
            }
        }

        return storms;
    }

    public void writeToFile() {
        NBTTagCompound mainNBT = new NBTTagCompound();
        NBTTagCompound listVolcanoesNBT = new NBTTagCompound();

        for (int listStormsNBT = 0; listStormsNBT < this.listVolcanoes.size(); ++listStormsNBT) {
            VolcanoObject saveFolder = (VolcanoObject) this.listVolcanoes.get(listStormsNBT);
            NBTTagCompound ex = new NBTTagCompound();
            saveFolder.writeToNBT(ex);
            listVolcanoesNBT.setTag("volcano_" + saveFolder.ID, ex);
        }

        mainNBT.setTag("volcanoData", listVolcanoesNBT);
        mainNBT.setLong("lastUsedIDVolcano", VolcanoObject.lastUsedID);
        NBTTagCompound var8 = new NBTTagCompound();

        for (int var9 = 0; var9 < this.listStormObjects.size(); ++var9) {
            StormObject var11 = (StormObject) this.listStormObjects.get(var9);
            NBTTagCompound objNBT = var11.writeToNBT();
            var8.setTag("storm_" + var11.ID, objNBT);
        }

        mainNBT.setTag("stormData", var8);
        mainNBT.setLong("lastUsedIDStorm", StormObject.lastUsedStormID);
        mainNBT.setLong("lastStormFormed", this.lastStormFormed);
        String var10 = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName()
            + "weather2"
            + File.separator;

        try {
            if (!(new File(var10)).exists()) {
                (new File(var10)).mkdirs();
            }

            FileOutputStream var12 = new FileOutputStream(var10 + "WeatherData_" + this.dim + ".dat");
            CompressedStreamTools.writeCompressed(mainNBT, var12);
            var12.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public void readFromFile() {
        NBTTagCompound rtsNBT = new NBTTagCompound();
        String saveFolder = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName()
            + "weather2"
            + File.separator;
        boolean readFail = false;

        try {
            if ((new File(saveFolder + "WeatherData_" + this.dim + ".dat")).exists()) {
                rtsNBT = CompressedStreamTools
                    .readCompressed(new FileInputStream(saveFolder + "WeatherData_" + this.dim + ".dat"));
            }
        } catch (Exception var15) {
            var15.printStackTrace();
            readFail = true;
        }

        if (!readFail) {
            try {
                File nbtVolcanoes = new File(saveFolder + "WeatherData_" + this.dim + "_BACKUP0.dat");
                if (nbtVolcanoes.exists()) {
                    FileUtils.copyFile(nbtVolcanoes, new File(saveFolder + "WeatherData_" + this.dim + "_BACKUP1.dat"));
                }

                if ((new File(saveFolder + "WeatherData_" + this.dim + ".dat")).exists()) {
                    FileUtils.copyFile(
                        new File(saveFolder + "WeatherData_" + this.dim + ".dat"),
                        new File(saveFolder + "WeatherData_" + this.dim + "_BACKUP0.dat"));
                }
            } catch (Exception var14) {
                var14.printStackTrace();
            }
        } else {
            System.out.println(
                "WARNING! Weather2 File: WeatherData.dat failed to load, automatically restoring to backup from previous game run");

            try {
                if ((new File(saveFolder + "WeatherData_" + this.dim + "_BACKUP0.dat")).exists()) {
                    rtsNBT = CompressedStreamTools
                        .readCompressed(new FileInputStream(saveFolder + "WeatherData_" + this.dim + "_BACKUP0.dat"));
                } else {
                    System.out.println("WARNING! Failed to find backup file WeatherData_BACKUP0.dat, nothing loaded");
                }
            } catch (Exception var13) {
                var13.printStackTrace();
                System.out.println("WARNING! Error loading backup file WeatherData_BACKUP0.dat, nothing loaded");
            }
        }

        this.lastStormFormed = rtsNBT.getLong("lastStormFormed");
        VolcanoObject.lastUsedID = rtsNBT.getLong("lastUsedIDVolcano");
        StormObject.lastUsedStormID = rtsNBT.getLong("lastUsedIDStorm");
        NBTTagCompound nbtVolcanoes1 = rtsNBT.getCompoundTag("volcanoData");
        Iterator it = nbtVolcanoes1.func_150296_c()
            .iterator();

        while (it.hasNext()) {
            String nbtStorms = (String) it.next();
            NBTTagCompound tagName = nbtVolcanoes1.getCompoundTag(nbtStorms);
            VolcanoObject teamData = new VolcanoObject(
                (WeatherManagerBase) ServerTickHandler.lookupDimToWeatherMan.get(Integer.valueOf(0)));

            try {
                teamData.readFromNBT(tagName);
            } catch (Exception var12) {
                var12.printStackTrace();
            }

            this.addVolcanoObject(teamData);
            ((WeatherManagerServer) ((WeatherManagerServer) this)).syncVolcanoNew(teamData);
            teamData.initPost();
        }

        NBTTagCompound nbtStorms1 = rtsNBT.getCompoundTag("stormData");
        it = nbtStorms1.func_150296_c()
            .iterator();

        while (it.hasNext()) {
            String tagName1 = (String) it.next();
            NBTTagCompound teamData1 = nbtStorms1.getCompoundTag(tagName1);
            if (ServerTickHandler.lookupDimToWeatherMan.get(Integer.valueOf(this.dim)) != null) {
                StormObject to = new StormObject(
                    (WeatherManagerBase) ServerTickHandler.lookupDimToWeatherMan.get(Integer.valueOf(this.dim)));

                try {
                    to.readFromNBT(teamData1);
                } catch (Exception var11) {
                    var11.printStackTrace();
                }

                this.addStormObject(to);
                ((WeatherManagerServer) ((WeatherManagerServer) this)).syncStormNew(to);
            } else {
                System.out.println("WARNING: trying to load storm objects for missing dimension: " + this.dim);
            }
        }

    }
}
