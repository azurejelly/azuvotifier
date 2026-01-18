package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.util.CommonConstants;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;

public class VotifierReloadSubCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;

    public VotifierReloadSubCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Audience audience = context.cause().audience();
        audience.sendMessage(
                Component.text("Reloading azuvotifier...")
                        .color(TextColor.color(CommonConstants.PROCESSING_COLOR))
        );

        if (!plugin.reload()) {
            return CommandResult.error(
                    Component.text("Reload failed. Check the server console for more information.")
                            .color(TextColor.color(CommonConstants.FAILURE_COLOR))
            );
        }


        audience.sendMessage(
                Component.text("Reload successful.")
                        .color(TextColor.color(CommonConstants.SUCCESS_COLOR))
        );

        return CommandResult.success();
    }

    public Command.Parameterized build() {
        return Command.builder()
                .permission("azuvotifier.reload")
                .shortDescription(Component.text("Reloads azuvotifier."))
                .extendedDescription(Component.text("Reloads the azuvotifier plugin."))
                .executor(this)
                .build();
    }
}
