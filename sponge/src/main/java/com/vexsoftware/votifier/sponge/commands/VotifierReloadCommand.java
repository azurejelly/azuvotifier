package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;

@Deprecated
public class VotifierReloadCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;

    public VotifierReloadCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.cause().audience().sendMessage(Component.text("Reloading NuVotifier...", NamedTextColor.GRAY));

        if (!plugin.reload()) {
            return CommandResult.error(
                    Component.text("Looks like there was a problem reloading NuVotifier, check the console!",
                            NamedTextColor.RED)
            );
        }

        context.cause().audience().sendMessage(Component.text("NuVotifier has been reloaded!", NamedTextColor.GREEN));
        return CommandResult.success();
    }

    public Command.Parameterized build() {
        return Command.builder()
                .permission("nuvotifier.reload")
                .shortDescription(Component.text("Reloads NuVotifier."))
                .extendedDescription(Component.text("Reloads the NuVotifier plugin."))
                .executor(this)
                .build();
    }
}
