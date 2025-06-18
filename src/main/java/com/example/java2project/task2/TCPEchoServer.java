package com.example.java2project.task2;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TCPEchoServer {

    private final static int PORT = 12345;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        new TCPEchoServer().start();
    }

    public void start() {
        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("Echo Server running on port {}", PORT);
            closeServer(serverSocket);
            acceptClients(serverSocket, executorService);
        } catch (IOException e) {
            log.error("Connection error: {}", e.getMessage());
        } finally {
            executorService.shutdown();
            log.info("Server stopped");
        }
    }

    private void closeServer(ServerSocket serverSocket){
        new Thread(() -> {
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))){
                while (running){
                    String command = console.readLine();
                    if("exit".equalsIgnoreCase(command)){
                        running = false;
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            log.warn("Error closing server: {}", e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Error in server termination thread: {}", e.getMessage());
            }
        }, "ClosingServer").start();
    }

    private void acceptClients(ServerSocket serverSocket, ExecutorService threadPool) {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connected: {}", clientSocket.getInetAddress());
                threadPool.execute(() -> handleClient(clientSocket));
            } catch (SocketException e) {
                if (!running) break;
                log.warn("SocketException while accepting client: {}", e.getMessage());
            } catch (IOException e) {
                log.warn("Error connecting client: {}", e.getMessage());
            }
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
                log.info("Received: {}", inputLine);
                out.println(inputLine);
            }
            log.info("Client {} has disconnected", socket.getInetAddress());
            System.out.println("\nЧтобы завершить работу сервера введите 'exit'");
        } catch (IOException e) {
            log.warn("Error while working with client: {}", e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.warn("Error closing client socket: {}", e.getMessage());
            }
        }
    }
}
