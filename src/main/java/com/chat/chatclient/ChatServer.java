/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.chat.chatclient;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private Map<String, PrintWriter> clientWriters = new HashMap<>(); // Nom du client -> PrintWriter
    private Set<String> connectedUsers = new HashSet<>(); // Utilisateurs connectés

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }

    // Démarrer le serveur
    public void start() {
        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Serveur démarré...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gestion des clients
   private class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // Mandefa ny fangatahana anaran'ny client
           writer.println("Quel est votre nom ?");
                clientName = reader.readLine();

                synchronized (connectedUsers) {
                    if (connectedUsers.contains(clientName)) {
                        writer.println("ERREUR: Nom d'utilisateur déjà pris.");
                        clientSocket.close();
                        return;
                    }
                    connectedUsers.add(clientName);
                    clientWriters.put(clientName, writer);
                    sendConnectedUsers();
                }


            // Mamakivaky sy mandefa hafatra amin'ny mpampiasa
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("MESSAGE:")) {
                    String[] parts = message.split(":");
                    String recipient = parts[1];  // Destinataire
                    String msgContent = parts[2]; // Contenu du message
                    sendPrivateMessage(recipient, msgContent);
                } else if (message.startsWith("FILE:")) {
                    String[] parts = message.split(":");
                    String recipient = parts[1]; // Destinataire
                    String fileName = parts[2];  // Nom du fichier
                    sendFileToClient(recipient, fileName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                synchronized (connectedUsers) {
                    connectedUsers.remove(clientName);  // Esorina ny client amin'ny lisitra
                    clientWriters.remove(clientName);  // Esorina amin'ny map
                    sendConnectedUsers();  // Manavao ny lisitry ny mpampiasa mifandray
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Mandefa ny lisitry ny mpampiasa mifandray amin'ny client rehetra
    private void sendConnectedUsers() {
    String usersList = String.join(",", connectedUsers);
    for (PrintWriter writer : clientWriters.values()) {
        writer.println("USERS_LIST:" + usersList);
    }
}


    // Mandefa hafatra manokana amin'ny mpandefa
    private void sendPrivateMessage(String recipient, String message) {
        PrintWriter recipientWriter = clientWriters.get(recipient);
        if (recipientWriter != null) {
            recipientWriter.println(clientName + ": " + message);
        } else {
            writer.println("Destinataire non trouvé.");
        }
    }
     
    // Mandefa rakitra amin'ny client
  private void sendFileToClient(String recipient, String fileName) {
    PrintWriter recipientWriter = clientWriters.get(recipient);
    if (recipientWriter != null) {
        recipientWriter.println("RECEIVE_FILE:" + fileName);
        try (InputStream fileInput = new FileInputStream(new File(fileName));
             OutputStream recipientOut = clientSocket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                recipientOut.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

private void notifyAllClients(String message) {
    for (PrintWriter writer : clientWriters.values()) {
        writer.println("NOTIFICATION:" + message);
    }
}

}

}
