package com.plugin.duel;

import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {

    private static DuelPlugin instance;
    private DuelManager duelManager;
    private ArenaManager arenaManager;

    @Override
    public void onLoad() {
        // onLoad fires before onEnable - if this prints, class loading is fine
        getLogger().info("DuelPlugin class loaded successfully (Java " + System.getProperty("java.version") + ")");
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            saveDefaultConfig();
            getLogger().info("Config loaded.");

            arenaManager = new ArenaManager(this);
            getLogger().info("ArenaManager ready.");

            duelManager = new DuelManager(this);
            getLogger().info("DuelManager ready.");

            getCommand("1v1").setExecutor(new DuelCommand(this));
            getLogger().info("Commands registered.");

            getServer().getPluginManager().registerEvents(new DuelListener(this), this);
            getLogger().info("DuelPlugin v" + getDescription().getVersion() + " enabled successfully!");

        } catch (Throwable t) {
            getLogger().severe("=== DuelPlugin FAILED TO ENABLE ===");
            getLogger().severe("Error: " + t.getClass().getName() + " - " + t.getMessage());
            t.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (duelManager != null) duelManager.cleanup();
        getLogger().info("DuelPlugin disabled.");
    }

    public static DuelPlugin getInstance() { return instance; }
    public DuelManager getDuelManager() { return duelManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
}
