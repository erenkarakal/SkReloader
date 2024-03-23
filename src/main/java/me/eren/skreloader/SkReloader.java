package me.eren.skreloader;

import ch.njol.skript.Skript;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkReloader extends JavaPlugin {

    private static SkReloader instance;

    @Override
    public void onEnable() {
        instance = this;
        Skript.registerAddon(this);
        new Thread(FileWatcher::start).start();
    }

    @Override
    public void onDisable() {
        FileWatcher.shouldStop = true;
    }

    public static SkReloader getInstance() {
        return instance;
    }
}
