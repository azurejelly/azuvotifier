package com.vexsoftware.votifier.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.vexsoftware.votifier.velocity.NuVotifierVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VotifierReloadCommand implements SimpleCommand {

    private final NuVotifierVelocity plugin;

    public VotifierReloadCommand(NuVotifierVelocity plugin) {
        this.plugin = plugin;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        sender.sendMessage(Component.text("Reloading NuVotifier...", NamedTextColor.GRAY));
        
        if (plugin.reload()) {
            sender.sendMessage(Component.text("NuVotifier has been reloaded!", NamedTextColor.DARK_GREEN));
        } else {
            sender.sendMessage(Component.text("Looks like there was a problem reloading NuVotifier, check the console!", NamedTextColor.DARK_RED));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("nuvotifier.reload");
    }
}
