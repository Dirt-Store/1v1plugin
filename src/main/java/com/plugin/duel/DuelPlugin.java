package com.plugin.duel;

import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {

    private static DuelPlugin instance;
    private DuelManager duelManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        instance = this;
        try {
            saveDefaultConfig();
            arenaManager = new ArenaManager(this);
            duelManager = new DuelManager(this);
            getCommand("1v1").setExecutor(new DuelCommand(this));
            getServer().getPluginManager().registerEvents(new DuelListener(this), this);
            getLogger().info("DuelPlugin enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable DuelPlugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (duelManager != null) duelManager.cleanup();
        getLogger().info("DuelPlugin disabled!");
    }

    public static DuelPlugin getInstance() { return instance; }
    public DuelManager getDuelManager() { return duelManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
}
