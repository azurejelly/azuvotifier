package com.vexsoftware.votifier.standalone.config.server;

public class BackendServer {

    private final String address;
    private final int port;
    private final String token;

    public BackendServer() {
        this.address = "127.0.0.1";
        this.port = 25565;
        this.token = "exampleToken";
    }

    public BackendServer(String address, int port, String token) {
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
