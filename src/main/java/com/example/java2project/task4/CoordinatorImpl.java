package com.example.java2project.task4;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Getter
@Setter
public class CoordinatorImpl implements Coordinator {

    private final Queue<String> tasksMap;
    private final Queue<Integer> retryTasks = new LinkedList<>();
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
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("The input file list must not be empty.");
        }
        if (reduceTaskCount <= 0) {
            throw new IllegalArgumentException("The number of reduce tasks must be greater than 0");
        }
        this.reduceTaskCount = reduceTaskCount;
        this.totalMapTasks = inputFiles.size();
        this.tasksMap = new LinkedList<>(inputFiles);
        for (int i = 0; i < reduceTaskCount; i++) {
            reduceMapTasks.put(i, new ArrayList<>());
        }
    }

    public synchronized Task getTask() {
        cleanMapTasks();
        if (!isMapDone) {
            if (!tasksMap.isEmpty()) {
              return createMapTask();
            } else if (completedMapTasks == totalMapTasks) {
                isMapDone = true;
            }
        }
        cleanReduceTasks();
        if (!retryTasks.isEmpty()) {
            return cleanRetryReduceTasks();
        }
        if (isMapDone && !isAllDone) {
            if (nextReduceTask < reduceTaskCount) {
                return createReduceTask();
            } else {
                isAllDone = true;
            }
        }
        return Task.builder()
                .type(TaskType.NONE)
                .build();
    }

    private Task createMapTask(){
        String fileName = tasksMap.poll();
        activeMapTasks.put(fileName, System.currentTimeMillis());
        return Task.builder()
                .fileName(fileName)
                .type(TaskType.MAP)
                .reduceTaskCount(reduceTaskCount)
                .mapTaskId(nextMapTaskId++)
                .build();
    }

    private Task createReduceTask(){
        int reduceId = nextReduceTask++;
        activeReduceTasks.put(reduceId, System.currentTimeMillis());
        List<String> filesForReduce = reduceMapTasks.get(reduceId);
        return Task.builder()
                .type(TaskType.REDUCE)
                .reduceId(reduceId)
                .filesForReduce(filesForReduce)
                .build();
    }

    private void cleanMapTasks(){
        Iterator<Map.Entry<String, Long>> mapIterator = activeMapTasks.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, Long> entry = mapIterator.next();
            long now = System.currentTimeMillis();
            if (now - entry.getValue() > TASK_TIMEOUT_MS) {
                log.warn("Map task stuck: {} - returning to queue", entry.getKey());
                tasksMap.add(entry.getKey());
                mapIterator.remove();
            }
        }
    }

    private void cleanReduceTasks(){
        Iterator<Map.Entry<Integer, Long>> reduceIt = activeReduceTasks.entrySet().iterator();
        while (reduceIt.hasNext()) {
            Map.Entry<Integer, Long> entry = reduceIt.next();
            long now = System.currentTimeMillis();
            if (now - entry.getValue() > TASK_TIMEOUT_MS) {
                log.warn("Reduce task stuck: {} - retrying", entry.getKey());
                retryTasks.add(entry.getKey());
                reduceIt.remove();
                nextReduceTask = Math.min(nextReduceTask, entry.getKey());             }
        }
    }

    private Task cleanRetryReduceTasks(){
        Integer reduceId = retryTasks.poll();
        if (reduceId == null) {
            log.warn("Attempting to take a task for re-reduce, but the queue is empty");
            return Task.builder().type(TaskType.NONE).build();
        }
        activeReduceTasks.put(reduceId, System.currentTimeMillis());
        return Task.builder()
                .type(TaskType.REDUCE)
                .reduceId(reduceId)
                .filesForReduce(reduceMapTasks.get(reduceId))
                .build();
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

    @Override
    public synchronized void taskFailed(Task task) {
        if (task.getType() == TaskType.MAP) {
            log.warn("Map task failed № {}", task.getFileName());
            tasksMap.add(task.getFileName());
            activeMapTasks.remove(task.getFileName());
        } else if (task.getType() == TaskType.REDUCE) {
            log.warn("Reduce task failed № {}", task.getReduceId());
            retryTasks.add(task.getReduceId());
            activeReduceTasks.remove(task.getReduceId());
        }
    }

    @Override
    public synchronized boolean isFinished() {
        return isAllDone;
    }
}
