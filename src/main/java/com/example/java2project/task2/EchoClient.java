package com.example.java2project.task2;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;

@Slf4j
public class EchoClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in))
        ) {
            log.info("Connected to server"  + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Введите текст (или 'exit' для выхода):");
            String userInput;
            while ((userInput = console.readLine()) != null) {
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
                out.println(userInput);
                String response = in.readLine();
                log.info("Server response: {}", response);
            }
        } catch (IOException e) {
            log.error("Error while working with server: {}", e.getMessage());
        }
    }
}
