package mrbubblegum.fastcrystal.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue, String description, boolean hidden) {
        super(name, defaultValue, description, hidden);
    }

    public BooleanSetting(String name, boolean defaultValue, String description) {
        super(name, defaultValue, description);
    }

    public void setValue(boolean value) {
        this.value = value;
    }

}