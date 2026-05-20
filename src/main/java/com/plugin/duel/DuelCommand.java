package com.plugin.duel;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class DuelCommand implements CommandExecutor {

    private final DuelPlugin plugin;

    public DuelCommand(DuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is for players only!");
            return true;
        }

        DuelManager dm = plugin.getDuelManager();
        ArenaManager am = plugin.getArenaManager();

        // /1v1  (no args) → join queue
        if (args.length == 0) {
            if (dm.isInDuel(player)) {
                player.sendMessage("§c✖ §fأنت في مبارزة بالفعل!");
                return true;
            }
            if (dm.isInQueue(player)) {
                player.sendMessage("§e⚠ §fأنت في قائمة الانتظار بالفعل.");
                return true;
            }
            dm.joinQueue(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /1v1 create <name>
        if (sub.equals("create")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("§c✖ §fما عندك صلاحية!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§c الاستخدام: /1v1 create <اسم>");
                return true;
            }
            String name = args[1];
            if (!am.createArena(name)) {
                player.sendMessage("§c✖ §fالساحة §e" + name + " §fموجودة بالفعل.");
                return true;
            }
            player.sendMessage("§a✔ §fتم إنشاء الساحة §e" + name + " §fبنجاح!");
            player.sendMessage("§7الخطوات التالية:");
            player.sendMessage("§7  /1v1 set-tp " + name + " 1  §f← نقطة رسبن 1");
            player.sendMessage("§7  /1v1 set-tp " + name + " 2  §f← نقطة رسبن 2");
            player.sendMessage("§7  /1v1 setback " + name + "   §f← مكان الرجوع بعد المبارزة");
            return true;
        }

        // /1v1 delete <name>
        if (sub.equals("delete")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("§c✖ §fما عندك صلاحية!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§c الاستخدام: /1v1 delete <اسم>");
                return true;
            }
            String name = args[1];
            if (!am.deleteArena(name)) {
                player.sendMessage("§c✖ §fالساحة §e" + name + " §fما موجودة!");
                return true;
            }
            player.sendMessage("§a✔ §fتم حذف الساحة §e" + name + " §fبنجاح.");
            return true;
        }

        // /1v1 list
        if (sub.equals("list")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("§c✖ §fما عندك صلاحية!");
                return true;
            }
            if (am.getArenaNames().isEmpty()) {
                player.sendMessage("§e⚠ §fلا يوجد ساحات مضافة.");
                return true;
            }
            player.sendMessage("§6══ §eالساحات §6══");
            for (String arenaName : am.getArenaNames()) {
                Arena arena = am.getArena(arenaName);
                String status = arena.isReady() ? "§a✔ جاهزة" : "§c✖ ناقصة إعداد";
                player.sendMessage("§f• §e" + arenaName + " §7- " + status);
            }
            return true;
        }

        // /1v1 set-tp <arena> <1|2>
        if (sub.equals("set-tp")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("§c✖ §fما عندك صلاحية!");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("§c الاستخدام: /1v1 set-tp <اسم> <1|2>");
                return true;
            }
            String arenaName = args[1];
            int slot;
            try { slot = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                player.sendMessage("§c✖ §fالرقم يجب يكون 1 أو 2");
                return true;
            }
            if (slot != 1 && slot != 2) {
                player.sendMessage("§c✖ §fالرقم يجب يكون 1 أو 2");
                return true;
            }
            if (!am.arenaExists(arenaName)) {
                player.sendMessage("§c✖ §fالساحة §e" + arenaName + " §fما موجودة!");
                return true;
            }
            am.setSpawn(arenaName, slot, player.getLocation());
            player.sendMessage("§a✔ §fتم تحديد نقطة الرسبن §e" + slot + " §fللساحة §e" + arenaName);
            return true;
        }

        // /1v1 setback <arena>
        if (sub.equals("setback")) {
            if (!player.hasPermission("duel.admin")) {
                player.sendMessage("§c✖ §fما عندك صلاحية!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§c الاستخدام: /1v1 setback <اسم>");
                return true;
            }
            String arenaName = args[1];
            if (!am.arenaExists(arenaName)) {
                player.sendMessage("§c✖ §fالساحة §e" + arenaName + " §fما موجودة!");
                return true;
            }
            am.setBack(arenaName, player.getLocation());
            player.sendMessage("§a✔ §fتم تحديد مكان الرجوع للساحة §e" + arenaName);
            return true;
        }

        // /1v1 cancel
        if (sub.equals("cancel")) {
            if (dm.isInQueue(player)) {
                dm.leaveQueue(player);
            } else {
                player.sendMessage("§e⚠ §fأنت مو في قائمة الانتظار.");
            }
            return true;
        }

        // /1v1 accept [playerName]
        if (sub.equals("accept")) {
            DuelRequest request;
            if (args.length >= 2) {
                request = dm.getPendingRequestFrom(player, args[1]);
                if (request == null) {
                    player.sendMessage("§c✖ §fما في طلب مبارزة من §e" + args[1]);
                    return true;
                }
            } else {
                request = dm.getPendingRequest(player);
                if (request == null) {
                    player.sendMessage("§c✖ §fما في طلب مبارزة معلق.");
                    return true;
                }
            }
            if (request.isExpired()) {
                player.sendMessage("§c✖ §fانتهت مهلة الطلب.");
                dm.removePendingRequest(player);
                return true;
            }
            Inventory gui = DuelGUI.create(player, request.getSender());
            GUISessionStore.store(player, request);
            player.openInventory(gui);
            return true;
        }

        // /1v1 help or unknown subcommand starting with known keywords → show help
        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

        // /1v1 <playerName> → send request
        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c✖ §fاللاعب §e" + targetName + " §fمو اونلاين.");
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage("§c✖ §fما تقدر تبارز نفسك 😅");
            return true;
        }
        if (dm.isInDuel(target)) {
            player.sendMessage("§c✖ §f§e" + target.getName() + " §fفي مبارزة الحين.");
            return true;
        }
        if (dm.isBusy(player)) {
            player.sendMessage("§c✖ §fأنت مشغول الحين!");
            return true;
        }
        dm.sendRequest(player, target);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6══ §eDuelPlugin §6══");
        player.sendMessage("§e/1v1 §7- انضم لقائمة الانتظار");
        player.sendMessage("§e/1v1 <لاعب> §7- أرسل طلب مبارزة");
        player.sendMessage("§e/1v1 accept §7- اقبل طلب مبارزة");
        player.sendMessage("§e/1v1 cancel §7- اخرج من قائمة الانتظار");
        if (player.hasPermission("duel.admin")) {
            player.sendMessage("§c[Admin] §e/1v1 create <اسم> §7- أنشئ ساحة");
            player.sendMessage("§c[Admin] §e/1v1 delete <اسم> §7- احذف ساحة");
            player.sendMessage("§c[Admin] §e/1v1 list §7- قائمة الساحات");
            player.sendMessage("§c[Admin] §e/1v1 set-tp <اسم> <1|2> §7- حدد نقطة رسبن");
            player.sendMessage("§c[Admin] §e/1v1 setback <اسم> §7- حدد مكان الرجوع");
        }
    }
}
