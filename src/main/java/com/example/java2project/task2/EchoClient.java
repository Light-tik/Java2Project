package com.example.java2project.task2;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class EchoClient {

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 12345);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Подключено к серверу. Введите текст:");
            while (true) {
                String userInput = scanner.nextLine();
                out.println(userInput);
                String response = in.readLine();
                System.out.println("Ответ от сервера: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
