package me.perch.copyright;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;

import static me.perch.copyright.Copyright.canCopyMap;
import static me.perch.copyright.Copyright.isCopyrightedMap;

public class CopyEvents implements Listener {

    private final Copyright main;
    public CopyEvents(Copyright main){
        this.main = main;
    }

    @EventHandler
    public void craftEvent(CraftItemEvent event) {
        ItemStack item = event.getInventory().getResult();

        if(!isCopyrightedMap(item)) return;

        if (!canCopyMap( (Player) event.getWhoClicked(), item)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_duplicate")));
        }

    }

    @EventHandler
    public void copyEvent(InventoryClickEvent event){
        if(!event.getInventory().getType().equals(InventoryType.CARTOGRAPHY)) return;

        CartographyInventory inv = (CartographyInventory) event.getInventory();
        ItemStack item = inv.getItem(2);

        if(!isCopyrightedMap(item)) return;

        if (!canCopyMap( (Player) event.getWhoClicked(), item)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_duplicate")));
        }

    }

}