package me.perch.copyright;

import io.papermc.paper.event.player.CartographyItemEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Crafter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static me.perch.copyright.Copyright.canCopyMap;
import static me.perch.copyright.Copyright.isCopyrightedMap;

public class CopyEventsPaper implements Listener {

    private final Copyright main;

    public CopyEventsPaper(Copyright main) {
        this.main = main;
    }

    private void deny(HumanEntity who) {
        if (who instanceof Player p) {
            p.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_duplicate")));
        }
    }

    // Paper: cartography completion event (fires as result is about to complete)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographyComplete(CartographyItemEvent event) {
        if (!(event.getInventory() instanceof CartographyInventory inv)) return;

        ItemStack inputMap = inv.getItem(0); // base map
        if (!isCopyrightedMap(inputMap)) return;

        HumanEntity who = event.getWhoClicked();
        if (who instanceof Player p) {
            if (!canCopyMap(p, inputMap)) {
                event.setCancelled(true);
                deny(who);
                inv.setItem(2, null);
            }
        } else {
            event.setCancelled(true);
            inv.setItem(2, null);
        }
    }

    // Paper 1.21+: Crafter automation — scan inputs; if any copyrighted map involved, cancel
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        Block block = event.getBlock();
        if (block == null) { event.setCancelled(true); return; }

        BlockState state = block.getState();
        if (!(state instanceof Crafter crafter)) { event.setCancelled(true); return; }

        Inventory inv = crafter.getInventory(); // generic Inventory in your API
        if (inv == null) return;

        for (ItemStack it : inv.getContents()) {
            if (isCopyrightedMap(it)) {
                event.setCancelled(true); // cancelling is enough to prevent crafting
                return;
            }
        }
    }
}
