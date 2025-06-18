package com.example.java2project.task4;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class Worker implements Runnable {

    private final Coordinator coordinator;
    @Getter
    private final int id;

    private static final int MAX_RETRIES = 3;

    @Override
    public void run() {
        while (true) {
            Task task = coordinator.getTask();

            switch (task.type) {
                case MAP -> runWithRetries(() -> handleMap(task), task);
                case REDUCE -> runWithRetries(() -> handleReduce(task), task);
                case NONE -> {
                    if (coordinator.isFinished()) {
                        log.info("Worker {} has finished work", id);
                        return;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            log.warn(e.getMessage());
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void runWithRetries(Runnable runnable, Task task){
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try {
                log.info("Worker {} performs {} task : {}", id, task.getType(), task.getId());
                runnable.run();
                return;
            } catch (Exception e) {
                log.warn("Error executing {} task {} on attempt {}: {}", task.getType(), task.getId(), i, e.getMessage());
                if (i == MAX_RETRIES) {
                    log.error("Worker {} failed to complete {} task after {} attempts: {}", id, task.getType(), MAX_RETRIES, task.getId());
                    coordinator.taskFailed(task);
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private void handleMap(Task task) {
        try {
            String content = Files.readString(Path.of(task.getFileName()));
            List<KeyValue> kvs = map(task.getFileName(), content);
            Map<Integer, List<KeyValue>> buckets = new HashMap<>();

            List<KeyValue> sorted = new ArrayList<>(kvs);
            sorted.sort(Comparator.comparing(KeyValue::getKey));

            for (KeyValue kv : sorted) {
                int bucket = Math.abs(kv.getKey().hashCode()) % task.getReduceTaskCount();
                buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(kv);
            }
            Map<Integer, String> filesCreated = new HashMap<>();
            for (Map.Entry<Integer, List<KeyValue>> entry : buckets.entrySet()) {
                int rediceId = entry.getKey();
                String fileName = String.format("mr-%d-%d.txt", task.getMapTaskId() , rediceId);
                filesCreated.put(rediceId, fileName);
                try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName))){
                    for (KeyValue kv : entry.getValue()) {
                        writer.write(kv.getKey() + "\t" + kv.getValue() + "\n");
                    }
                }
            }
            coordinator.mapTaskDone(task.getFileName(), filesCreated);
            log.info("Worker {} completed MAP task for file: {}", getId(), task.fileName);
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private void handleReduce(Task task) {
        try {
            Map<String, List<String>> grouped = new HashMap<>();

            for (String fileName : task.getFilesForReduce()) {
                List<String> lines = Files.readAllLines(Path.of(fileName));
                for (String line : lines) {
                    String[] parts = line.split("\t");
                    if (parts.length == 2) {
                        grouped.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
                    }
                }
            }
            List<String> sortedKeys = new ArrayList<>(grouped.keySet());
            Collections.sort(sortedKeys);

            String outputFile = "result-" + task.getReduceId() + ".txt";
            try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFile))) {
                for (String key : sortedKeys) {
                    String result = reduce(key, grouped.get(key));
                    writer.write(key + " " + result + "\n");
                }
            }
            coordinator.reduceTaskDone(task.getReduceId());
            log.info("Worker {} completed REDUCE task № {}", id, task.reduceId);
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    public List<KeyValue> map(String fileName, String content) {
        String [] words= content.trim().split("\\W+");
        List<KeyValue> result = new ArrayList<>();
        for (String word : words) {
            result.add(new KeyValue(word.toLowerCase(), "1"));
        }
        return result;
    }

    public String reduce(String key, List<String> values) {
        return String.valueOf(values.size());
    }
}
