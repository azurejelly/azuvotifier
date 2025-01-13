package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.util.ArgsToVote;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collection;

public class TestVoteCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;

    public TestVoteCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
        try {
            Collection<String> a = args.getAll("args");
            Vote v = ArgsToVote.parse(a.toArray(new String[0]));
            plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
            return CommandResult.success();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Text.builder("Error while parsing arguments to create test vote: " + e.getMessage()).color(TextColors.DARK_RED).build());
            sender.sendMessage(Text.builder("Usage hint: /testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]").color(TextColors.GRAY).build());
            return CommandResult.empty();
        }

    }
}
