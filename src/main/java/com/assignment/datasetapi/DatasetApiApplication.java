package com.assignment.datasetapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatasetApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasetApiApplication.class, args);
        System.out.println("""
                
                Dataset API started successfully!
                Swagger UI:   http://localhost:8080/swagger-ui.html
                H2 Console:   http://localhost:8080/h2-console
                API Base URL: http://localhost:8080/api/dataset
                """);
    }
}
