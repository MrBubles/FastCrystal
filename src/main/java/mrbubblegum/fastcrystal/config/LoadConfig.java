package mrbubblegum.fastcrystal.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mrbubblegum.fastcrystal.FastCrystalMod;
import mrbubblegum.fastcrystal.settings.BooleanSetting;
import mrbubblegum.fastcrystal.settings.KeybindSetting;
import mrbubblegum.fastcrystal.settings.Setting;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author ChiquitaV2
 */
@SuppressWarnings("unchecked")
public class LoadConfig {

    private static final String folderName = SaveConfig.folderName;

    public LoadConfig() {
        try {
            loadAllSettings();
        } catch (IOException ignored) {

        }
    }

    private static void loadAllSettings() throws IOException {
        InputStreamReader inputStreamReader = null;
        try {
            Path path = Paths.get(folderName + "FastCrystal.json");
            if (!Files.exists(path)) {
                return;
            }

            InputStream inputStream = Files.newInputStream(path);
            inputStreamReader = new InputStreamReader(inputStream);
            JsonObject fastcrystalObj = JsonParser.parseReader(inputStreamReader).getAsJsonObject();


            for (Setting value : FastCrystalMod.SETTINGS) {
                JsonElement valueElement = fastcrystalObj.get(value.getName());
                if (valueElement == null) continue;
                if (value instanceof KeybindSetting) {
                    value.setValue(valueElement.getAsInt());
                } else if (value instanceof BooleanSetting) {
                    value.setValue(valueElement.getAsBoolean());
//                } else if (value instanceof FloatSetting) {
//                    value.setValue(valueElement.getAsFloat());
//                } else if (value instanceof IntegerSetting) {
//                    value.setValue(valueElement.getAsInt());
                }
            }
            inputStream.close();
        } catch (IOException ignored) {

        } finally {
            if (inputStreamReader != null)
                inputStreamReader.close();
        }
    }


}