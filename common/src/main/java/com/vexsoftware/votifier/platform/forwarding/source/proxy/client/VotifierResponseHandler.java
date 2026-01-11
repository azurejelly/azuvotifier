package com.vexsoftware.votifier.platform.forwarding.source.proxy.client;

public interface VotifierResponseHandler {

    void onSuccess();

    void onFailure(Throwable error);
}
