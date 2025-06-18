package com.example.java2project.task4;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<String> fileNames = List.of("input1.txt", "input2.txt", "input3.txt");
        List<String> contents = List.of(
                "sea ocean water shells",
                "sea sand sun water",
                "ship dolphin sail coconuts");

        for (int i = 0; i < fileNames.size(); i++) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileNames.get(i)))) {
                writer.write(contents.get(i));
            } catch (IOException e) {
                log.error(e.getMessage());
                return;
            }
        }
        int numReduceTasks = 3;
        CoordinatorImpl coordinator = new CoordinatorImpl(fileNames, numReduceTasks);
        int numWorkers = 4;
        Thread[] workers = new Thread[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Thread(new Worker(coordinator, i));
            workers[i].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        System.out.println("\n Итоговые файлы");
        for (int i = 0; i < numReduceTasks; i++) {
            Path resultFile = Paths.get("result-" + i + ".txt");
            System.out.println("Результат из result-" + i + ".txt:");
            try {
                Files.readAllLines(resultFile).forEach(System.out::println);
            } catch (IOException e) {
                log.warn("Failed to read {}", resultFile);
            }
            System.out.println();
        }
    }
}
