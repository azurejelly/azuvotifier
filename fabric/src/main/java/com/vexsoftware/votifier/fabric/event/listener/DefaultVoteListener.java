package com.vexsoftware.votifier.fabric.event.listener;

import com.vexsoftware.votifier.fabric.configuration.FabricConfig;
import com.vexsoftware.votifier.fabric.configuration.loader.ConfigLoader;
import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.fabric.platform.provider.MinecraftServerProvider;
import com.vexsoftware.votifier.model.Vote;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class DefaultVoteListener implements VoteListener {

    @Override
    public void onVote(Vote vote) {
        FabricConfig cfg = ConfigLoader.get();
        List<String> commands = cfg.experimental.runOnVote;

        if (commands == null || commands.isEmpty()) {
            return;
        }

        MinecraftServer server = MinecraftServerProvider.getServer();
        var commandManager = server.getCommandManager();
        var source = server.getCommandSource();

        for (String command : commands) {
            command = command
                    .replace("%player%", vote.getUsername())
                    .replace("%service%", vote.getServiceName())
                    .replace("%address%", vote.getAddress())
                    .replace("%timestamp%", vote.getTimestamp());

            commandManager.parseAndExecute(source, command);
        }
    }
}
