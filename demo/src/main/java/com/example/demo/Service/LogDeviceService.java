package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.example.demo.Entity.LogDevice;
import com.example.demo.Entity.LogDeviceStats;
import com.example.demo.Repository.LogDeviceRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service xử lý logic nghiệp vụ cho LogDevice
 */
@Service
public class LogDeviceService {
    
    private final LogDeviceRepository repo;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    // Danh sách các key cần phân tích từ log
    private static final List<String> KEYS = Arrays.asList(
        "on_relay1:false", "on_relay2:false", "on_relay3:false", "on_relay4:false",
        "off_relay1:false", "off_relay2:false", "off_relay3:false", "off_relay4:false",
        "on_all:false", "off_all:false", "remote_learn:false", "remote_control:false",
        "open:false", "close:false", "stop:false",
        "remote_control_close:false", "remote_control_open:false", "remote_control_stop:false"
    );

    public LogDeviceService(LogDeviceRepository repo) {
        this.repo = repo;
    }

    /**
     * Lấy danh sách LogDevice theo ngày
     * 
     * @param dateString ngày theo định dạng ddMMyyyy
     * @return danh sách LogDevice
     */
    public List<LogDevice> getByDate(String dateString) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        LocalDate localDate = LocalDate.parse(dateString, inputFormatter);

