package com.muc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LoginPane extends JFrame {
    private final ChatClient chatClient;
    JTextField loginField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JButton  loginButton = new JButton("Login");

    public LoginPane(){
        super("Login");
        this.chatClient = new ChatClient("localhost",4444);
        chatClient.connect();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(loginField);
        p.add(passwordField);
        p.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        getContentPane().add(p, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }

    private void handleLogin() {
        String login = loginField.getText();
        String password = passwordField.getText();

        try{
            if(chatClient.login(login, password)){
                // show user list window
                UserListPane userListPane = new UserListPane(chatClient);
                JFrame frame = new JFrame("User List");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(400, 600);

                frame.getContentPane().add(userListPane, BorderLayout.CENTER);
                frame.setVisible(true);

                setVisible(false);
            }
            else{
                //error
                JOptionPane.showMessageDialog(this, "Invalid login/password");
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LoginPane loginPane = new LoginPane();
        loginPane.setVisible(true);
    }
}
