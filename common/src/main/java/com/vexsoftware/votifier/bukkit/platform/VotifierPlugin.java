package com.vexsoftware.votifier.bukkit.platform;

import com.vexsoftware.votifier.bukkit.VoteHandler;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierScheduler;
import io.netty.util.AttributeKey;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

public interface VotifierPlugin extends VoteHandler {

    AttributeKey<VotifierPlugin> KEY = AttributeKey.valueOf("votifier_plugin");

    Map<String, Key> getTokens();

    KeyPair getProtocolV1Key();

    LoggingAdapter getPluginLogger();

    VotifierScheduler getScheduler();

    default boolean isDebug() {
        return false;
    }
}
