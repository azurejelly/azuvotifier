package com.vexsoftware.votifier.fabric.event.listener;

import com.mojang.brigadier.CommandDispatcher;
import com.vexsoftware.votifier.fabric.commands.VotifierCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandRegistrationCallbackListener implements CommandRegistrationCallback {

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment env) {
        VotifierCommand.register(dispatcher);
    }
}
