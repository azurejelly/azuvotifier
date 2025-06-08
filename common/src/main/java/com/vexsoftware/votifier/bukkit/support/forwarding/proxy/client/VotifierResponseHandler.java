package com.vexsoftware.votifier.bukkit.support.forwarding.proxy.client;

public interface VotifierResponseHandler {

    void onSuccess();

    void onFailure(Throwable error);
}
