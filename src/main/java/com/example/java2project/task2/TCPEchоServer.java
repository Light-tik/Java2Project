package com.example.java2project.task2;

import java.io.*;
import java.net.*;

public class TCPEchоServer {
    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Echo Server запущен на порту " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Подключился клиент: " + clientSocket.getInetAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Получено: " + inputLine);
                out.println(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Ошибка при работе с клиентом: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}
