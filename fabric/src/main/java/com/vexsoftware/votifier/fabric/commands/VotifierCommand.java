package com.vexsoftware.votifier.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.vexsoftware.votifier.fabric.VotifierFabric;
import com.vexsoftware.votifier.fabric.utils.CommandResult;
import com.vexsoftware.votifier.fabric.utils.FabricUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class VotifierCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // TODO: use a proper permission system
        dispatcher.register(
                CommandManager.literal("votifier")
                        .executes(VotifierCommand::info)
                        .then(CommandManager.literal("reload")
                                .requires(src -> src.hasPermissionLevel(4))
                                .executes(VotifierCommand::reload)
                        ).then(CommandManager.literal("test") // i don't like this
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes((ctx) -> test(ctx, null, null))
                                .then(CommandManager.argument("target", StringArgumentType.string())
                                        .executes((ctx) ->
                                                        test(ctx, StringArgumentType.getString(ctx, "target"), null)
                                        ).then(CommandManager.argument("service", StringArgumentType.string())
                                                .executes((ctx) ->
                                                        test(ctx,
                                                                StringArgumentType.getString(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "service")
                                                        )
                                                )
                                        )
                                )
                        )
        );
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

        if (ctx.getSource().hasPermissionLevel(4)) {
            var minecraft = FabricUtils.getMinecraftVersion();
            var fabric = FabricUtils.getModVersion("fabric-api");

            text.append(
                    Text.literal("\nServer: ")
                            .withColor(0xf3b0ff)
            ).append(
                    Text.literal("Minecraft " + minecraft + ", Fabric API " + fabric)
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
        var plugin = VotifierFabric.getInstance();

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

        var plugin = VotifierFabric.getInstance();
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
