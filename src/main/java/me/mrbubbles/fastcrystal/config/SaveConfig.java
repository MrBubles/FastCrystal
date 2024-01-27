package me.mrbubbles.fastcrystal.config;

import com.google.gson.*;
import me.mrbubbles.fastcrystal.FastCrystal;
import me.mrbubbles.fastcrystal.settings.BooleanSetting;
import me.mrbubbles.fastcrystal.settings.KeybindSetting;
import me.mrbubbles.fastcrystal.settings.Setting;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveConfig {

    public static void saveConfig(Path configFile) {
        try {
            createConfigFile(configFile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            OutputStreamWriter fileOutputStreamWriter = new OutputStreamWriter(new FileOutputStream(configFile.toFile()), StandardCharsets.UTF_8);
            JsonObject fastCrystalObj = new JsonObject();

            for (Setting value : Setting.settings) {
                if (value instanceof KeybindSetting) {
                    fastCrystalObj.add(value.getName(), new JsonPrimitive((int) value.getValue()));
                } else if (value instanceof BooleanSetting) {
                    fastCrystalObj.add(value.getName(), new JsonPrimitive((boolean) value.getValue()));
                }
            }

            fastCrystalObj.add("OpenedUI", new JsonPrimitive(FastCrystal.openedUI));

            String jsonString = gson.toJson(JsonParser.parseString(fastCrystalObj.toString()));
            fileOutputStreamWriter.write(jsonString);
            fileOutputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createConfigFile(Path configFile) throws IOException {
        if (Files.exists(configFile)) {
            if (configFile.toFile().delete()) Files.createFile(configFile);
        } else {
            Files.createFile(configFile);
        }
    }
}