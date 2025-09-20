package com.hiswork.backend.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ExcelParsingService {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    @Data
    public static class StudentRecord {
        private String studentId;   // 학번
        private String name;        // 이름
        private String email;       // 이메일
        private String course;      // 과목
        
        public boolean isValid() {
            return studentId != null && !studentId.trim().isEmpty() &&
                   name != null && !name.trim().isEmpty() &&
                   email != null && EMAIL_PATTERN.matcher(email.trim()).matches() &&
                   course != null && !course.trim().isEmpty();
        }
        
        public String getValidationError() {
            if (studentId == null || studentId.trim().isEmpty()) {
                return "학번(ID)이 비어있습니다";
            }
            if (name == null || name.trim().isEmpty()) {
                return "이름이 비어있습니다";
            }
            if (email == null || email.trim().isEmpty()) {
                return "이메일이 비어있습니다";
            }
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                return "유효하지 않은 이메일 형식입니다";
            }
            if (course == null || course.trim().isEmpty()) {
                return "과목명이 비어있습니다";
            }
            return null;
        }
    }
    
    public List<StudentRecord> parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("파일명이 없습니다");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case "xlsx":
            case "xls":
                return parseExcelFile(file);
            case "csv":
                return parseCsvFile(file);
            default:
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. Excel(.xlsx, .xls) 또는 CSV 파일만 업로드 가능합니다.");
        }
    }
    
    private List<StudentRecord> parseExcelFile(MultipartFile file) throws IOException {
        List<StudentRecord> records = new ArrayList<>();
        
        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용
            
            boolean isFirstRow = true;
            for (Row row : sheet) {
                // 첫 번째 행은 헤더로 간주하고 스킵
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // 빈 행 스킵
                if (isRowEmpty(row)) {
                    continue;
                }
                
                StudentRecord record = new StudentRecord();
                
                // 첫 번째 컬럼: 학번 (ID)
                Cell studentIdCell = row.getCell(0);
                if (studentIdCell != null) {
                    record.setStudentId(getCellValueAsString(studentIdCell).trim());
                }
                
                // 두 번째 컬럼: 이름 (필수)
                Cell nameCell = row.getCell(1);
                if (nameCell != null) {
                    record.setName(getCellValueAsString(nameCell).trim());
                }
                
                // 세 번째 컬럼: 이메일 (필수)
                Cell emailCell = row.getCell(2);
                if (emailCell != null) {
                    record.setEmail(getCellValueAsString(emailCell).trim());
                }
                
                // 네 번째 컬럼: 과목 (필수)
                Cell courseCell = row.getCell(3);
                if (courseCell != null) {
                    record.setCourse(getCellValueAsString(courseCell).trim());
                }
                
                records.add(record);
            }
        }
        
        log.info("Excel 파일 파싱 완료: {} 행", records.size());
        return records;
    }
    
    private List<StudentRecord> parseCsvFile(MultipartFile file) throws IOException {
        List<StudentRecord> records = new ArrayList<>();
        
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), "UTF-8");
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord csvRecord : parser) {
                StudentRecord record = new StudentRecord();
                
                // CSV에서는 컬럼 인덱스 또는 헤더명으로 접근
                if (csvRecord.size() >= 4) {
                    record.setStudentId(csvRecord.get(0).trim()); // 첫 번째 컬럼: 학번 (ID)
                    record.setName(csvRecord.get(1).trim());      // 두 번째 컬럼: 이름
                    record.setEmail(csvRecord.get(2).trim());     // 세 번째 컬럼: 이메일
                    record.setCourse(csvRecord.get(3).trim());    // 네 번째 컬럼: 과목
                    
                    records.add(record);
                }
            }
        }
        
        log.info("CSV 파일 파싱 완료: {} 행", records.size());
        return records;
    }
    
    private Workbook createWorkbook(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream());
        } else {
            return new HSSFWorkbook(file.getInputStream());
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        
        for (int i = 0; i < 4; i++) { // 첫 4개 컬럼만 확인
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
