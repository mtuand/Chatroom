package com.muc;


import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private String login = null;
    private Server server;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket){
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run(){
        try{
            handleClientSocket();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null){
            if("quit".equalsIgnoreCase((line))){
                handleLogoff();
                break;
            }
            String[] tokens = line.split(" ", 3);
            if(tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if("login".equalsIgnoreCase(cmd)){
                    handleLogin(outputStream, tokens);
                }
                else if("send".equalsIgnoreCase(cmd)){
                    handleMessage(tokens);
                }
                else if("join".equalsIgnoreCase(cmd)){
                    handleJoin(tokens);
                }
                else if("leave".equalsIgnoreCase(cmd)){
                    handleLeave(tokens);
                }
                else{
                    String message = "Unknown " + cmd + "\n";
                    outputStream.write(message.getBytes());

                }
            }
        }
        System.out.println("Closed");
        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if(tokens.length > 1){
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    public boolean isMember(String topic){
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if(tokens.length > 1){
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    // tokens: send + user + content
    // tokens: send + #topic + content
    private void handleMessage(String[] tokens) throws IOException {
        String receiver = tokens[1];
        String content = tokens[2];

        boolean isTopic = receiver.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList){
            if(isTopic){
                if(worker.isMember(receiver)){
                    String out = login + " to " + receiver + ": " + content + "\n";
                    worker.send(out);
                }
            }
            else{
                if(receiver.equalsIgnoreCase(worker.getLogin())){
                    String out = login + ": "  + content + "\n";
                    worker.send(out);
                }
            }

        }
    }

    private void handleLogoff() throws IOException{
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();
        // notify others of this online presence
        String presence = "Offline " + login + "\n";
        for(ServerWorker worker: workerList){
            if(!login.equals(worker.getLogin())){
                worker.send(presence);
            }
        }
        clientSocket.close();
    }

    public String getLogin(){
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException{
        if(tokens.length == 3){
            String login = tokens[1];
            String password = tokens[2];

            if((login.equals("guest") && password.equals("guest")) || login.equals("admin") && password.equals("admin")){
                String message = "Valid login.\n";
                outputStream.write(message.getBytes());
                this.login = login;
                System.out.println("User has logged in successfully.");

                List<ServerWorker> workerList = server.getWorkerList();

                // send current user info on others online
                for(ServerWorker worker: workerList){
                    if(worker.getLogin() != null){
                        if(!login.equals(worker.getLogin())){
                            String notification = "Online " + worker.getLogin() + "\n";
                            send(notification);
                        }
                    }
                }

                // notify others of this online presence
                String presence = "Online " + login + "\n";
                for(ServerWorker worker: workerList){
                    if(!login.equals(worker.getLogin())){
                        worker.send(presence);
                    }
                }
            }

            else{
                String message = "Error. Invalid login.\n";
                outputStream.write(message.getBytes());
                System.out.println("Failed login for " + login);
            }
        }
    }

    private void send(String notification) throws IOException {
        if (login != null) {
            outputStream.write(notification.getBytes());
        }
    }
}
