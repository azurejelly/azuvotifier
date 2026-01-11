package com.vexsoftware.votifier.platform.plugin.proxy;

import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.plugin.VotifierPlugin;

import java.util.Collection;
import java.util.Optional;

public interface ProxyVotifierPlugin extends VotifierPlugin {

    Collection<BackendServer> getAllBackendServers();

    Optional<BackendServer> getServer(String name);
}
