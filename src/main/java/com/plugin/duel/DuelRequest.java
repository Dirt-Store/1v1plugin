package com.plugin.duel;

import org.bukkit.entity.Player;

public class DuelRequest {

    private final Player sender;
    private final Player target;
    private final long timestamp;

    public DuelRequest(Player sender, Player target) {
        this.sender = sender;
        this.target = target;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getSender() { return sender; }
    public Player getTarget() { return target; }
    public long getTimestamp() { return timestamp; }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30000;
    }
}
