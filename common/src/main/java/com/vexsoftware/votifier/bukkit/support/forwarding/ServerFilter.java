package com.vexsoftware.votifier.bukkit.support.forwarding;

import java.util.Collection;
import java.util.Collections;

public class ServerFilter {

    private final Collection<String> names;
    private final boolean whitelist;

    public ServerFilter(Collection<String> names, boolean whitelist) {
        this.names = names;
        this.whitelist = whitelist;
    }

    public ServerFilter() {
        this(Collections.emptySet(), true);
    }

    public boolean isAllowed(String name) {
        return names.contains(name) == whitelist;
    }
}
