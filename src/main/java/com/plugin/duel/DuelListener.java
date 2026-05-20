package com.plugin.duel;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        Player player = event.getPlayer();
        DuelSession session = plugin.getDuelManager().getSession(player);
        if (session == null || !session.isFrozen()) return;

        Location from = event.getFrom();
        // Block horizontal movement only, allow looking around
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            Location cancel = from.clone();
            cancel.setYaw(to.getYaw());
            cancel.setPitch(to.getPitch());
            event.setTo(cancel);
        }
    }

    // ── End duel on player death ───────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        DuelSession session = plugin.getDuelManager().getSession(loser);
        if (session == null) return;

        Player winner = session.getOpponent(loser);
        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Respawn and end duel on next tick (after respawn packet)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            loser.spigot().respawn();
            plugin.getDuelManager().endDuel(winner, loser);
        }, 1L);
    }

    // ── Prevent PvP damage outside duels ─────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        DuelSession session = plugin.getDuelManager().getSession(attacker);
        if (session == null || !session.contains(victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§c✖ §fما تقدر تضرب هذا اللاعب هنا!");
        }
    }

    // ── Prevent damage during freeze countdown ────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageFrozen(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        DuelSession session = plugin.getDuelManager().getSession(victim);
        if (session != null && session.isFrozen()) {
            event.setCancelled(true);
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
            if (opponent.isOnline())
                opponent.sendMessage("§e⚠ §f" + player.getName() + " §fخرج من السيرفر، انتهت المبارزة.");
        }
    }

    // ── GUI Click Handler ─────────────────────────────────────────────────
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Use Adventure Component comparison instead of toString()
        Component titleComponent = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(titleComponent);
        if (!titleStr.contains("طلب مبارزة")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        DuelRequest request = GUISessionStore.get(player);
        if (request == null) {
            player.closeInventory();
            return;
        }

        if (request.isExpired()) {
            player.closeInventory();
            GUISessionStore.remove(player);
            player.sendMessage("§c✖ §fانتهت مهلة الطلب.");
            return;
        }

        Material mat = clicked.getType();

        if (mat == Material.GREEN_STAINED_GLASS_PANE) {
            player.closeInventory();
            GUISessionStore.remove(player);
            plugin.getDuelManager().acceptRequest(player, request);

        } else if (mat == Material.RED_STAINED_GLASS_PANE) {
            player.closeInventory();
            GUISessionStore.remove(player);
            player.sendMessage("§c✖ §fرفضت طلب المبارزة.");
            if (request.getSender().isOnline())
                request.getSender().sendMessage("§c✖ §f§e" + player.getName() + " §fرفض طلب مبارزتك.");
        }
    }
}
