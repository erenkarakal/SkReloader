package me.eren.skreloader;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.log.RetainingLogHandler;
import org.bukkit.Bukkit;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class FileWatcher {

    private static final Path SCRIPTS_FOLDER = Paths.get(Skript.getInstance().getDataFolder().getAbsolutePath() + "/" + Skript.SCRIPTSFOLDER + "/");

    public static void start() {

        try (Stream<Path> files = Files.walk(SCRIPTS_FOLDER);
                WatchService watchService = FileSystems.getDefault().newWatchService()) {

            files.filter(file -> file.toFile().isDirectory()).forEach(file -> {
                try {
                    file.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                } catch (IOException e) {
                    throw new RuntimeException("Error while registering the FileWatcher!", e);
                }
            });

            broadcast("Started FileWatcher! Scripts folder: '" + SCRIPTS_FOLDER + "'");

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path context = (Path) event.context();
                    broadcast(kind + " |-----| " + context);
                    Path fullPath = SCRIPTS_FOLDER.resolve((Path) key.watchable()).resolve(context).normalize();
                    File file = fullPath.toFile();

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        broadcast("Loading new file §e" + getScriptName(file) + "§f...");

                        Bukkit.getScheduler().runTask(SkReloader.getInstance(), () -> {
                            try (RetainingLogHandler logHandler = new RetainingLogHandler()) {
                                ScriptLoader.loadScripts(file, logHandler);
                                printErrors(logHandler);
                            } finally {
                                broadcast("Loaded!");
                            }
                        });

                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Script script = ScriptLoader.getScript(file);
                        if (script == null) continue;
                        broadcast("Reloading §e" + getScriptName(file) + "§f...");

                        Bukkit.getScheduler().runTask(SkReloader.getInstance(), () -> {
                            try (RetainingLogHandler logHandler = new RetainingLogHandler()) {
                                ScriptLoader.reloadScript(script, logHandler);
                                printErrors(logHandler);
                            } finally {
                                broadcast("Reloaded!");
                            }
                        });
                    }
                }

                if (!key.reset()) {
                    broadcast("Stopping the FileWatcher. Directory is no longer accessible (or something else went wrong).");
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private static void broadcast(String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("skreloader.message"))
                .forEach(p -> p.sendMessage("§7[§6§lSkReloader§7] §f" + message) );
        Bukkit.getConsoleSender().sendMessage("§7[§6§lSkReloader§7] §f" + message);
    }

    private static void printErrors(RetainingLogHandler logHandler) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("skreloader.message"))
                .forEach(p -> logHandler.printErrors(p, null));
        logHandler.printErrors(Bukkit.getConsoleSender(), null);
    }

    private static String getScriptName(File file) {
        String name = file.toString().replaceAll(SCRIPTS_FOLDER + "/", "");
        if (name.contains("/")) {
            return "/" + name;
        }
        return name;
    }
}
