package com.vexsoftware.votifier.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vexsoftware.votifier.fabric.AzuVotifierFabric;
import com.vexsoftware.votifier.fabric.util.CommandResult;
import com.vexsoftware.votifier.fabric.util.FabricUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.util.CommonConstants;
import com.vexsoftware.votifier.util.UsernameUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class VotifierCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String command : List.of("azuvotifier", "votifier")) {
            // Main command
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(command)
                    .executes(VotifierCommand::info);

            // Reload subcommand
            root.then(
                    CommandManager.literal("reload")
                            .requires(Permissions.require("azuvotifier.reload", 4))
                            .executes(VotifierCommand::reload)
            );

            // Test vote subcommand
            root.then(
                    CommandManager.literal("test")
                            .requires(Permissions.require("azuvotifier.test-vote", 4))
                            .executes((ctx) -> test(ctx, null, null))
                            .then(CommandManager.argument("target", StringArgumentType.string())
                                    .executes((ctx) ->
                                            test(ctx, StringArgumentType.getString(ctx, "target"), null))
                                    .then(CommandManager.argument("service", StringArgumentType.string())
                                            .executes((ctx) -> test(ctx,
                                                    StringArgumentType.getString(ctx, "target"),
                                                    StringArgumentType.getString(ctx, "service")
                                            ))
                                    )
                            )
            );

            dispatcher.register(root);
        }
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        var version = FabricUtil.getModVersion("azuvotifier");
        var text = Text.literal("This server is running ")
                .withColor(CommonConstants.SECONDARY_COLOR)
                .append(
                        Text.literal("azuvotifier")
                                .withColor(CommonConstants.PRIMARY_COLOR)
                ).append(
                        Text.literal(" version ")
                                .withColor(CommonConstants.SECONDARY_COLOR)
                ).append(
                        Text.literal(version)
                                .withColor(CommonConstants.PRIMARY_COLOR)
                );

        if (Permissions.check(ctx.getSource(), "azuvotifier.more-info", 2)) {
            var minecraft = FabricUtil.getMinecraftVersion();
            var fabric = FabricUtil.getModVersion("fabric-api");
            var loader = FabricUtil.getModVersion("fabricloader");

            text.append(
                    Text.literal("\nServer: ")
                            .withColor(CommonConstants.SECONDARY_COLOR)
            ).append(
                    Text.literal("Minecraft " + minecraft + ", Fabric API " + fabric + ", Fabric Loader " + loader)
                            .withColor(CommonConstants.PRIMARY_COLOR)
            );
        }

        text.append(
                Text.literal("\nModrinth: ")
                        .withColor(CommonConstants.SECONDARY_COLOR)
        ).append(
                Text.literal(CommonConstants.MODRINTH_URL)
                        .styled(s -> s.withClickEvent(
                                new ClickEvent.OpenUrl(
                                        URI.create(CommonConstants.MODRINTH_URL)
                                )
                        )).withColor(CommonConstants.PRIMARY_COLOR)
        );

        ctx.getSource().sendMessage(text);
        return CommandResult.SUCCESS;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        var plugin = AzuVotifierFabric.getInstance();

        plugin.getLogger().info("Reloading azuvotifier...");
        ctx.getSource().sendMessage(
                Text.literal("Reloading azuvotifier...")
                        .withColor(CommonConstants.PROCESSING_COLOR)
        );

        try {
            plugin.halt();
        } catch (RuntimeException ex) {
            plugin.getLogger().warn("An error occurred while halting Votifier. This might be fine!", ex);
        }

        if (!plugin.init()) {
            plugin.getLogger().error("An error occurred while attempting to reload azuvotifier. Please check for any errors above.");

            try {
                plugin.halt();
            } catch (RuntimeException ex) {
                plugin.getLogger().error("An error occurred while re-halting Votifier. The mod is in an unstable state!", ex);
            }

            ctx.getSource().sendMessage(
                    Text.literal("Reload failed. Check the server console for more information.")
                            .withColor(CommonConstants.FAILURE_COLOR)
            );
        } else {
            ctx.getSource().sendMessage(
                    Text.literal("Reload successful.")
                            .withColor(CommonConstants.SUCCESS_COLOR)
            );
        }

        return CommandResult.SUCCESS;
    }

    private static int test(CommandContext<ServerCommandSource> ctx, @Nullable String username, @Nullable String service) {
        if (username == null) {
            username = ctx.getSource().getName();
        }

        if (!UsernameUtil.isValid(username)) {
            ctx.getSource().sendMessage(
                    Text.literal("You must provide a valid Minecraft username.")
                            .withColor(CommonConstants.FAILURE_COLOR)
            );

            return CommandResult.SUCCESS; // not really
        }

        if (service == null) {
            service = CommonConstants.DEFAULT_TEST_SERVICE;
        }

        var plugin = AzuVotifierFabric.getInstance();
        var timestamp = String.valueOf(Instant.now().getEpochSecond());
        var vote = new Vote(service, username, CommonConstants.DEFAULT_TEST_ADDRESS, timestamp);

        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, vote.getAddress());
        ctx.getSource().sendMessage(
                Text.literal("Sent a test vote for " + username + " with service " + service)
                        .withColor(CommonConstants.SUCCESS_COLOR)
        );

        return CommandResult.SUCCESS;
    }
}
