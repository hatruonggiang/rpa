package com.example.demo.Controller;

import org.springframework.web.bind.annotation.*;

import com.example.demo.Entity.LogDevice;
import com.example.demo.Entity.LogDeviceStats;
import com.example.demo.Service.LogDeviceService;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/logdevices")
public class LogDeviceController {

    private final LogDeviceService service;

    public LogDeviceController(LogDeviceService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public List<LogDevice> getAll(@RequestParam String date) {
        return service.getByDate(date);
    }

    
    @GetMapping("/stat")
    public List<LogDeviceStats> getLogDeviceStats(@RequestParam String date) {
        return service.getLogDeviceStats(date);
    }

    // Endpoint for log analysis - phân tích dữ liệu theo ngày
    @GetMapping("/stat2")
    public Map<String, Map<String, Integer>> getLogDeviceStats2(@RequestParam String date) {
        return service.getLogDeviceStats2(date);
    }

    // Export raw data to Excel
    @GetMapping("/export/data")
    public ResponseEntity<byte[]> exportDataToExcel(@RequestParam String date) throws IOException {
        List<LogDevice> data = service.getByDate(date);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.exportDataToExcel(data, outputStream, date);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "logdevice_data_" + date + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    // Export statistics analysis to Excel  
    @GetMapping("/export/stat1")
    public ResponseEntity<byte[]> exportStat1ToExcel(@RequestParam String date) throws IOException {
        List<LogDeviceStats> stats = service.getLogDeviceStats(date);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.exportStat1ToExcel(stats, outputStream, date);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "logdevice_stat1_" + date + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    // Export analysis 2 to Excel
    @GetMapping("/export/stat2") 
    public ResponseEntity<byte[]> exportStat2ToExcel(@RequestParam String date) throws IOException {
        Map<String, Map<String, Integer>> analysis = service.getLogDeviceStats2(date);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.exportStat2ToExcel(analysis, outputStream, date);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "logdevice_stat2_" + date + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }
}