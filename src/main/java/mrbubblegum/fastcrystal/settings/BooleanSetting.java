package mrbubblegum.fastcrystal.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue, boolean hidden) {
        super(name, defaultValue, hidden);
    }

    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void setValue(boolean value) {
        this.value = value;
    }

}