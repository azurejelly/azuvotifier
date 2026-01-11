package com.vexsoftware.votifier.platform.forwarding.listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.forwarding.sink.ForwardingVoteSink;

/**
 * Represents a method to call when a vote is forwarded through the {@link ForwardingVoteSink}.
 */
public interface ForwardedVoteListener {

    /**
     * Called whenever a vote is forwarded from a {@link ForwardingVoteSink}
     *
     * @param v Vote that was forwarded
     */
    void onForward(Vote v);
}
