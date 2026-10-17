package net.jahus.perplayerhardcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.jahus.perplayerhardcore.PerPlayerHardcore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("perplayerhardcore.json");

    public static String prefix = "[HARDCORE] ";
    public static String alreadyTracked = "Player is already in hardcore mode. Use /hardcore stop <player> first.";
    public static String notTracked = "Player is not in hardcore mode.";
    public static String livesRemoveNone = "No lives left to remove.";
    public static String startPlayer = "Hardcore mode started! %d %s. %s.";
    public static String startAdmin = "Hardcore mode started for %s (%d %s. %s). Hardcore spawn is currently set to the world spawn. Use /hardcore setspawn to change it.";
    public static String stopPlayer = "Hardcore mode has been stopped.";
    public static String stopAdmin = "Hardcore tracking stopped for %s.";
    public static String setspawnPlayer = "Your hardcore spawn has been set to %s.";
    public static String setspawnAdmin = "Hardcore spawn set for %s to %s.";
    public static String livesAddedPlayer = "You now have %d more %s (total granted: %d, remaining: %d).";
    public static String livesAddedAdmin = "%s now has %d more %s (total granted: %d, remaining: %d).";
    public static String livesRemovedPlayer = "You lost %d %s (total granted: %d, remaining: %d).";
    public static String livesRemovedAdmin = "%s lost %d %s (total granted: %d, remaining: %d).";
    public static String livesLeft = "You only have %d %s left.";
    public static String gameOver = "GAME OVER!";
    public static String spectator = "Your game mode has been changed to spectator mode.";
    public static String life = "life";
    public static String lives = "lives";
    public static String modeWorldspawn = "World spawn at game over";
    public static String modeSpectator = "Spectator at game over";
    public static String get = "Player: %s\nMode: %s\nInitial lives: %d\nBonus lives: %d\nTotal granted: %d\nRemaining: %d\nHardcore spawn: %s";
    public static String listHeader = "Tracked hardcore players:";
    public static String listNone = "None.";
    public static String listEntry = "%s (%s, lives: %d+%d, rem: %d)";

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    if (data.prefix != null) prefix = data.prefix;
                    if (data.alreadyTracked != null) alreadyTracked = data.alreadyTracked;
                    if (data.notTracked != null) notTracked = data.notTracked;
                    if (data.livesRemoveNone != null) livesRemoveNone = data.livesRemoveNone;
                    if (data.startPlayer != null) startPlayer = data.startPlayer;
                    if (data.startAdmin != null) startAdmin = data.startAdmin;
                    if (data.stopPlayer != null) stopPlayer = data.stopPlayer;
                    if (data.stopAdmin != null) stopAdmin = data.stopAdmin;
                    if (data.setspawnPlayer != null) setspawnPlayer = data.setspawnPlayer;
                    if (data.setspawnAdmin != null) setspawnAdmin = data.setspawnAdmin;
                    if (data.livesAddedPlayer != null) livesAddedPlayer = data.livesAddedPlayer;
                    if (data.livesAddedAdmin != null) livesAddedAdmin = data.livesAddedAdmin;
                    if (data.livesRemovedPlayer != null) livesRemovedPlayer = data.livesRemovedPlayer;
                    if (data.livesRemovedAdmin != null) livesRemovedAdmin = data.livesRemovedAdmin;
                    if (data.livesLeft != null) livesLeft = data.livesLeft;
                    if (data.gameOver != null) gameOver = data.gameOver;
                    if (data.spectator != null) spectator = data.spectator;
                    if (data.life != null) life = data.life;
                    if (data.lives != null) lives = data.lives;
                    if (data.modeWorldspawn != null) modeWorldspawn = data.modeWorldspawn;
                    if (data.modeSpectator != null) modeSpectator = data.modeSpectator;
                    if (data.get != null) get = data.get;
                    if (data.listHeader != null) listHeader = data.listHeader;
                    if (data.listNone != null) listNone = data.listNone;
                    if (data.listEntry != null) listEntry = data.listEntry;
                }
            } catch (Exception e) {
                PerPlayerHardcore.LOGGER.error("Failed to load config", e);
            }
        }
        save();
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.prefix = prefix;
        data.alreadyTracked = alreadyTracked;
        data.notTracked = notTracked;
        data.livesRemoveNone = livesRemoveNone;
        data.startPlayer = startPlayer;
        data.startAdmin = startAdmin;
        data.stopPlayer = stopPlayer;
        data.stopAdmin = stopAdmin;
        data.setspawnPlayer = setspawnPlayer;
        data.setspawnAdmin = setspawnAdmin;
        data.livesAddedPlayer = livesAddedPlayer;
        data.livesAddedAdmin = livesAddedAdmin;
        data.livesRemovedPlayer = livesRemovedPlayer;
        data.livesRemovedAdmin = livesRemovedAdmin;
        data.livesLeft = livesLeft;
        data.gameOver = gameOver;
        data.spectator = spectator;
        data.life = life;
        data.lives = lives;
        data.modeWorldspawn = modeWorldspawn;
        data.modeSpectator = modeSpectator;
        data.get = get;
        data.listHeader = listHeader;
        data.listNone = listNone;
        data.listEntry = listEntry;

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            PerPlayerHardcore.LOGGER.error("Failed to save config", e);
        }
    }

    private static class ConfigData {
        String prefix, alreadyTracked, notTracked, livesRemoveNone;
        String startPlayer, startAdmin, stopPlayer, stopAdmin;
        String setspawnPlayer, setspawnAdmin;
        String livesAddedPlayer, livesAddedAdmin, livesRemovedPlayer, livesRemovedAdmin, livesLeft;
        String gameOver, spectator, life, lives, modeWorldspawn, modeSpectator;
        String get, listHeader, listNone, listEntry;
    }
}
