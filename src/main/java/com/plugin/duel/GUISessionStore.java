package com.plugin.duel;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUISessionStore {

    private static final Map<UUID, DuelRequest> sessions = new HashMap<>();

    public static void store(Player player, DuelRequest request) {
        sessions.put(player.getUniqueId(), request);
    }

    public static DuelRequest get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public static void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }
}
