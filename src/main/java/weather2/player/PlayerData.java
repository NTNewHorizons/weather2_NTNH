package weather2.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import CoroUtil.util.CoroUtilFile;
import weather2.Weather;

public class PlayerData {

    public static HashMap playerNBT = new HashMap();

    public static NBTTagCompound getPlayerNBT(String username) {
        if (!playerNBT.containsKey(username)) {
            tryLoadPlayerNBT(username);
        }

        return (NBTTagCompound) playerNBT.get(username);
    }

    public static void tryLoadPlayerNBT(String username) {
        NBTTagCompound playerData = new NBTTagCompound();

        try {
            String fileURL = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName()
                + File.separator
                + "weather2"
                + File.separator
                + "PlayerData"
                + File.separator
                + username
                + ".dat";
            if ((new File(fileURL)).exists()) {
                playerData = CompressedStreamTools.readCompressed(new FileInputStream(fileURL));
            }
        } catch (Exception var3) {
            ;
        }

        playerNBT.put(username, playerData);
    }

    public static void writeAllPlayerNBT(boolean resetData) {
        String fileURL = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile
            .getWorldFolderName() + File.separator + "weather2" + File.separator + "PlayerData";
        if (!(new File(fileURL)).exists()) {
            (new File(fileURL)).mkdir();
        }

        Iterator it = playerNBT.entrySet()
            .iterator();

        while (it.hasNext()) {
            Entry pairs = (Entry) it.next();
            writePlayerNBT((String) pairs.getKey(), (NBTTagCompound) pairs.getValue());
        }

        if (resetData) {
            playerNBT.clear();
        }

    }

    public static void writePlayerNBT(String username, NBTTagCompound parData) {
        String fileURL = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName()
            + File.separator
            + "weather2"
            + File.separator
            + "PlayerData"
            + File.separator
            + username
            + ".dat";

        try {
            FileOutputStream ex = new FileOutputStream(fileURL);
            CompressedStreamTools.writeCompressed(parData, ex);
            ex.close();
        } catch (Exception var4) {
            var4.printStackTrace();
            Weather.dbg("Error writing Weather2 player data for " + username);
        }

    }

}
