package me.perch.copyright;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static me.perch.copyright.Copyright.canCopyMap;
import static me.perch.copyright.Copyright.isCopyrightedMap;

public class CopyEvents implements Listener {

    private final Copyright main;
    public CopyEvents(Copyright main){
        this.main = main;
    }

    private void deny(HumanEntity who) {
        if (who instanceof Player p) {
            p.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_duplicate")));
        }
    }

    // (Safety) If someone datapacks a grid recipe that yields a copyrighted map, block unless bypass/owner
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        ItemStack result = inv.getResult();
        if (!isCopyrightedMap(result)) return;

        HumanEntity who = event.getWhoClicked();
        if (who instanceof Player p) {
            if (!canCopyMap(p, result)) {
                event.setCancelled(true);
                deny(who);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // Cartography UI: check the *input* (slot 0) → owner can copy manually, others cannot
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographyClick(InventoryClickEvent event){
        Inventory top = event.getView().getTopInventory();
        if (!(top instanceof CartographyInventory inv)) return;

        ItemStack baseMap = inv.getItem(0); // base/filled map
        if (!isCopyrightedMap(baseMap)) return;

        HumanEntity who = event.getWhoClicked();
        if (who instanceof Player p) {
            if (!canCopyMap(p, baseMap)) {
                event.setCancelled(true);
                deny(who);
                inv.setItem(2, null); // wipe preview for clarity
            }
        } else {
            event.setCancelled(true);
            inv.setItem(2, null);
        }
    }

    // Automation moves (e.g., hoppers pulling from cartography output) → block copyrighted copies
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        if (source != null && source.getType() == InventoryType.CARTOGRAPHY) {
            ItemStack moving = event.getItem();
            if (isCopyrightedMap(moving)) {
                event.setCancelled(true);
            }
        }
    }
}
