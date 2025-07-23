package com.vexsoftware.votifier.commands;

import com.vexsoftware.votifier.NuVotifierBukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VotifierReloadCommand implements CommandExecutor {

    private final NuVotifierBukkit plugin;

    public VotifierReloadCommand(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command c, @NotNull String l, String @NotNull [] args) {
        if (!sender.hasPermission("nuvotifier.reload")) {
            sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do this!");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Reloading NuVotifier...");

        if (plugin.reload()) {
            sender.sendMessage(ChatColor.DARK_GREEN + "NuVotifier has been reloaded!");
        } else {
            sender.sendMessage(
                    ChatColor.DARK_RED + "Looks like there was a problem reloading NuVotifier, check the console!"
            );
        }

        return true;
    }
}