        Instant start = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return repo.findByCreatedTimeBetween(start, end);
    }

    /**
     * Thống kê LogDevice theo deviceCode và macHc với statusTestCase = 1,2
     * 
     * @param dateString ngày theo định dạng ddMMyyyy
     * @return danh sách thống kê
     */
    public List<LogDeviceStats> getLogDeviceStats(String dateString) {
        // Parse ngày từ format ddMMyyyy
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        LocalDate localDate = LocalDate.parse(dateString, inputFormatter);

        // Tạo khoảng thời gian từ đầu ngày đến đầu ngày hôm sau
        Instant start = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Aggregation agg = newAggregation(
                // Lọc theo ngày và statusTestCase = 1 hoặc 2
                match(Criteria.where("statusTestCase").in(1, 2)
                        .and("createdTime").gte(start).lt(end)),
                
                // Nhóm theo deviceCode và macHc
                group("deviceCode", "macHc")
                        .sum(ConditionalOperators.when(Criteria.where("statusTestCase").is(1)).then(1).otherwise(0))
                        .as("countStatus1")
                        .sum(ConditionalOperators.when(Criteria.where("statusTestCase").is(2)).then(1).otherwise(0))
                        .as("countStatus2")
                        .count().as("countTotal"),
                
                // Định dạng lại kết quả
                project("countStatus1", "countStatus2", "countTotal")
                        .and("_id.deviceCode").as("deviceCode")
                        .and("_id.macHc").as("macHc")
        );

        AggregationResults<LogDeviceStats> results = mongoTemplate.aggregate(agg, "tbl_log_device", LogDeviceStats.class);
        return results.getMappedResults();
    }

    /**
     * Thống kê LogDevice theo phân tích log chi tiết
     * Phương pháp 2: Phân tích các key cụ thể trong log
     * 
     * @param dateString ngày theo định dạng ddMMyyyy
     * @return Map chứa thống kê theo macHc và các key
     */
    public Map<String, Map<String, Integer>> getLogDeviceStats2(String dateString) {
        // Bước 1: Lấy dữ liệu của ngày được chọn
        List<LogDevice> devicesOfDay = getByDate(dateString);
        
        // Bước 2: Lấy danh sách macHc unique từ dữ liệu
        List<String> uniqueMacList = devicesOfDay.stream()
                .map(LogDevice::getMacHc)
                .filter(mac -> mac != null && !mac.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        
        // Bước 3: Đưa dữ liệu vào hàm phân tích
        return analyzeLogData(devicesOfDay, uniqueMacList);
    }

    /**
     * Hàm phân tích chính - tương đương COUNTIFS trong Excel
     * 
     * @param devices danh sách LogDevice cần phân tích
     * @param macList danh sách macHc unique
     * @return Map kết quả phân tích
     */
    private Map<String, Map<String, Integer>> analyzeLogData(List<LogDevice> devices, List<String> macList) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        
        // Khởi tạo map cho từng macHc (hàng)
        for (String mac : macList) {
            Map<String, Integer> rowData = new LinkedHashMap<>();
            
            // Khởi tạo các cột (KEYS) với giá trị 0
            for (String key : KEYS) {
                rowData.put(key, 0);
            }
            
            result.put(mac, rowData);
        }
        
        // Thực hiện đếm - tương đương COUNTIFS(macHc_column=mac, log_column contains key)
        for (String mac : macList) {
            Map<String, Integer> rowData = result.get(mac);
            
            for (String key : KEYS) {
                int count = countMatchingRecords(devices, mac, key);
                rowData.put(key, count);
            }
        }
        
        return result;
    }

    /**
     * Hàm đếm tương đương COUNTIFS trong Excel
     * Excel: =COUNTIFS($E:$E,$S18,$K:$K,"*off_all:false*")
     * Java tương đương:
     * 
     * @param devices danh sách thiết bị
     * @param targetMac MAC địa chỉ cần tìm
     * @param targetKey key cần tìm trong log
     * @return số lượng bản ghi phù hợp
     */
    private int countMatchingRecords(List<LogDevice> devices, String targetMac, String targetKey) {
        return (int) devices.stream()
                .filter(device -> targetMac.equals(device.getMacHc()))          // $E:$E=$S18
                .filter(device -> {                                             
                    String log = device.getLog();
                    return log != null && log.toLowerCase().contains(targetKey.toLowerCase()); // $K:$K="*key*"
                })
                .count();
    }

    /**
     * Xuất dữ liệu LogDevice ra Excel
     * 
     * @param data danh sách LogDevice
     * @param outputStream stream đầu ra
     * @param date ngày xuất
     * @throws IOException lỗi xuất file
     */
    public void exportDataToExcel(List<LogDevice> data, ByteArrayOutputStream outputStream, String date) 
            throws IOException {
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dữ liệu LogDevice - " + date);
        
        // Tạo style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // Tạo hàng header
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Serial", "Mã thiết bị", "Loại thiết bị", "MAC HC", "Phiên bản FW", 
            "Trạng thái test case", "Loại", "Lệnh", "RQI", "Log", "Thời gian tạo", "Thời gian cập nhật", "Class"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Điền dữ liệu
        for (int i = 0; i < data.size(); i++) {
            LogDevice device = data.get(i);
            Row row = sheet.createRow(i + 1);
            
            row.createCell(0).setCellValue(device.get_id() != null ? device.get_id() : "");
            row.createCell(1).setCellValue(device.getSerial() != null ? device.getSerial() : "");
            row.createCell(2).setCellValue(device.getDeviceCode() != null ? device.getDeviceCode() : "");
            row.createCell(3).setCellValue(device.getDeviceType() != null ? device.getDeviceType() : "");
            row.createCell(4).setCellValue(device.getMacHc() != null ? device.getMacHc() : "");
            row.createCell(5).setCellValue(device.getFwVersion() != null ? device.getFwVersion() : "");
            row.createCell(6).setCellValue(device.getStatusTestCase() != 0 ? 
                String.valueOf(device.getStatusTestCase()) : "");
            row.createCell(7).setCellValue(device.getType() != 0 ? 
                String.valueOf(device.getType()) : "");
            row.createCell(8).setCellValue(device.getCmd() != null ? device.getCmd() : "");
            row.createCell(9).setCellValue(device.getRqi() != null ? device.getRqi() : "");
            row.createCell(10).setCellValue(device.getLog() != null ? device.getLog() : "");
            row.createCell(11).setCellValue(device.getCreatedTime() != null ? 
                device.getCreatedTime().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            row.createCell(12).setCellValue(device.getUpdatedTime() != null ? 
                device.getUpdatedTime().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            row.createCell(13).setCellValue(device.getClass().getSimpleName());
        }
        
        // Tự động điều chỉnh độ rộng cột
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        workbook.write(outputStream);
        workbook.close();
    }

    /**
     * Xuất thống kê phân tích 1 ra Excel
     * 
     * @param stats danh sách thống kê
     * @param outputStream stream đầu ra
     * @param date ngày xuất
     * @throws IOException lỗi xuất file
     */
    public void exportStat1ToExcel(List<LogDeviceStats> stats, ByteArrayOutputStream outputStream, String date) 
            throws IOException {
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Phân tích thống kê - " + date);
        
        // Tạo style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // Lấy danh sách deviceCode và macAddress unique
        List<String> deviceCodes = stats.stream()
            .map(LogDeviceStats::getDeviceCode)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
            
        List<String> macAddresses = stats.stream()
            .map(LogDeviceStats::getMacHc)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        // Tạo các hàng header
        Row headerRow1 = sheet.createRow(0);
        Row headerRow2 = sheet.createRow(1);
        
        headerRow1.createCell(0).setCellValue("Địa chỉ MAC");
        headerRow2.createCell(0).setCellValue("");
        
        int colIndex = 1;
        for (String deviceCode : deviceCodes) {
            Cell cell = headerRow1.createCell(colIndex);
            cell.setCellValue(deviceCode);
            cell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, colIndex, colIndex + 2));
            
            headerRow2.createCell(colIndex).setCellValue("Trạng thái 1");
            headerRow2.createCell(colIndex + 1).setCellValue("Trạng thái 2");
            headerRow2.createCell(colIndex + 2).setCellValue("Tổng");
            
            colIndex += 3;
        }
        
        // Tạo ma trận để tra cứu dữ liệu
        Map<String, Map<String, LogDeviceStats>> matrix = new HashMap<>();
        for (LogDeviceStats stat : stats) {
            matrix.computeIfAbsent(stat.getMacHc(), k -> new HashMap<>())
                .put(stat.getDeviceCode(), stat);
        }
        
        // Điền dữ liệu
        int rowIndex = 2;
        for (String mac : macAddresses) {
            Row row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(mac);
            
            colIndex = 1;
            for (String deviceCode : deviceCodes) {
                LogDeviceStats stat = matrix.getOrDefault(mac, new HashMap<>()).get(deviceCode);
                if (stat != null) {
                    row.createCell(colIndex).setCellValue(stat.getCountStatus1());
                    row.createCell(colIndex + 1).setCellValue(stat.getCountStatus2());
                    row.createCell(colIndex + 2).setCellValue(stat.getCountTotal());
                } else {
                    row.createCell(colIndex).setCellValue(0);
                    row.createCell(colIndex + 1).setCellValue(0);
                    row.createCell(colIndex + 2).setCellValue(0);
                }
                colIndex += 3;
            }
            rowIndex++;
        }
        
        // Hàng tổng kết
        Row totalRow = sheet.createRow(rowIndex);
        totalRow.createCell(0).setCellValue("TỔNG CỘNG");
        
        colIndex = 1;
        for (String deviceCode : deviceCodes) {
            int totalStatus1 = stats.stream()
                .filter(s -> s.getDeviceCode().equals(deviceCode))
                .mapToInt(LogDeviceStats::getCountStatus1)
                .sum();
                
            int totalStatus2 = stats.stream()
                .filter(s -> s.getDeviceCode().equals(deviceCode))
                .mapToInt(LogDeviceStats::getCountStatus2)
                .sum();
                
            int totalCount = stats.stream()
                .filter(s -> s.getDeviceCode().equals(deviceCode))
                .mapToInt(LogDeviceStats::getCountTotal)
                .sum();
            
            totalRow.createCell(colIndex).setCellValue(totalStatus1);
            totalRow.createCell(colIndex + 1).setCellValue(totalStatus2);
            totalRow.createCell(colIndex + 2).setCellValue(totalCount);
            colIndex += 3;
        }
        
        workbook.write(outputStream);
        workbook.close();
    }

    /**
     * Xuất phân tích 2 ra Excel
     * 
     * @param analysis kết quả phân tích
     * @param outputStream stream đầu ra
     * @param date ngày xuất
     * @throws IOException lỗi xuất file
     */
    public void exportStat2ToExcel(Map<String, Map<String, Integer>> analysis, 
                                  ByteArrayOutputStream outputStream, String date) throws IOException {
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Phân tích Log - " + date);
        
        if (analysis.isEmpty()) {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Không có dữ liệu");
            workbook.write(outputStream);
            workbook.close();
            return;
        }
        
        // Tạo style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // Lấy danh sách MAC và log keys
        List<String> macAddresses = new ArrayList<>(analysis.keySet());
        List<String> logKeys = new ArrayList<>(analysis.values().iterator().next().keySet());
        
        // Tạo hàng header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Địa chỉ MAC");
        
        for (int i = 0; i < logKeys.size(); i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(logKeys.get(i));
            cell.setCellStyle(headerStyle);
        }
        
        // Điền dữ liệu
        for (int i = 0; i < macAddresses.size(); i++) {
            String mac = macAddresses.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(mac);
            
            Map<String, Integer> macData = analysis.get(mac);
            for (int j = 0; j < logKeys.size(); j++) {
                String key = logKeys.get(j);
                int count = macData.getOrDefault(key, 0);
                row.createCell(j + 1).setCellValue(count);
            }
        }
        
        // Hàng tổng kết
        Row totalRow = sheet.createRow(macAddresses.size() + 1);
        totalRow.createCell(0).setCellValue("TỔNG CỘNG");
        
        for (int j = 0; j < logKeys.size(); j++) {
            String key = logKeys.get(j);
            int total = analysis.values().stream()
                .mapToInt(macData -> macData.getOrDefault(key, 0))
                .sum();
            totalRow.createCell(j + 1).setCellValue(total);
        }
        
        // Tự động điều chỉnh độ rộng cột
        for (int i = 0; i <= logKeys.size(); i++) {
            sheet.autoSizeColumn(i);
        }
        
        workbook.write(outputStream);
        workbook.close();
    }
}