package mrbubblegum.fastcrystal.settings;

public class Setting<T> {

    T value;
    String name;
    boolean hidden;

    Setting(String name, T defaultValue, boolean hidden) {
        this.name = name;
        this.value = defaultValue;
        this.hidden = hidden;
    }

    Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.hidden = false;
    }

    public String getName() {
        return name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}