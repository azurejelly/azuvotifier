package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;

public interface VoteHandler {

    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) throws Exception;

    default void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        throw new RuntimeException("Unimplemented onError handler");
    }

    default void onConnectionReset(String remoteAddress, boolean voteAlreadyCompleted) {
        // Log connection reset at INFO level since it's a normal network event
        System.out.println("[Votifier] Connection reset from " + remoteAddress + (voteAlreadyCompleted ? " (vote processed)" : " (vote incomplete)"));
    }
}