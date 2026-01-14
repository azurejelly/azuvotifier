package com.vexsoftware.votifier.sponge.commands;

import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.sponge.util.Constants;
import com.vexsoftware.votifier.sponge.util.SpongeUtil;
import com.vexsoftware.votifier.util.CommonConstants;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.Platform;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;

public class VotifierCommand implements CommandExecutor {

    private final NuVotifierSponge plugin;

    public VotifierCommand(NuVotifierSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Audience audience = context.cause().audience();
        Component text = Component.text("This server is running ")
                .color(TextColor.color(0xF3B0FF))
                .append(
                        Component.text("azuvotifier")
                                .color(TextColor.color(0xE867FF))
                ).append(
                        Component.text(" version ")
                                .color(TextColor.color(0xF3B0FF))
                ).append(
                        Component.text(SpongeUtil.getPluginVersion(Constants.PLUGIN_ID))
                                .color(TextColor.color(0xE867FF))
                );

        if (context.cause().hasPermission("azuvotifier.more-info")) {
            String minecraft = SpongeUtil.getMinecraftVersion();
            String api = SpongeUtil.getPlatformComponentVersion(Platform.Component.API);
            String implementation = SpongeUtil.getPlatformComponentName(Platform.Component.IMPLEMENTATION)
                    + " " + SpongeUtil.getPlatformComponentVersion(Platform.Component.IMPLEMENTATION);

            text = text.append(
                    Component.text("\nServer: ")
                            .color(TextColor.color(0xF3B0FF))
            ).append(
                    Component.text("Minecraft " + minecraft + ", Sponge API " + api + ", " + implementation)
                            .color(TextColor.color(0xE867FF))
            );
        }

        text = text.append(
                Component.text("\nModrinth: ")
                        .color(TextColor.color(0xF3B0FF))
        ).append(
                Component.text(CommonConstants.MODRINTH_URL)
                        .clickEvent(ClickEvent.openUrl(CommonConstants.MODRINTH_URL))
                        .color(TextColor.color(0xE867FF))
        );

        audience.sendMessage(text);
        return CommandResult.success();
    }

    public Command.Parameterized build() {
        return Command.builder()
                .shortDescription(Component.text("Main azuvotifier command."))
                .extendedDescription(Component.text("Shows information about azuvotifier."))
                .executor(this)
                .addChild(new VotifierTestSubCommand(plugin).build(), "test", "testvote", "test-vote", "t")
                .addChild(new VotifierReloadSubCommand(plugin).build(), "reload", "r")
                .build();
    }
}
