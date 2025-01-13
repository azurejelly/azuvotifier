package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class VotifierReloadCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;

    public VotifierReloadCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        src.sendMessage(Text.builder("Reloading NuVotifier...").color(TextColors.GRAY).build());

        return plugin.reload()
                ? CommandResult.success()
                : CommandResult.empty();
    }
}
