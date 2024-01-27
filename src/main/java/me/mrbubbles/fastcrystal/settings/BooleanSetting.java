package me.mrbubbles.fastcrystal.settings;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean value, String description, boolean hidden, boolean register) {
        super(name, value, description, hidden, register);
    }

    public BooleanSetting(String name, boolean value, String description, boolean register) {
        super(name, value, description, register);
    }

    public BooleanSetting(String name, boolean value, String description) {
        super(name, value, description);
    }
}