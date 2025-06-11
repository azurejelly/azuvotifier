package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;

public class TestVoteCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;
    private final Parameter.Value<String> serviceParameter;
    private final Parameter.Value<String> usernameParameter;
    private final Parameter.Value<String> addressParameter;

    public TestVoteCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
        this.serviceParameter = Parameter.string().key("service").optional().build();
        this.usernameParameter = Parameter.string().key("username").optional().build();
        this.addressParameter = Parameter.string().key("address").optional().build();
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String timestamp = Long.toString(System.currentTimeMillis(), 10);
        String service = context.one(serviceParameter).orElse("azu.cool");
        String username = context.one(usernameParameter).orElse("azurebytes");
        String address = context.one(addressParameter).orElse("127.0.0.1");

        Vote vote = new Vote(service, username, address, timestamp);
        plugin.onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, "");
        return CommandResult.success();
    }

    public Command.Parameterized build() {
        return Command.builder()
                .addParameter(serviceParameter)
                .addParameter(usernameParameter)
                .addParameter(addressParameter)
                .shortDescription(Component.text("Produces a test vote."))
                .extendedDescription(Component.text("Sends a test vote to the server's listeners"))
                .permission("nuvotifier.testvote")
                .executor(this)
                .build();
    }
}
