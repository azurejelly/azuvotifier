package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.util.CommonConstants;
import com.vexsoftware.votifier.util.UsernameUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;

import java.time.Instant;

public class VotifierTestSubCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;
    private final Parameter.Value<String> service;
    private final Parameter.Value<String> username;

    public VotifierTestSubCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
        this.service = Parameter.string().key("service").optional().build();
        this.username = Parameter.string().key("username").optional().build();
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Audience audience = context.cause().audience();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String service = context.one(this.service).orElse("azuuure.dev");
        String caller = context.one(this.username)
                .orElse(
                        context.cause()
                                .first(Player.class)
                                .map(Player::name)
                                .orElse("Console")
                );

        if (!UsernameUtil.isValid(caller)) {
            return CommandResult.error(
                    Component.text("You must provide a valid Minecraft username.")
                            .color(TextColor.color(CommonConstants.FAILURE_COLOR))
            );
        }

        Vote vote = new Vote(service, caller, CommonConstants.DEFAULT_TEST_ADDRESS, timestamp);
        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, vote.getAddress());

        audience.sendMessage(
                Component.text("Sent a test vote for " + caller + " with service " + service)
                        .color(TextColor.color(CommonConstants.SUCCESS_COLOR))
        );

        return CommandResult.success();
    }

    public Command.Parameterized build() {
        return Command.builder()
                .addParameters(service, username)
                .permission("azuvotifier.test")
                .shortDescription(Component.text("Produces a test vote."))
                .extendedDescription(Component.text("Sends a test vote to the server's listeners"))
                .executor(this)
                .build();
    }
}
