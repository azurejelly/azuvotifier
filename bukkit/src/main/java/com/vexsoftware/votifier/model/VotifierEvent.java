package com.vexsoftware.votifier.model;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@code VotifierEvent} is a custom Bukkit event class that is sent
 * synchronously to CraftBukkit's main thread allowing other plugins to listen
 * for votes.
 * <p>
 * It will only be sent asynchronously when running under Folia.
 *
 * @author frelling
 */
public class VotifierEvent extends Event {

    /**
     * Event listener handler list.
     */
    private static final HandlerList handlers = new HandlerList();

    /**
     * Encapsulated vote record.
     */
    private final Vote vote;

    /**
     * Constructs a vote event that encapsulated the given vote record.
     *
     * @param vote vote record
     */
    public VotifierEvent(final Vote vote) {
        this.vote = vote;
    }

    /**
     * Constructs a vote event that encapsulated the given vote record.
     *
     * @param vote vote record
     * @param async whether the event will be fired asynchronously
     */
    public VotifierEvent(final Vote vote, final boolean async) {
        super(async);
        this.vote = vote;
    }

    /**
     * Return the encapsulated vote record.
     *
     * @return vote record
     */
    public Vote getVote() {
        return vote;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
