package com.vexsoftware.votifier.bukkit.support.forwarding;

import com.vexsoftware.votifier.bukkit.model.Vote;

/**
 * Represents a source for forwarding votes through to servers.
 */
public interface ForwardingVoteSource {

    /**
     * Initializes the forwarding vote source. Occurs on the onEnable method of a plugin.
     */
    void init();

    /**
     * Forwards a vote to all servers set up to receive votes.
     *
     * @param v Vote to forward to servers
     */
    void forward(Vote v);

    /**
     * Stop or close any outstanding network interfaces. Occurs on the onDisable method of a plugin.
     */
    void halt();
}
