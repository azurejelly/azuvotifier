package com.vexsoftware.votifier.bukkit.commands;

import com.vexsoftware.votifier.bukkit.NuVotifierBukkit;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class TestVoteCommand implements CommandExecutor {

    private final NuVotifierBukkit plugin;

    public TestVoteCommand(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command c, @NotNull String l, String @NotNull [] args) {
        if (!sender.hasPermission("nuvotifier.testvote")) {
            sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do this!");
            return true;
        }

        Vote vote;
        try {
            vote = ArgsToVote.parse(args);
            plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, "localhost.test");
            sender.sendMessage(ChatColor.GREEN + "Test vote executed: " + vote);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.DARK_RED + "Error while parsing arguments to create test vote: " + e.getMessage());
            sender.sendMessage(ChatColor.GRAY + "Usage hint: /testvote [username] [serviceName] [username] [address] [localTimestamp] [timestamp]");
        }

        return true;
    }
}
