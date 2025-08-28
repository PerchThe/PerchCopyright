package me.perch.copyright;

import me.perch.copyright.commands.ReloadCommand;
import me.perch.copyright.commands.CopyrightCommand;
import me.perch.copyright.commands.CopyrightTab;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Copyright extends JavaPlugin implements Listener {

    // ——— Static access ———
    private static Copyright INSTANCE;
    public static Copyright getInstance() { return INSTANCE; }

    // Current key (namespace = your plugin id)
    public static NamespacedKey OWNER_KEY;

    // Legacy keys we want to recognize & migrate (add more if you had them)
    private static NamespacedKey[] LEGACY_OWNER_KEYS;

    // Hex color translate (supports #RRGGBB -> &x&R&R&G&G&B&B)
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    public static String colorCode(String message) {
        if (message == null || message.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
            StringBuilder builder = new StringBuilder();
            for (char c : replaceSharp.toCharArray()) builder.append("&").append(c);
            message = message.replace(hexCode, builder.toString());
            matcher = HEX_PATTERN.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ——— Map copyright helpers (null-safe) ———

    /** True iff this is a filled map with ANY recognized owner tag (current or legacy). */
    public static boolean isCopyrightedMap(ItemStack map) {
        return getOwnerUuid(map) != null;
    }

    /** Read UUID from current key; if absent, try legacy keys (and migrate); else null. */
    public static UUID getOwnerUuid(ItemStack map) {
        if (map == null || map.getType() != Material.FILLED_MAP) return null;
        if (!map.hasItemMeta()) return null;
        ItemMeta meta = map.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1) Current format (STRING at OWNER_KEY)
        String raw = pdc.get(OWNER_KEY, PersistentDataType.STRING);
        if (raw != null && !raw.isEmpty()) {
            try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) {}
        }

        // 2) Legacy formats → try & migrate
        UUID legacy = readUuidFromAnyLegacyKey(pdc);
        if (legacy != null) {
            pdc.set(OWNER_KEY, PersistentDataType.STRING, legacy.toString());
            // Optional cleanup: remove legacy keys
            for (NamespacedKey k : LEGACY_OWNER_KEYS) pdc.remove(k);
            map.setItemMeta(meta);
            return legacy;
        }

        return null;
    }

    private static UUID readUuidFromAnyLegacyKey(PersistentDataContainer pdc) {
        for (NamespacedKey key : LEGACY_OWNER_KEYS) {
            // STRING
            String s = pdc.get(key, PersistentDataType.STRING);
            if (s != null && !s.isEmpty()) {
                try { return UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
            }
            // BYTE_ARRAY (16)
            byte[] ba = pdc.get(key, PersistentDataType.BYTE_ARRAY);
            if (ba != null && ba.length == 16) {
                ByteBuffer bb = ByteBuffer.wrap(ba);
                return new UUID(bb.getLong(), bb.getLong());
            }
            // LONG_ARRAY (2)
            long[] la = pdc.get(key, PersistentDataType.LONG_ARRAY);
            if (la != null && la.length == 2) {
                return new UUID(la[0], la[1]);
            }
        }
        return null;
    }

    /** True if the given player is the recorded owner of the map. */
    public static boolean isMapOwner(Player player, ItemStack map) {
        if (player == null) return false;
        UUID id = getOwnerUuid(map);
        return id != null && id.equals(player.getUniqueId());
    }

    /**
     * Permission gate for copying (manual only).
     * - Owner may copy
     * - Players with "copyright.bypass" may copy
     * - Non-player actors: never
     */
    public static boolean canCopyMap(HumanEntity who, ItemStack map) {
        if (!(who instanceof Player p)) return false;
        if (isMapOwner(p, map)) return true;
        return p.hasPermission("copyright.bypass");
    }

    // ——— Plugin lifecycle ———
    @Override
    public void onEnable() {
        INSTANCE = this;
        OWNER_KEY = new NamespacedKey(this, "owner");
        // Legacy owner keys (from your old class): "tradermarker:owner"
        LEGACY_OWNER_KEYS = new NamespacedKey[] {
                new NamespacedKey("tradermarker","owner")
                // add more if you previously used other keys
        };

        // Default messages
        getConfig().addDefault("lang.cant_remove", "&cYou can't remove this Copyright.");
        getConfig().addDefault("lang.Copyright_removed", "&aCopyright removed.");
        getConfig().addDefault("lang.Copyright_added", "&aCopyright added.");
        getConfig().addDefault("lang.not_holding_map", "&cYou need to be holding a map to use this command.");
        getConfig().addDefault("lang.cant_duplicate", "&cYou can't duplicate this.");
        getConfig().addDefault("lang.cant_Copyright", "&cThis map has already been Copyrighted.");
        getConfig().addDefault("lang.remove_no_Copyright", "&cThis map hasn't been Copyrighted.");
        getConfig().addDefault("lang.no_perms", "&cYou don't have permission to use this command.");
        getConfig().addDefault("lang.Copyright_format", "&cBy %player%");
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Listeners
        getServer().getPluginManager().registerEvents(new CopyEvents(this), this);

        // Paper-only listeners (if available)
        try {
            Class.forName("io.papermc.paper.event.player.CartographyItemEvent");
            Class.forName("org.bukkit.event.block.CrafterCraftEvent");
            getServer().getPluginManager().registerEvents(new CopyEventsPaper(this), this);
            getLogger().info("[PerchCopyright] Paper integrations enabled.");
        } catch (ClassNotFoundException ignored) {
            getLogger().info("[PerchCopyright] Running without Paper-only integrations.");
        }

        // Commands
        if (getCommand("Copyright") != null) {
            getCommand("Copyright").setExecutor(new CopyrightCommand(this));
            getCommand("Copyright").setTabCompleter(new CopyrightTab());
        } else {
            getLogger().warning("Command 'Copyright' not defined in plugin.yml");
        }
        if (getCommand("Copyrightreload") != null) {
            getCommand("Copyrightreload").setExecutor(new ReloadCommand(this));
        } else {
            getLogger().warning("Command 'Copyrightreload' not defined in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        // No teardown necessary
    }
}
