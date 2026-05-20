package com.plugin.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaManager {

    private final DuelPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(DuelPlugin plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    public boolean createArena(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) return false;
        arenas.put(key, new Arena(name));
        saveArenas();
        return true;
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        if (!arenas.containsKey(key)) return false;
        arenas.remove(key);
        // Remove from config
        plugin.getConfig().set("arenas." + key, null);
        plugin.saveConfig();
        return true;
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

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    private void saveArenas() {
        FileConfiguration config = plugin.getConfig();
        // Clear the arenas section and rewrite entirely to avoid stale keys
        config.set("arenas", null);
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            String key = "arenas." + entry.getKey();
            Arena a = entry.getValue();
            if (a.getSpawn1() != null) saveLocation(config, key + ".spawn1", a.getSpawn1());
            if (a.getSpawn2() != null) saveLocation(config, key + ".spawn2", a.getSpawn2());
            if (a.getBack() != null) saveLocation(config, key + ".back", a.getBack());
            // Save the original display name
            config.set(key + ".displayName", a.getName());
        }
        plugin.saveConfig();
    }

    private void loadArenas() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("arenas")) return;
        if (config.getConfigurationSection("arenas") == null) return;
        for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
            String displayName = config.getString("arenas." + key + ".displayName", key);
            Arena arena = new Arena(displayName);
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
        if (loc.getWorld() == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", (double) loc.getYaw());
        config.set(path + ".pitch", (double) loc.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena location: " + path);
            return null;
        }
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Map<String, Arena> getArenas() { return arenas; }
}
