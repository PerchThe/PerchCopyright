package me.perch.copyright.commands;

import me.perch.copyright.Copyright;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Copyright main;
    public ReloadCommand(Copyright main){
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("Copyright.reload")){
            Copyright.colorCode(main.getConfig().getString("lang.no_perms"));
            return true;
        }

        sender.sendMessage(ChatColor.GOLD+"Reloading config...");
        main.reloadConfig();
        sender.sendMessage(ChatColor.GREEN+"Config reloaded!");

        if(!main.getConfig().getString("lang.Copyright_format").contains("%player%")){
            sender.sendMessage(ChatColor.RED+"Copyright format doesn't have a %player% placeholder!");
        }

        return true;
    }
}