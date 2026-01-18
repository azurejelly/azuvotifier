package com.vexsoftware.votifier.commands;

import com.vexsoftware.votifier.util.CommonConstants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VotifierTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // Sender is trying to obtain a list of subcommands
            List<String> subcommands = new ArrayList<>();

            // Only add subcommands the player can execute
            if (sender.hasPermission("azuvotifier.reload")) {
                subcommands.add("reload");
            }

            if (sender.hasPermission("azuvotifier.test")) {
                subcommands.add("test");
            }

            return subcommands;
        } else {
            // Sender is trying to autocomplete a subcommand
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "test":
                case "t": {
                    if (!sender.hasPermission("azuvotifier.test")) {
                        // Attempting to autocomplete without enough permissions
                        return List.of();
                    }

                    if (args.length == 2) {
                        // /votifier test
                        //
                        // By returning null, Bukkit will automatically auto-complete
                        // with a list of online players. This is exactly what we want.
                        return null;
                    } else if (args.length == 3) {
                        // /votifier test player
                        return List.of(CommonConstants.DEFAULT_TEST_SERVICE);
                    } else {
                        // Unknown argument or one that we cannot autocomplete
                        return List.of();
                    }
                }
                default: {
                    // Nothing to autocomplete
                    return List.of();
                }
            }
        }
    }
}
