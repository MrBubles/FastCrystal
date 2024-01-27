package me.mrbubbles.fastcrystal.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.mrbubbles.fastcrystal.FastCrystal;
import me.mrbubbles.fastcrystal.settings.BooleanSetting;
import me.mrbubbles.fastcrystal.settings.KeybindSetting;
import me.mrbubbles.fastcrystal.settings.Setting;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class LoadConfig {

    public static void loadConfig(Path configFile) {
        InputStreamReader inputStreamReader = null;
        try {
            if (!Files.exists(configFile)) return;

            InputStream inputStream = Files.newInputStream(configFile);
            inputStreamReader = new InputStreamReader(inputStream);
            JsonObject fastCrystalObj = JsonParser.parseReader(inputStreamReader).getAsJsonObject();

            for (Setting value : Setting.settings) {
                JsonElement valueElement = fastCrystalObj.get(value.getName());
                if (valueElement == null) continue;
                if (value instanceof KeybindSetting) {
                    value.setValue(valueElement.getAsInt());
                } else if (value instanceof BooleanSetting) {
                    value.setValue(valueElement.getAsBoolean());
                }
            }

            FastCrystal.openedUI = fastCrystalObj.get("OpenedUI").getAsBoolean();

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}