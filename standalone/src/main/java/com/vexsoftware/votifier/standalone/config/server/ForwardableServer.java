package com.vexsoftware.votifier.standalone.config.server;

public class ForwardableServer {

    private final String address;
    private final int port;
    private final String token;

    public ForwardableServer() {
        this.address = "127.0.0.1";
        this.port = 25565;
        this.token = "exampleToken";
    }

    public ForwardableServer(String address, int port, String token) {
        this.address = address;
        this.port = port;
        this.token = token;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }
}
