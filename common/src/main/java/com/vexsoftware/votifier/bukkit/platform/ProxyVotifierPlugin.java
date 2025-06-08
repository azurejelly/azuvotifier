package com.vexsoftware.votifier.bukkit.platform;

import java.util.Collection;
import java.util.Optional;

public interface ProxyVotifierPlugin extends VotifierPlugin {

    Collection<BackendServer> getAllBackendServers();

    Optional<BackendServer> getServer(String name);
}
