package com.vexsoftware.votifier.bungee.commands;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class VotifierReloadCommand extends Command {

    private final NuVotifierBungee plugin;

    private static final BaseComponent RELOADING = new TextComponent("Reloading NuVotifier...");
    private static final BaseComponent RELOADED = new TextComponent("NuVotifier has been reloaded!");
    private static final BaseComponent PROBLEM = new TextComponent("Looks like there was a problem reloading NuVotifier, check the console!");
    private static final BaseComponent PERMISSION = new TextComponent("You do not have permission to do this!");

    static {
        RELOADING.setColor(ChatColor.GRAY);
        RELOADED.setColor(ChatColor.DARK_GREEN);
        PROBLEM.setColor(ChatColor.DARK_RED);
        PERMISSION.setColor(ChatColor.DARK_RED);
    }

    public VotifierReloadCommand(NuVotifierBungee plugin) {
        super("pnvreload", "nuvotifier.reload", "bnvreload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuvotifier.reload")) {
            sender.sendMessage(PERMISSION);
            return;
        }

        sender.sendMessage(RELOADING);

        if (plugin.reload()) {
            sender.sendMessage(RELOADED);
        } else {
            sender.sendMessage(PROBLEM);
        }
    }
}
