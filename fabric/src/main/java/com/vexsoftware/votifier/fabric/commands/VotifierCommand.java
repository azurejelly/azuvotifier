package com.vexsoftware.votifier.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vexsoftware.votifier.fabric.AzuVotifierFabric;
import com.vexsoftware.votifier.fabric.utils.CommandResult;
import com.vexsoftware.votifier.fabric.utils.FabricUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

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
        var version = FabricUtils.getModVersion("azuvotifier");
        var text = Text.literal("This server is running ")
                .withColor(0xf3b0ff)
                .append(
                        Text.literal("azuvotifier")
                                .withColor(0xe867ff)
                ).append(
                        Text.literal(" version ")
                                .withColor(0xf3b0ff)
                ).append(
                        Text.literal(version)
                                .withColor(0xe867ff)
                );

        if (Permissions.check(ctx.getSource(), "azuvotifier.more-info", 2)) {
            var minecraft = FabricUtils.getMinecraftVersion();
            var fabric = FabricUtils.getModVersion("fabric-api");
            var loader = FabricUtils.getModVersion("fabricloader");

            text.append(
                    Text.literal("\nServer: ")
                            .withColor(0xf3b0ff)
            ).append(
                    Text.literal("Minecraft " + minecraft + ", Fabric API " + fabric + ", Fabric Loader " + loader)
                            .withColor(0xe867ff)
            );
        }

        text.append(
                Text.literal("\nModrinth: ")
                        .withColor(0xf3b0ff)
        ).append(
                Text.literal("https://modrinth.com/project/azuvotifier")
                        .withColor(0xe867ff)
        );

        ctx.getSource().sendMessage(text);
        return CommandResult.SUCCESS;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        var plugin = AzuVotifierFabric.getInstance();

        plugin.getLogger().info("Reloading azuvotifier...");
        ctx.getSource().sendMessage(Text.literal("Reloading azuvotifier...").withColor(0xf1e079));

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
                            .withColor(0xcb3a3a)
            );
        } else {
            ctx.getSource().sendMessage(
                    Text.literal("Reload successful.")
                            .withColor(0x79f17b)
            );
        }

        return CommandResult.SUCCESS;
    }

    private static int test(CommandContext<ServerCommandSource> ctx, @Nullable String username, @Nullable String service) {
        if (username == null) {
            username = ctx.getSource().getName();
        }

        if (service == null) {
            service = "azuuure.dev";
        }

        var plugin = AzuVotifierFabric.getInstance();
        var timestamp = String.valueOf(Instant.now().getEpochSecond());
        var vote = new Vote(service, username, "127.0.0.1", timestamp);

        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, vote.getAddress());
        ctx.getSource().sendMessage(
                Text.literal("Sent a test vote for " + username + " using service \"" + service + "\"")
                        .withColor(0x79f17b)
        );

        return CommandResult.SUCCESS;
    }
}
