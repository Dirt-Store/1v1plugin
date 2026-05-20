package com.plugin.duel;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class DuelListener implements Listener {

    private final DuelPlugin plugin;

    public DuelListener(DuelPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Freeze players during countdown ───────────────────────────────────
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DuelSession session = plugin.getDuelManager().getSession(player);
        if (session != null && session.isFrozen()) {
            // Cancel horizontal movement only
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom().clone().add(0, event.getTo().getY() - event.getFrom().getY(), 0));
            }
        }
    }

    // ── End duel on player death ───────────────────────────────────────────
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        DuelSession session = plugin.getDuelManager().getSession(loser);
        if (session == null) return;

        Player winner = session.getOpponent(loser);
        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Respawn loser
        loser.spigot().respawn();

        plugin.getDuelManager().endDuel(winner, loser);
    }

    // ── Prevent PvP damage outside duels ─────────────────────────────────
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        DuelSession session = plugin.getDuelManager().getSession(attacker);
        if (session == null || !session.contains(victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§c✖ §fما تقدر تضرب هذا اللاعب هنا!");
        }
    }

    // ── Handle disconnect during duel ─────────────────────────────────────
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = plugin.getDuelManager();

        if (dm.isInQueue(player)) dm.leaveQueue(player);

        DuelSession session = dm.getSession(player);
        if (session != null) {
            Player opponent = session.getOpponent(player);
            dm.endDuel(opponent, player);
            opponent.sendMessage("§e⚠ §f" + player.getName() + " §fخرج من السيرفر، انتهت المبارزة.");
        }
    }

    // ── GUI Click Handler ─────────────────────────────────────────────────
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().title().toString();
        if (!title.contains("طلب مبارزة")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        DuelRequest request = GUISessionStore.get(player);
        if (request == null) {
            player.closeInventory();
            return;
        }

        Material mat = clicked.getType();

        if (mat == Material.GREEN_STAINED_GLASS_PANE) {
            // Accept
            player.closeInventory();
            GUISessionStore.remove(player);
            plugin.getDuelManager().acceptRequest(player, request);

        } else if (mat == Material.RED_STAINED_GLASS_PANE) {
            // Deny
            player.closeInventory();
            GUISessionStore.remove(player);
            player.sendMessage("§c✖ §fرفضت طلب المبارزة.");
            if (request.getSender().isOnline())
                request.getSender().sendMessage("§c✖ §f§e" + player.getName() + " §fرفض طلب مبارزتك.");
        }
    }
}
