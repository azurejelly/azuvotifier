package com.vexsoftware.votifier.fabric.event;

import com.vexsoftware.votifier.model.Vote;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface VoteListener {

    Event<VoteListener> EVENT = EventFactory.createArrayBacked(VoteListener.class, (listeners) -> (vote) -> {
        for (VoteListener listener : listeners) {
            listener.onVote(vote);
        }
    });

    void onVote(Vote vote);
}
