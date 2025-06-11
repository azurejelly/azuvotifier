package com.vexsoftware.votifier.standalone.config.redis;

public class RedisVotifierConfiguration {

    private final boolean enabled;
    private final String address;
    private final int port;
    private final String username;
    private final String password;
    private final String uri;
    private final String channel;

    public RedisVotifierConfiguration(
            boolean enabled, String address, int port,
            String username, String password, String uri,
            String channel
    ) {
        this.enabled = enabled;
        this.address = address;
        this.port = port;
        this.password = password;
        this.channel = channel;
        this.username = username;
        this.uri = uri;
    }

    public RedisVotifierConfiguration() {
        this.enabled = false;
        this.address = "127.0.0.1";
        this.port = 6379;
        this.username = "";
        this.password = "";
        this.uri = "";
        this.channel = "nuvotifier:votes";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public String getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }

    public String getUri() {
        return uri;
    }
}
