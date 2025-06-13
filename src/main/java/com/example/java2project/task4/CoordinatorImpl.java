package com.example.java2project.task4;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class CoordinatorImpl implements Coordinator {

    private final Queue<String> tasksMap;
    private final int reduceTaskCount;
    private final int totalMapTasks;
    private int nextMapTaskId = 0;
    private int nextReduceTask = 0;
    private int completedMapTasks = 0;
    private int completedReduceTasks = 0;

    private final Map<Integer, List<String>> reduceMapTasks = new HashMap<>();

    private static final long TASK_TIMEOUT_MS = 5000;

    private final Map<String, Long> activeMapTasks = new HashMap<>();
    private final Map<Integer, Long> activeReduceTasks = new HashMap<>();

    private boolean isMapDone = false;
    private boolean isAllDone = false;

    public CoordinatorImpl(List<String> inputFiles, int reduceTaskCount) {
        this.reduceTaskCount = reduceTaskCount;
        this.totalMapTasks = inputFiles.size();
        this.tasksMap = new LinkedList<>(inputFiles);
        for (int i = 0; i < reduceTaskCount; i++) {
            reduceMapTasks.put(i, new ArrayList<>());
        }
    }

    public synchronized Task getTask() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<String, Long>> mapIterator = activeMapTasks.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, Long> entry = mapIterator.next();
            if (now - entry.getValue() > TASK_TIMEOUT_MS) {
                System.out.println("Map-задача зависла: " + entry.getKey() + " — возвращаем в очередь");
                tasksMap.add(entry.getKey());
                mapIterator.remove();
            }
        }
        if (!isMapDone) {
            if (!tasksMap.isEmpty()) {
                String fileName = tasksMap.poll();
                Task task = new Task(fileName, TaskType.MAP, reduceTaskCount);
                task.setMapTaskId(nextMapTaskId++);
                return task;
            } else if (completedMapTasks == totalMapTasks) {
                isMapDone = true;
            }
        }

        Iterator<Map.Entry<Integer, Long>> reduceIt = activeReduceTasks.entrySet().iterator();
        while (reduceIt.hasNext()) {
            Map.Entry<Integer, Long> entry = reduceIt.next();
            if (now - entry.getValue() > TASK_TIMEOUT_MS) {
                System.out.println("Reduce-задача зависла: " + entry.getKey() + " — повторная попытка");
                reduceIt.remove();
                nextReduceTask = Math.min(nextReduceTask, entry.getKey());             }
        }

        if (isMapDone && !isAllDone) {
            if (nextReduceTask < reduceTaskCount) {
                int reduceId = nextReduceTask++;
                List<String> filesForReduce = reduceMapTasks.get(reduceId);
                return new Task(TaskType.REDUCE, reduceId, filesForReduce);
            } else {
                isAllDone = true;
            }
        }

        return new Task(TaskType.NONE);
    }

    public synchronized void mapTaskDone(String mapFileName, Map<Integer, String> intermediateFiles) {
        completedMapTasks++;
        activeMapTasks.remove(mapFileName);
        for (Map.Entry<Integer, String> entry : intermediateFiles.entrySet()) {
            reduceMapTasks.get(entry.getKey()).add(entry.getValue());
        }
    }

    public synchronized void reduceTaskDone(int reduceId) {
        completedReduceTasks++;
        activeReduceTasks.remove(reduceId);
        if (completedReduceTasks == reduceTaskCount) {
            isAllDone = true;
        }
    }
}
