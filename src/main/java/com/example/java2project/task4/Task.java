package com.example.java2project.task4;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class Task {
    public TaskType type;
    public String fileName;
    public int mapTaskId;
    public int reduceId;
    public int reduceTaskCount;
    public List<String> filesForReduce;

    public int getId() {
        return type == TaskType.MAP ? mapTaskId : reduceId;
    }
}
