package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // // Test log analysis on startup
        // String testDate = "25062024"; // Example date in ddMMyyyy format
        // List<LogDeviceStats> stats = service.getLogDeviceStats(testDate);
        // System.out.println("Log Device Stats for " + testDate + ":");
        // for (LogDeviceStats stat : stats) {
        //     System.out.println(stat);
        // }
    }


}
