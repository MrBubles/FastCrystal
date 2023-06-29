package mrbubblegum.fastcrystal.settings;

public class Setting<T> {

    T value;
    String name;
    String description;
    boolean hidden;

    Setting(String name, T defaultValue, String description, boolean hidden) {
        this.name = name;
        this.value = defaultValue;
        this.description = description;
        this.hidden = hidden;
    }

    Setting(String name, T defaultValue, String description) {
        this.name = name;
        this.value = defaultValue;
        this.description = description;
        this.hidden = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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