package com.vexsoftware.votifier.bukkit.sponge.event;

import com.vexsoftware.votifier.bukkit.model.Vote;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * This event is posted whenever a vote is received and processed by NuVotifier. Note that NuVotifier posts this event
 * synchronously.
 */
public class VotifierEvent extends AbstractEvent {

    private final Vote vote;
    private final Cause cause;
    private final Object source;
    private final EventContext context;

    public VotifierEvent(Vote vote, Cause cause, Object source, EventContext context) {
        this.vote = vote;
        this.cause = cause;
        this.source = source;
        this.context = context;
    }

    public Vote vote() {
        return vote;
    }

    @Override
    public Cause cause() {
        return cause;
    }

    @Override
    public Object source() {
        return source;
    }

    @Override
    public EventContext context() {
        return context;
    }
}
