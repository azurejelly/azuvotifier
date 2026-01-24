package com.vexsoftware.votifier.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.ProxyVersion;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.util.CommonConstants;
import com.vexsoftware.votifier.util.UsernameUtil;
import com.vexsoftware.votifier.velocity.NuVotifierVelocity;
import com.vexsoftware.votifier.velocity.utils.VelocityConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VotifierProxyCommand implements SimpleCommand {

    private final NuVotifierVelocity plugin;

    public VotifierProxyCommand(NuVotifierVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            CommandSource source = invocation.source();
            List<String> list = new ArrayList<>();

            if (source.hasPermission("azuvotifier.test")) {
                list.add("test");
            }

            if (source.hasPermission("azuvotifier.reload")) {
                list.add("reload");
            }

            return list;
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("test")) {
            CommandSource source = invocation.source();
            if (!source.hasPermission("azuvotifier.test")) {
                return List.of();
            }

            if (args.length == 2) {
                return plugin.getServer()
                        .getAllPlayers()
                        .stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());
            }

            if (args.length == 3) {
                return List.of("test.azuuure.dev");
            }
        }

        return List.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            info(source);
            return;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "test": {
                test(source, args);
                return;
            }
            case "reload": {
                reload(source);
                return;
            }
            default: {
                info(source);
            }
        }
    }

    public void info(CommandSource source) {
        TextColor secondary = TextColor.color(CommonConstants.SECONDARY_COLOR);

        // very hacky workaround for legacy clients showing the entire message in light blue.
        // adventure maps every other color correctly, so this is the only one that we need to
        // change depending on the client's version
        if (source instanceof Player) {
            Player player = (Player) source;

            if (player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_16)) {
                // fallback to white on anything below 1.16
                secondary = NamedTextColor.WHITE;
            }
        } else {
            // just in case, we'll also use white for the server console
            secondary = NamedTextColor.WHITE;
        }

        source.sendMessage(
                Component.text("This proxy is running ")
                        .color(TextColor.color(secondary))
                        .append(
                                Component.text("azuvotifier")
                                        .color(TextColor.color(CommonConstants.PRIMARY_COLOR))
                        ).append(
                                Component.text(" version ")
                                        .color(secondary)
                        ).append(
                                Component.text(VelocityConstants.VERSION)
                                        .color(TextColor.color(CommonConstants.PRIMARY_COLOR))
                        )
        );

        if (source.hasPermission("azuvotifier.more-info")) {
            ProxyVersion version = plugin.getServer().getVersion();
            source.sendMessage(
                    Component.text("Proxy: ")
                            .color(secondary)
                            .append(
                                    Component.text(version.getName() + " " + version.getVersion())
                                            .color(TextColor.color(CommonConstants.PRIMARY_COLOR))
                            )
            );
        }

        source.sendMessage(
                Component.text("Modrinth: ")
                        .color(secondary)
                        .append(
                                Component.text(CommonConstants.MODRINTH_URL)
                                        .color(TextColor.color(CommonConstants.PRIMARY_COLOR))
                        )
        );
    }

    public void reload(CommandSource source) {
        if (handleMissingPermission(source, "azuvotifier.reload")) {
            return;
        }

        source.sendMessage(
                Component.text("Reloading azuvotifier...")
                        .color(TextColor.color(CommonConstants.PROCESSING_COLOR))
        );

        if (!plugin.reload()) {
            source.sendMessage(
                    Component.text("Reload failed. Check the proxy console for more information.")
                            .color(TextColor.color(CommonConstants.FAILURE_COLOR))
            );
        } else {
            source.sendMessage(
                    Component.text("Reload successful.")
                            .color(TextColor.color(CommonConstants.SUCCESS_COLOR))
            );
        }
    }

    public void test(CommandSource source, String[] args) {
        if (handleMissingPermission(source, "azuvotifier.test")) {
            return;
        }

        String caller;
        if (source instanceof Player) {
            Player player = (Player) source;
            caller = player.getUsername();
        } else {
            caller = "Console";
        }

        String target = args.length >= 2 ? args[1] : caller;
        String service = args.length >= 3 ? args[2] : CommonConstants.DEFAULT_TEST_SERVICE;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        if (!UsernameUtil.isValid(target)) {
            source.sendMessage(
                    Component.text("You must provide a valid Minecraft username.")
                            .color(TextColor.color(CommonConstants.FAILURE_COLOR))
            );

            return;
        }

        Vote vote = new Vote(service, target, CommonConstants.DEFAULT_TEST_ADDRESS, timestamp);
        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, CommonConstants.DEFAULT_TEST_ADDRESS);

        source.sendMessage(
                Component.text("Sent a test vote for " + target + " with service " + service)
                        .color(TextColor.color(CommonConstants.SUCCESS_COLOR))
        );
    }

    public boolean handleMissingPermission(CommandSource source, String permission) {
        if (source.hasPermission(permission)) {
            return false;
        }

        source.sendMessage(
                Component.text("You do not have permission to execute this command.")
                        .color(TextColor.color(CommonConstants.FAILURE_COLOR))
        );

        return true;
    }
}
