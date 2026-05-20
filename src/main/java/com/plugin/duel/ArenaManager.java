package com.plugin.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final DuelPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(DuelPlugin plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    public void createArena(String name) {
        if (!arenas.containsKey(name.toLowerCase())) {
            arenas.put(name.toLowerCase(), new Arena(name));
        }
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public void setSpawn(String arenaName, int slot, Location loc) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return;
        if (slot == 1) arena.setSpawn1(loc);
        else if (slot == 2) arena.setSpawn2(loc);
        saveArenas();
    }

    public void setBack(String arenaName, Location loc) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return;
        arena.setBack(loc);
        saveArenas();
    }

    private void saveArenas() {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            String key = "arenas." + entry.getKey();
            Arena a = entry.getValue();
            if (a.getSpawn1() != null) saveLocation(config, key + ".spawn1", a.getSpawn1());
            if (a.getSpawn2() != null) saveLocation(config, key + ".spawn2", a.getSpawn2());
            if (a.getBack() != null) saveLocation(config, key + ".back", a.getBack());
        }
        plugin.saveConfig();
    }

    private void loadArenas() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("arenas")) return;
        for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
            Arena arena = new Arena(key);
            if (config.contains("arenas." + key + ".spawn1"))
                arena.setSpawn1(loadLocation(config, "arenas." + key + ".spawn1"));
            if (config.contains("arenas." + key + ".spawn2"))
                arena.setSpawn2(loadLocation(config, "arenas." + key + ".spawn2"));
            if (config.contains("arenas." + key + ".back"))
                arena.setBack(loadLocation(config, "arenas." + key + ".back"));
            arenas.put(key.toLowerCase(), arena);
        }
    }

    private void saveLocation(FileConfiguration config, String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        World world = Bukkit.getWorld(config.getString(path + ".world"));
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Map<String, Arena> getArenas() { return arenas; }
}
