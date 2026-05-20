package com.plugin.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class DuelManager {

    private final DuelPlugin plugin;

    private final Set<UUID> queue = new HashSet<>();

    // target UUID -> list of requests (multiple senders possible)
    private final Map<UUID, List<DuelRequest>> pendingRequests = new HashMap<>();

    private final Map<UUID, DuelSession> activeDuels = new HashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    public DuelManager(DuelPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Queue ──────────────────────────────────────────────────────────────

    public boolean isInQueue(Player player) {
        return queue.contains(player.getUniqueId());
    }

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    public boolean isBusy(Player player) {
        return isInQueue(player) || isInDuel(player);
    }

    public void joinQueue(Player player) {
        UUID uuid = player.getUniqueId();
        queue.add(uuid);

        Optional<UUID> opponentUUID = queue.stream()
                .filter(u -> !u.equals(uuid))
                .findFirst();

        if (opponentUUID.isPresent()) {
            Player opponent = Bukkit.getPlayer(opponentUUID.get());
            if (opponent != null && opponent.isOnline()) {
                queue.remove(uuid);
                queue.remove(opponentUUID.get());
                startDuel(player, opponent);
            }
        } else {
            player.sendMessage("§e⚔ §fأنت الآن في قائمة الانتظار.. بانتظار منافس!");
        }
    }

    public void leaveQueue(Player player) {
        queue.remove(player.getUniqueId());
        player.sendMessage("§c✖ §fخرجت من قائمة الانتظار.");
    }

    // ── Direct Requests ────────────────────────────────────────────────────

    public void sendRequest(Player sender, Player target) {
        UUID targetUUID = target.getUniqueId();
        UUID senderUUID = sender.getUniqueId();

        List<DuelRequest> requests = pendingRequests.computeIfAbsent(targetUUID, k -> new ArrayList<>());

        // Remove old request from same sender if exists
        requests.removeIf(r -> r.getSender().getUniqueId().equals(senderUUID));

        DuelRequest request = new DuelRequest(sender, target);
        requests.add(request);

        // Cancel old expiry task for this sender->target pair
        BukkitTask oldTask = expiryTasks.remove(senderUUID);
        if (oldTask != null) oldTask.cancel();

        sender.sendMessage("§a✔ §fتم إرسال طلب مبارزة إلى §e" + target.getName());
        target.sendMessage("§e⚔ §f§e" + sender.getName() + " §fيريد مبارزتك! اكتب §a/1v1 accept §fللقبول خلال §c30 ثانية");

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<DuelRequest> list = pendingRequests.get(targetUUID);
            if (list != null) {
                list.removeIf(r -> r.getSender().getUniqueId().equals(senderUUID));
                if (list.isEmpty()) pendingRequests.remove(targetUUID);
            }
            expiryTasks.remove(senderUUID);
            if (sender.isOnline())
                sender.sendMessage("§c✖ §fانتهت مهلة طلب المبارزة مع §e" + target.getName());
            if (target.isOnline())
                target.sendMessage("§c✖ §fانتهت مهلة طلب المبارزة من §e" + sender.getName());
        }, 600L);

        expiryTasks.put(senderUUID, task);
    }

    public DuelRequest getPendingRequest(Player target) {
        List<DuelRequest> list = pendingRequests.get(target.getUniqueId());
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }

    public DuelRequest getPendingRequestFrom(Player target, String senderName) {
        List<DuelRequest> list = pendingRequests.get(target.getUniqueId());
        if (list == null) return null;
        return list.stream()
                .filter(r -> r.getSender().getName().equalsIgnoreCase(senderName))
                .findFirst()
                .orElse(null);
    }

    public void removePendingRequest(Player target) {
        pendingRequests.remove(target.getUniqueId());
    }

    public void acceptRequest(Player target, DuelRequest request) {
        UUID targetUUID = target.getUniqueId();
        pendingRequests.remove(targetUUID);
        UUID senderUUID = request.getSender().getUniqueId();
        BukkitTask task = expiryTasks.remove(senderUUID);
        if (task != null) task.cancel();
        startDuel(request.getSender(), target);
    }

    // ── Duel Start ─────────────────────────────────────────────────────────

    public void startDuel(Player p1, Player p2) {
        Arena arena = plugin.getArenaManager().getArenas().values().stream()
                .filter(Arena::isReady)
                .findFirst()
                .orElse(null);

        if (arena == null) {
            p1.sendMessage("§c✖ §fلا يوجد ساحة جاهزة! اطلب من الأدمن يضبط الساحة.");
            p2.sendMessage("§c✖ §fلا يوجد ساحة جاهزة! اطلب من الأدمن يضبط الساحة.");
            return;
        }

        // Save previous locations for potential fallback
        DuelSession session = new DuelSession(p1, p2, arena);
        activeDuels.put(p1.getUniqueId(), session);
        activeDuels.put(p2.getUniqueId(), session);

        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        session.setFrozen(true);
        sendCountdown(p1, p2, session);
    }

    private void sendCountdown(Player p1, Player p2, DuelSession session) {
        showTitle(p1, "§c3", "");
        showTitle(p2, "§c3", "");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p1.isOnline() || !p2.isOnline()) { endDuelEarly(session); return; }
            showTitle(p1, "§e2", "");
            showTitle(p2, "§e2", "");
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p1.isOnline() || !p2.isOnline()) { endDuelEarly(session); return; }
            showTitle(p1, "§a1", "");
            showTitle(p2, "§a1", "");
        }, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p1.isOnline() || !p2.isOnline()) { endDuelEarly(session); return; }
            showTitle(p1, "§a§lGO!", "§fحظاً موفقاً!");
            showTitle(p2, "§a§lGO!", "§fحظاً موفقاً!");
            session.setFrozen(false);
        }, 60L);
    }

    private void endDuelEarly(DuelSession session) {
        activeDuels.remove(session.getPlayer1().getUniqueId());
        activeDuels.remove(session.getPlayer2().getUniqueId());
    }

    private void showTitle(Player player, String title, String subtitle) {
        if (!player.isOnline()) return;
        player.showTitle(Title.title(
                Component.text(title),
                Component.text(subtitle),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(300))
        ));
    }

    // ── Duel End ───────────────────────────────────────────────────────────

    public void endDuel(Player winner, Player loser) {
        DuelSession session = activeDuels.get(winner.getUniqueId());
        if (session == null) return;

        activeDuels.remove(winner.getUniqueId());
        activeDuels.remove(loser.getUniqueId());

        if (winner.isOnline()) {
            showTitle(winner, "§6🏆 فزت!", "§fهزمت §e" + loser.getName());
            winner.sendMessage("§6⚔ §fانتهت المبارزة! §aفزت §fعلى §e" + loser.getName());
        }
        if (loser.isOnline()) {
            showTitle(loser, "§c💀 خسرت!", "§f" + winner.getName() + " §fفاز عليك");
            loser.sendMessage("§c⚔ §fانتهت المبارزة! §c" + winner.getName() + " §fهزمك");
        }

        Location back = session.getArena().getBack();
        if (back != null) {
            if (winner.isOnline()) winner.teleport(back);
            if (loser.isOnline()) loser.teleport(back);
        }
    }

    public DuelSession getSession(Player player) {
        return activeDuels.get(player.getUniqueId());
    }

    public void cleanup() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
    }
}
