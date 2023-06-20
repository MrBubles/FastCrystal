package mrbubblegum.fastcrystal.config;

import com.google.gson.*;
import mrbubblegum.fastcrystal.FastCrystalMod;
import mrbubblegum.fastcrystal.settings.BooleanSetting;
import mrbubblegum.fastcrystal.settings.KeybindSetting;
import mrbubblegum.fastcrystal.settings.Setting;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static mrbubblegum.fastcrystal.FastCrystalMod.mc;

/**
 * @author ChiquitaV2
 */
public class SaveConfig {

    public static String folderName = "FastCrystal/";
    private static Stopwatch saveTimer;

    public SaveConfig() {
        mc.execute(() -> {
            try {
                saveConfig();
                saveAllSettings();
                saveTimer = new Stopwatch();
                timedSave();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void saveConfig() throws IOException {
        if (!Files.exists(Paths.get(folderName))) {
            Files.createDirectories(Paths.get(folderName));
        }
    }

    public static void saveAllSettings() {
        try {
            makeFile(null, "FastCrystal");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            OutputStreamWriter fileOutputStreamWriter = new OutputStreamWriter(new FileOutputStream(folderName + "FastCrystal.json"), StandardCharsets.UTF_8);
            JsonObject fastcrystalObj = new JsonObject();

            for (Setting value : FastCrystalMod.SETTINGS) {
                if (value instanceof KeybindSetting) {
                    fastcrystalObj.add(value.getName(), new JsonPrimitive((int) value.getValue()));
                } else if (value instanceof BooleanSetting) {
                    fastcrystalObj.add(value.getName(), new JsonPrimitive((boolean) value.getValue()));
//                } else if (value instanceof FloatSetting) {
//                    fastcrystalObj.add(value.getName(), new JsonPrimitive((float) value.getValue()));
//                } else if (value instanceof IntegerSetting) {
//                    fastcrystalObj.add(value.getName(), new JsonPrimitive((int) value.getValue()));
                }
            }

            String jsonString = gson.toJson(JsonParser.parseString(fastcrystalObj.toString()));
            fileOutputStreamWriter.write(jsonString);
            fileOutputStreamWriter.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void makeFile(String location, String name) throws IOException {
        if (location != null) {
            if (Files.exists(Paths.get(folderName + location + name + ".json"))) {
                File file = new File(folderName + location + name + ".json");

                if (file.delete()) {
                    Files.createFile(Paths.get(folderName + location + name + ".json"));
                }
            } else {
                Files.createFile(Paths.get(folderName + location + name + ".json"));
            }
        } else {
            if (Files.exists(Paths.get(folderName + name + ".json"))) {
                File file = new File(folderName + name + ".json");

                file.delete();
            }
            Files.createFile(Paths.get(folderName + name + ".json"));
        }

    }

    private static void timedSave() {
        if (saveTimer.passed(5000)) {
            saveAllSettings();
            saveTimer.reset();
        }
    }
}