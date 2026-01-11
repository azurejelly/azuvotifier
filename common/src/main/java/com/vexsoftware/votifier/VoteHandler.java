package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;

public interface VoteHandler {

    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress);

    default void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        throw new RuntimeException("Unimplemented VoteHandler#onError(...) handler");
    }
}
