package com.vexsoftware.votifier.commands;

import com.vexsoftware.votifier.NuVotifierBukkit;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.util.BukkitColors;
import com.vexsoftware.votifier.util.CommonConstants;
import io.papermc.paper.ServerBuildInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class VotifierCommand implements CommandExecutor {

    private final NuVotifierBukkit plugin;

    public VotifierCommand(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Command framework? I hardly know her!
        if (args.length == 0) {
            execute(sender);
            return true;
        } else {
            switch (args[0].trim().toLowerCase()) {
                case "r":
                case "reload": {
                    if (handleMissingPermission(sender, "azuvotifier.reload")) {
                        return false;
                    }

                    reload(sender);
                    return true;
                }
                case "t":
                case "test": {
                    if (handleMissingPermission(sender, "azuvotifier.test")) {
                        return false;
                    }

                    test(sender, args);
                    return true;
                }
                default: {
                    execute(sender);
                    return false;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void execute(@NotNull CommandSender sender) {
        sender.sendMessage(
                BukkitColors.secondary() + "This server is running "
                        + BukkitColors.primary() + "azuvotifier"
                        + BukkitColors.secondary() + " version "

                        // PluginDescriptionFile is deprecated, but other alternatives
                        // such as Paper's PluginMeta are still marked as experimental and
                        // break support for older versions of Minecraft
                        + BukkitColors.primary() + plugin.getDescription().getVersion()
        );

        if (sender.hasPermission("azuvotifier.more-info")) {
            String server;

            try {
                // Paper provides a nice ServerBuildInfo class on newer versions. We can try to use it
                // behind a try/catch block to preserve compatibility for older versions
                ServerBuildInfo info = ServerBuildInfo.buildInfo();
                server = info.brandName() + " " + info.asString(ServerBuildInfo.StringRepresentation.VERSION_FULL);
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                // ServerBuildInfo is unavailable, use Bukkit#getName() for the server software name
                // (i.e. Paper, Purpur) and Bukkit#getVersion() for its version.
                server = Bukkit.getName() + " " + Bukkit.getVersion();
            }

            sender.sendMessage(BukkitColors.secondary() + "Server: " + BukkitColors.primary() + server);
        }

        sender.sendMessage(BukkitColors.secondary() + "Modrinth: " + BukkitColors.primary() + CommonConstants.MODRINTH_URL);
    }

    public void reload(@NotNull CommandSender sender) {
        sender.sendMessage(BukkitColors.processing() + "Reloading azuvotifier...");

        if (!plugin.reload()) {
            sender.sendMessage(BukkitColors.failure() + "Reload failed. Check the server console for more information.");
        } else {
            sender.sendMessage(BukkitColors.success() + "Reload successful.");
        }
    }

    public void test(@NotNull CommandSender sender, @NotNull String[] args) {
        String target = args.length >= 2 ? args[1] : sender.getName();
        String service = args.length >= 3 ? args[2] : CommonConstants.DEFAULT_TEST_SERVICE;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Vote vote = new Vote(service, target, CommonConstants.DEFAULT_TEST_ADDRESS, timestamp);
        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, vote.getAddress());

        sender.sendMessage(BukkitColors.success() + "Sent a test vote for " + target + " with service " + service);
    }

    public boolean handleMissingPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return false;
        }

        sender.sendMessage(BukkitColors.failure() + "You do not have permission to execute this command.");
        return true;
    }
}
