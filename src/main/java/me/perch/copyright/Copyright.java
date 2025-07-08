package me.perch.copyright;

import me.perch.copyright.commands.ReloadCommand;
import me.perch.copyright.commands.CopyrightCommand;
import me.perch.copyright.commands.CopyrightTab;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Copyright extends JavaPlugin implements Listener {

    public static final NamespacedKey Copyright_OWNER_KEY = new NamespacedKey("tradermarker","owner");
    private static final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
    public static String colorCode(String message) {
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static boolean isMapOwner(Player player, ItemStack map){
        Player owner = getOwnerOfMap(map);
        return player.getUniqueId() == owner.getUniqueId();
    }

    public static boolean canCopyMap(Player player, ItemStack map){
        if (isMapOwner(player, map)) return true;
        if (player.hasPermission("Copyright.bypass")) return true;
        return false;
    }

    public static Player getOwnerOfMap(ItemStack map){
        ItemMeta meta = map.getItemMeta();
        String ownerUUID = meta.getPersistentDataContainer().get(Copyright_OWNER_KEY, PersistentDataType.STRING);
        return Bukkit.getPlayer(UUID.fromString(ownerUUID));
    }

    public static boolean isCopyrightedMap(ItemStack map){
        if(map == null || map.getAmount()==0) return false;
        if (map.getType() != Material.FILLED_MAP) return false;
        if (!map.hasItemMeta()) return false;

        ItemMeta meta = map.getItemMeta();

        return meta.getPersistentDataContainer().has(Copyright_OWNER_KEY, PersistentDataType.STRING);
    }

    public void onEnable() {

        getConfig().addDefault("lang.cant_remove","&cYou can't remove this Copyright.");
        getConfig().addDefault("lang.Copyright_removed","&aCopyright removed.");
        getConfig().addDefault("lang.Copyright_added","&aCopyright added.");
        getConfig().addDefault("lang.not_holding_map","&cYou need to be holding a map to use this command.");
        getConfig().addDefault("lang.cant_duplicate","&cYou can't duplicate this.");
        getConfig().addDefault("lang.cant_Copyright","&cThis map has already been Copyrighted.");
        getConfig().addDefault("lang.remove_no_Copyright","&cThis map hasn't been Copyrighted.");
        getConfig().addDefault("lang.no_perms","&cYou don't have permission to use this command.");
        getConfig().addDefault("lang.Copyright_format","&cBy %player%");
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();

        this.getServer().getPluginManager().registerEvents(new CopyEvents(this), this);
        getCommand("Copyright").setExecutor(new CopyrightCommand(this));
        getCommand("Copyright").setTabCompleter(new CopyrightTab());
        getCommand("Copyrightreload").setExecutor(new ReloadCommand(this));
    }

}