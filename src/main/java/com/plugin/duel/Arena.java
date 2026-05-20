package com.plugin.duel;

import org.bukkit.Location;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private Location back;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public Location getBack() { return back; }
    public void setBack(Location back) { this.back = back; }

    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }
}
