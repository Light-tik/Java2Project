package com.example.java2project.task4;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
public class Worker implements Runnable {

    private final Coordinator coordinator;
    @Getter
    private final int id;

    @Override
    public void run() {
        while (true) {
            Task task = coordinator.getTask();

            switch (task.type){
                case MAP -> handleMap(task);
                case REDUCE -> handleReduce(task);
                case NONE -> {
                    System.out.println("Worker " + id + " закончил работу");
                    return;
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
            System.out.println("Worker " + getId() + " завершил MAP задачу для файла: " + task.fileName);
        } catch (IOException e) {
            e.printStackTrace();
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
            System.out.println("Worker " + id + " завершил REDUCE задачу #" + task.reduceId);
        } catch (IOException e) {
            e.printStackTrace();
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
