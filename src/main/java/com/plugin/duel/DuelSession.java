package com.plugin.duel;

import org.bukkit.entity.Player;

public class DuelSession {

    private final Player player1;
    private final Player player2;
    private final Arena arena;
    private boolean frozen = false;

    public DuelSession(Player player1, Player player2, Arena arena) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Arena getArena() { return arena; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public Player getOpponent(Player player) {
        if (player.getUniqueId().equals(player1.getUniqueId())) return player2;
        return player1;
    }

    public boolean contains(Player player) {
        return player.getUniqueId().equals(player1.getUniqueId()) ||
               player.getUniqueId().equals(player2.getUniqueId());
    }
}
