package me.mrbubbles.fastcrystal.settings;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Setting<T> {
    public static Set<Setting<?>> settings = new LinkedHashSet<>();
    T value;
    String name;
    String description;
    boolean hidden;

    Setting(String name, T value, String description, boolean hidden, boolean register) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.hidden = hidden;
        if (register) settings.add(this);
    }

    Setting(String name, T value, String description, boolean register) {
        this(name, value, description, false, register);
    }

    Setting(String name, T value, String description) {
        this(name, value, description, true);
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