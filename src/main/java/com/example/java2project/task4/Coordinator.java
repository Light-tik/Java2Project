package com.example.java2project.task4;

import java.util.Map;

public interface Coordinator {

    Task getTask();

    void mapTaskDone(String mapFileName, Map<Integer, String> intermediateFiles);

    void reduceTaskDone(int reduceId);
}
