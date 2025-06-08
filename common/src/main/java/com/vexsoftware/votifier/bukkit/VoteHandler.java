package com.vexsoftware.votifier.bukkit;

import com.vexsoftware.votifier.bukkit.model.Vote;
import com.vexsoftware.votifier.bukkit.net.VotifierSession;

public interface VoteHandler {

    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) throws Exception;

    default void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        throw new RuntimeException("Unimplemented onError handler");
    }
}
