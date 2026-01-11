package com.vexsoftware.votifier.bungee.commands;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class TestVoteCommand extends Command {

    private final NuVotifierBungee plugin;

    private static final BaseComponent PERMISSION = new TextComponent("You do not have permission to do this!");
    private static final BaseComponent USAGE = new TextComponent("Usage hint: /ptestvote [username] [serviceName] [username] [address] [localTimestamp] [timestamp]");

    static {
        USAGE.setColor(ChatColor.GRAY);
    }

    public TestVoteCommand(NuVotifierBungee plugin) {
        super("ptestvote", "nuvotifier.testvote");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuvotifier.testvote")) {
            sender.sendMessage(PERMISSION);
            return;
        }

        Vote vote;
        try {
            vote = ArgsToVote.parse(args);
        } catch (IllegalArgumentException e) {
            TextComponent c = new TextComponent("Error while parsing arguments to create test vote: " + e.getMessage());
            c.setColor(ChatColor.DARK_RED);
            sender.sendMessage(c);
            sender.sendMessage(USAGE);
            return;
        }

        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, "localhost.test");
        TextComponent c = new TextComponent("Test vote executed: " + vote);
        c.setColor(ChatColor.GREEN);
        sender.sendMessage(c);
    }
}
