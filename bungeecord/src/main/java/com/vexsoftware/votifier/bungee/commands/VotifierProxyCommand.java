package com.vexsoftware.votifier.bungee.commands;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.bungee.util.BungeeColors;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.util.CommonConstants;
import com.vexsoftware.votifier.util.UsernameUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VotifierProxyCommand extends Command implements TabExecutor {

    private final NuVotifierBungee plugin;

    public VotifierProxyCommand(NuVotifierBungee plugin) {
        super(
                "votifier-proxy", null,
                "votifierproxy", "azuvotifier-proxy", "azuvotifierproxy"
        );

        this.plugin = plugin;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            List<String> list = new ArrayList<>();

            if (sender.hasPermission("azuvotifier.test")) {
                list.add("test");
            }

            if (sender.hasPermission("azuvotifier.reload")) {
                list.add("reload");
            }

            return list;
        } else {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("test")) {
                if (!sender.hasPermission("azuvotifier.test")) {
                    return List.of();
                }

                if (args.length == 2) {
                    return ProxyServer.getInstance()
                            .getPlayers()
                            .stream()
                            .map(ProxiedPlayer::getName)
                            .collect(Collectors.toList());
                }

                if (args.length == 3) {
                    return List.of("test.azuuure.dev");
                }
            }

            return List.of();
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            info(sender);
            return;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "test": {
                test(sender, args);
                return;
            }
            case "reload": {
                reload(sender);
                return;
            }
            default: {
                info(sender);
            }
        }
    }

    public void info(CommandSender sender) {
        ChatColor primary = BungeeColors.primary(sender);
        ChatColor secondary = BungeeColors.secondary(sender);

        sender.sendMessage(
                new ComponentBuilder("This server is running ").color(secondary)
                    .append("azuvotifier").color(primary)
                    .append(" version ").color(secondary)
                    .append(plugin.getDescription().getVersion()).color(primary)
                    .build()
        );

        if (sender.hasPermission("azuvotifier.more-info")) {
            ProxyServer server = ProxyServer.getInstance();
            sender.sendMessage(
                    new ComponentBuilder("Server: ").color(secondary)
                            .append(server.getName() + " " + server.getVersion())
                            .color(primary)
                            .build()
            );
        }

        sender.sendMessage(
                new ComponentBuilder("Modrinth: ")
                        .color(secondary)
                        .append(CommonConstants.MODRINTH_URL)
                        .color(primary)
                        .build()
        );
    }

    public void test(CommandSender sender, String[] args) {
        if (handleMissingPermission(sender, "azuvotifier.test")) {
            return;
        }

        String target = args.length >= 2 ? args[1] : sender.getName();
        String service = args.length >= 3 ? args[2] : CommonConstants.DEFAULT_TEST_SERVICE;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        if (!UsernameUtil.isValid(target)) {
            TextComponent component = new TextComponent("You must provide a valid Minecraft username.");
            component.setColor(BungeeColors.failure(sender));
            sender.sendMessage(component);
            return;
        }

        Vote vote = new Vote(service, target, CommonConstants.DEFAULT_TEST_ADDRESS, timestamp);
        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, vote.getAddress());

        TextComponent component = new TextComponent("Sent a test vote for " + target + " with service " + service);
        component.setColor(BungeeColors.success(sender));
        sender.sendMessage(component);
    }

    public void reload(CommandSender sender) {
        if (handleMissingPermission(sender, "azuvotifier.reload")) {
            return;
        }

        TextComponent processing = new TextComponent("Reloading azuvotifier...");
        processing.setColor(BungeeColors.processing(sender));
        sender.sendMessage(processing);

        if (!plugin.reload()) {
            TextComponent failure = new TextComponent("Reload failed. Check the proxy console for more information.");
            failure.setColor(BungeeColors.failure(sender));
            sender.sendMessage(failure);
        } else {
            TextComponent success = new TextComponent("Reload successful.");
            success.setColor(BungeeColors.success(sender));
            sender.sendMessage(success);
        }
    }

    public boolean handleMissingPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return false;
        }

        TextComponent component = new TextComponent("You do not have permission to execute this command.");
        component.setColor(BungeeColors.failure(sender));
        sender.sendMessage(component);
        return true;
    }
}
