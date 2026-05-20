package com.plugin.duel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class DuelGUI {

    public static final String GUI_TITLE = "§8طلب مبارزة";

    public static Inventory create(Player target, Player sender) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE));

        // Fill with gray glass
        ItemStack gray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = gray.getItemMeta();
        grayMeta.displayName(Component.text(" "));
        gray.setItemMeta(grayMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        // Player head in middle
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(sender);
        skullMeta.displayName(Component.text("§e" + sender.getName()));
        skullMeta.lore(List.of(Component.text("§7يريد مبارزتك!")));
        head.setItemMeta(skullMeta);
        inv.setItem(13, head);

        // Accept - green glass
        ItemStack accept = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.displayName(Component.text("§a✔ قبول"));
        acceptMeta.lore(List.of(Component.text("§7اضغط للقبول المبارزة")));
        accept.setItemMeta(acceptMeta);
        inv.setItem(11, accept);

        // Deny - red glass
        ItemStack deny = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta denyMeta = deny.getItemMeta();
        denyMeta.displayName(Component.text("§c✖ رفض"));
        denyMeta.lore(List.of(Component.text("§7اضغط للرفض المبارزة")));
        deny.setItemMeta(denyMeta);
        inv.setItem(15, deny);

        return inv;
    }
}
