package me.perch.copyright.commands;

import me.perch.copyright.Copyright;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

import static me.perch.copyright.Copyright.*;

public class CopyrightCommand implements CommandExecutor {

    private final Copyright main;
    public CopyrightCommand(Copyright main){
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "You need to be a player to use this command.");
            return true;
        }

        Player player = (Player)sender;
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.FILLED_MAP)){
            player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.not_holding_map")));
            return true;
        }

        if(!player.hasPermission("Copyright.use")){
            player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.no_perms")));
            return true;
        }

        if(args.length < 1) return true;

        ItemStack item = player.getInventory().getItemInMainHand();
        MapMeta meta = (MapMeta) item.getItemMeta();
        ArrayList<String> lore;

        switch(args[0]) {
            case "remove":
                if (!isCopyrightedMap(item)){
                    player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.remove_no_Copyright")));
                    break;
                }

                if (!isMapOwner(player,item) && !player.hasPermission("Copyright.remove.other")){
                    player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_remove")));
                    break;
                }

                //lore
                lore = new ArrayList<>();
                meta.setLore(lore);

                //metadata
                meta.getPersistentDataContainer().remove(Copyright.OWNER_KEY);

                item.setItemMeta(meta);

                player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.Copyright_removed")));
                break;
            case "add":

                if (isCopyrightedMap(item)) {
                    player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.cant_Copyright")));
                    break;
                }

                //lore
                lore = new ArrayList<>();
                String copyrightText = main.getConfig().getString("lang.Copyright_format");
                copyrightText = copyrightText.replace("%player%", player.getName());
                copyrightText = Copyright.colorCode(copyrightText);
                lore.add(copyrightText);
                meta.setLore(lore);

                //metadata
                String uuid = player.getUniqueId().toString();
                meta.getPersistentDataContainer().set(Copyright.OWNER_KEY, PersistentDataType.STRING, uuid);

                item.setItemMeta(meta);

                player.sendMessage(Copyright.colorCode(main.getConfig().getString("lang.Copyright_added")));
                break;
        }

        return true;
    }

}