package com.vexsoftware.votifier.fabric.platform.forwarding;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;

public class FabricMessagingForwardingSink extends AbstractPluginMessagingForwardingSink {

    public FabricMessagingForwardingSink(ForwardedVoteListener listener, LoggingAdapter logger) {
        super(listener, logger);
    }

    @Override
    public void init() {

    }

    @Override
    public void halt() {

    }
}
