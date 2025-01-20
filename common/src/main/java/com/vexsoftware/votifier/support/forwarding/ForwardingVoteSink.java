package com.vexsoftware.votifier.support.forwarding;

/**
 * Represents a method at which to receive forwarded votes.
 */
public interface ForwardingVoteSink {

    /**
     * Initialize the forwarding sink. Occurs on the onEnable method of a plugin.
     */
    void init();

    /**
     * Stop or close any outstanding network interfaces. Occurs on the onDisable method of a plugin.
     */
    void halt();
}
