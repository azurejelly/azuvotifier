package com.vexsoftware.votifier.fabric.configuration;

import com.vexsoftware.votifier.util.TokenUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class FabricConfig {

    @Setting
    @Comment("The IP to listen to. Use 0.0.0.0 if you wish to listen to all interfaces on your server (all IP addresses).\n" +
            "This defaults to 0.0.0.0.")
    public String host = "0.0.0.0";

    @Setting
    @Comment("Port to listen for new votes on")
    public int port = 8192;

    @Setting(value = "check-for-updates")
    @Comment("Whether to check for updates when the server starts.")
    public boolean checkForUpdates = true;

    @Setting
    @Comment("Whether or not to print debug messages. In a production system, this should be set to false.\n" +
            "This is useful when initially setting up NuVotifier to ensure votes are being delivered.")
    public boolean debug = true;

    @Setting(value = "disable-v1-protocol")
    @Comment("Setting this option to true will disable handling of Protocol v1 packets. While the old protocol is not secure, this\n" +
            "option is currently not recommended as most voting sites only support the old protocol at present. However, if you are\n" +
            "using NuVotifier's proxy forwarding mechanism, enabling this option will increase your server's security.")
    public boolean disableV1Protocol = false;

    @Setting
    @Comment("All tokens, labeled by the serviceName of each server list.\n" +
            "Default token for all server lists, if another isn't supplied.")
    public Map<String, String> tokens = Collections.singletonMap("default", TokenUtil.newToken());

    @Setting
    @Comment("Configuration section for all vote forwarding to NuVotifier")
    public Forwarding forwarding = new Forwarding();

    @Comment("Experimental settings. You should only change those if you absolutely know what you're doing!")
    public Experimental experimental = new Experimental();

    @ConfigSerializable
    public static class Experimental {

        @Setting("skip-offline-players")
        @Comment("Prevents a vote event from firing if the player who voted isn't online.")
        public boolean skipOfflinePlayers = false;

        @Setting("run-on-vote")
        @Comment("A list of commands to run when a vote is received.")
        public List<String> runOnVote = defaultCommands();

        private static List<String> defaultCommands() {
            List<String> list = new ArrayList<>();
            list.add("give %player% minecraft:diamond 1");
            list.add("say Thanks for voting, %player%!");
            return list;
        }
    }

    @ConfigSerializable
    public static class Forwarding {

        @Setting
        @Comment("Sets whether to set up a remote method for fowarding. Supported methods:\n" +
                "- none - Does not set up a forwarding method.\n" +
                "- pluginMessaging - Sets up plugin messaging.\n" +
                "- redis - Sets up Redis forwarding.")
        public String method = "none";

        @Setting
        public PluginMessaging pluginMessaging = new PluginMessaging();

        @Setting
        public Redis redis = new Redis();

        @ConfigSerializable
        public static class PluginMessaging {

            @Setting
            public String channel = "nuvotifier:votes";
        }

        @ConfigSerializable
        public static class Redis {

            @Setting
            public String address = "127.0.0.1";

            @Setting
            public int port = 6379;

            @Setting
            public String username = "";

            @Setting
            public String password = "";

            @Setting
            public String uri = "";

            @Setting
            public String channel = "nuvotifier:votes";
        }
    }
}
