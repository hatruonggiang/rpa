package com.example.demo.Repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.Entity.LogDevice;

public interface LogDeviceRepository extends MongoRepository<LogDevice, String> {
    List<LogDevice> findByCreatedTimeBetween(Instant start, Instant end);
}
