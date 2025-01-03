package com.vexsoftware.votifier.support.forwarding.redis;

/**
 * @author AkramL, azurejelly
 */
public class RedisCredentials {

    private final String host;
    private final String username;
    private final String password;
    private final String uri;
    private final String channel;
    private final int port;

    private RedisCredentials(String host, int port, String username, String password, String channel) {
        this.host = host;
        this.username = username;
        this.port = port;
        this.password = password;
        this.uri = null;
        this.channel = channel;
    }

    private RedisCredentials(String uri, String channel) {
        this.host = null;
        this.username = null;
        this.password = null;
        this.port = -1;
        this.uri = uri;
        this.channel = channel;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getChannel() {
        return channel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getURI() {
        return uri;
    }

    public static class Builder {

        private String host;
        private int port;
        private String username;
        private String password;
        private String uri;
        private String channel;

        private Builder() {}

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public RedisCredentials build() {
            if (channel == null || channel.isBlank()) {
                throw new IllegalArgumentException("channel cannot be null or empty");
            }

            if (uri != null && !uri.isBlank()) {
                return new RedisCredentials(uri, channel);
            }

            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("channel cannot be null or empty");
            }

            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("port must be within range");
            }

            return new RedisCredentials(host, port, username, password, channel);
        }
    }
}
