package com.muc;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChatClient {
    private final String serverName;
    private final int serverPort;
    private OutputStream serverOut;
    private InputStream serverIn;
    private Socket socket;
    private BufferedReader bufferedIn;

    private ArrayList<UserStatusListener> userStatusListener = new ArrayList<>();
    private ArrayList<MessageListener> messageListener = new ArrayList<>();

    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 4444);
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("ONLINE: " + login);
            }

            @Override
            public void offline(String login) {
                System.out.println("OFFLINE " + login);
            }
        });

        client.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(String fromLogin, String content) {
                System.out.println(fromLogin + " " + content);
            }
        });

        if(!client.connect()){
            System.err.println("Connection failed.");
        }
        else{
            System.out.printf("Connection successful. ");
            if(client.login("guest", "guest")){
                System.out.println("Client successfully logged in.");
                client.send("admin", "hello world");
            }
            else{
                System.err.println("Client failed to log in.");
            }

            //client.logoff();
        }
    }

    public void send(String receiver, String content) throws IOException {
        String cmd = "send " + receiver + " " + content + "\n";
        serverOut.write(cmd.getBytes());
    }

    public boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());

        String response = bufferedIn.readLine();
        System.out.println("Response Line: " + response);

        if("valid login.".equalsIgnoreCase(response)){
            startMessageReader();
            return true;
        }
        else{
            return false;
        }
    }

    public void logoff() throws IOException{
        String cmd = "quit\n";
        serverOut.write(cmd.getBytes());

    }


    private void startMessageReader() {
        Thread t = new Thread(){
            @Override
            public void run(){
                readMessageLoop();
            }
        };
        t.start();
    }

    private void readMessageLoop() {
        String line;
        try{
            while((line = bufferedIn.readLine()) != null) {
                String[] tokens = line.split(" ", 3);
                if (tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if("online".equalsIgnoreCase(cmd)){
                        handleOnline(tokens);
                    }
                    else if("offline".equalsIgnoreCase(cmd)){
                        handleOffline(tokens);
                    }
                    else{
                        String[] tokensMsg = line.split(" ", 2);
                        handleMessage(tokensMsg);
                    }
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(String[] tokensMsg) {
        String login = tokensMsg[0];
        String content = tokensMsg[1];

        for(MessageListener listener: messageListener){
            listener.onMessage(login, content);
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener: userStatusListener){
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener: userStatusListener){
            listener.online(login);
        }
    }

    public boolean connect(){
        try{
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener){
        userStatusListener.add(listener);
    }

    public void removeUserStatusListener(UserStatusListener listener){
        userStatusListener.remove(listener);
    }

    public void addMessageListener(MessageListener listener){
        messageListener.add(listener);
    }

    public void removeMessageListener(MessageListener listener){
        messageListener.remove(listener);
    }
}
