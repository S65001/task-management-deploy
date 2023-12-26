package com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication

public class TaskManagementServiceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementServiceBackendApplication.class, args);
    }

}
