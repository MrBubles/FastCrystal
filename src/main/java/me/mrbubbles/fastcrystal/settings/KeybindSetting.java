package me.mrbubbles.fastcrystal.settings;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, int key, String description, boolean hidden, boolean register) {
        super(name, key, description, hidden, register);
    }

    public KeybindSetting(String name, int key, String description, boolean register) {
        super(name, key, description, register);
    }

    public KeybindSetting(String name, int key, String description) {
        super(name, key, description);
    }
}