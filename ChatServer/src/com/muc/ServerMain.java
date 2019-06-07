package com.muc;

public class ServerMain {
    public static void main(String[] args) {
        int port = 4444;
        Server server = new Server(port);
        server.start();
    }


}
