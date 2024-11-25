/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.chat.chatclient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatAppSwing {

    private JFrame frame;
    private JTextArea messageArea;
    private JTextField messageField;
    private JButton sendButton, fileButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel notificationLabel;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatAppSwing().createAndShowGUI());
    }

    // Création et affichage de l'interface graphique
    public void createAndShowGUI() {
        frame = new JFrame("Chat Privé");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        messageArea = new JTextArea(20, 40);
        messageArea.setEditable(false);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        messageField = new JTextField(30);
        sendButton = new JButton("Envoyer");
        sendButton.addActionListener(e -> sendMessage());

        fileButton = new JButton("Envoyer un fichier");
        fileButton.addActionListener(e -> sendFile());

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScroll = new JScrollPane(userList);

        notificationLabel = new JLabel();

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.add(messageField);
        inputPanel.add(sendButton);
        inputPanel.add(fileButton);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.add(messageScroll, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(notificationLabel, BorderLayout.NORTH);

        frame.getContentPane().setLayout(new BorderLayout(10, 10));
        frame.getContentPane().add(userListScroll, BorderLayout.WEST);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null); // Centre la fenêtre
        frame.setVisible(true);

        try {
            socket = new Socket("localhost", 12345);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Récupération du nom d'utilisateur
            String name = JOptionPane.showInputDialog("Entrez votre nom :");
            writer.println(name);

            // Lecture des messages du serveur (liste des utilisateurs connectés, messages privés)
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = reader.readLine()) != null) {
                        if (serverMessage.startsWith("USERS_LIST:")) {
                            updateUserList(serverMessage.substring(11)); // Mettre à jour la liste des utilisateurs
                        } else {
                            messageArea.append(serverMessage + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Impossible de se connecter au serveur.");
        }
    }

    // Mettre à jour la liste des utilisateurs connectés
  // Mettre à jour la liste des utilisateurs connectés
private void updateUserList(String users) {
    String[] usersArray = users.split(",");
    userListModel.clear();
    for (String user : usersArray) {
        userListModel.addElement(user);
    }
    // Afficher une notification que la liste des utilisateurs a été mise à jour
    showNotification("Liste des utilisateurs mise à jour");
}

    // Envoie d'un message privé
   private void sendMessage() {
    String message = messageField.getText().trim();
    String selectedUser = userList.getSelectedValue();
    if (selectedUser == null) {
        showNotification("Veuillez sélectionner un destinataire.");
        return;
    }
    if (!message.isEmpty()) {
        writer.println("MESSAGE:" + selectedUser + ":" + message);
        messageArea.append("Vous -> " + selectedUser + ": " + message + "\n");
        messageField.setText("");
    }
}
  private void saveReceivedFile(String fileName) {
    try (FileOutputStream fileOut = new FileOutputStream(new File(fileName))) {
        char[] buffer = new char[4096];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
            fileOut.write(new String(buffer, 0, bytesRead).getBytes());
        }
        showNotification("Fichier reçu: " + fileName);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    // Envoie d'un fichier
    private void sendFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("Tous les fichiers", "*.*"));
    int result = fileChooser.showOpenDialog(frame);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file))) {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null) {
                writer.println("FILE:" + selectedUser + ":" + file.getName());
                byte[] buffer = new byte[4096];
                int bytesRead;
                OutputStream socketOut = socket.getOutputStream();
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    socketOut.write(buffer, 0, bytesRead);
                }
                messageArea.append("Vous -> " + selectedUser + ": Fichier " + file.getName() + " envoyé\n");
                showNotification("Fichier envoyé à " + selectedUser);
            } else {
                showNotification("Veuillez sélectionner un destinataire.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


    // Afficher une notification dans l'interface
    private void showNotification(String message) {
        notificationLabel.setText(message);
        Timer timer = new Timer(3000, e -> notificationLabel.setText(""));
        timer.setRepeats(false);
        timer.start();
    }
}
