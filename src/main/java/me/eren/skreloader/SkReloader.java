package me.eren.skreloader;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public final class SkReloader extends JavaPlugin {

    private static SkReloader instance;
    private static Logger logger;
    private static BukkitTask fileWatcher;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        Skript.registerAddon(this);
        fileWatcher = Bukkit.getScheduler().runTaskAsynchronously(this, FileWatcher::start);
    }

    @Override
    public void onDisable() {
        fileWatcher.cancel();
    }

    public static SkReloader getInstance() {
        return instance;
    }

    public static Logger getLog() {
        return logger;
    }
}
