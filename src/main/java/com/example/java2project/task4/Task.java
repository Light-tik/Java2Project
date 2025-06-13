package com.example.java2project.task4;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Task {
    public TaskType type;
    public String fileName;
    public int mapTaskId;
    public int reduceId;
    public int reduceTaskCount;
    public List<String> filesForReduce;

    public Task(String fileName, TaskType type, int reduceTaskCount) {
        this.fileName = fileName;
        this.type = type;
        this.reduceTaskCount = reduceTaskCount;
    }

    public Task(TaskType type, int reduceId, List<String> filesForReduce) {
        this.type = type;
        this.reduceId = reduceId;
        this.filesForReduce = filesForReduce;
    }

    public Task(TaskType type) {
        this.type = type;
    }

    public Task(String fileName, int mapTaskId, int reduceId) {
        this.fileName = fileName;
        this.mapTaskId = mapTaskId;
        this.reduceId = reduceId;
    }
}
