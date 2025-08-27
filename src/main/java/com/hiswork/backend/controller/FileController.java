package com.hiswork.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FileController {
    
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    
    @Value("${app.file.pdf-templates-dir}")
    private String pdfTemplatesDir;
    
    /**
     * PDF 템플릿 이미지 파일 서비스
     */
    @GetMapping("/pdf-template-images/{filename:.+}")
    public ResponseEntity<Resource> getPdfTemplateImage(@PathVariable String filename) {
        log.info("PDF 템플릿 이미지 요청: {}", filename);
        
        try {
            Path filePath = Paths.get(pdfTemplatesDir).resolve(filename).normalize();
            log.info("파일 경로: {}", filePath);
            log.info("PDF 템플릿 디렉토리: {}", pdfTemplatesDir);
            
            Resource resource = new FileSystemResource(filePath);
            
            if (!resource.exists()) {
                log.warn("파일이 존재하지 않음: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            if (!resource.isReadable()) {
                log.warn("파일을 읽을 수 없음: {}", filePath);
                return ResponseEntity.status(403).build();
            }
            
            // 파일 확장자에 따른 Content-Type 설정
            String contentType = getContentType(filename);
            log.info("Content-Type: {}", contentType);
            
            log.info("PDF 템플릿 이미지 제공 성공: {}", filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("PDF 템플릿 이미지 서비스 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * PDF 파일 다운로드
     */
    @GetMapping("/pdf-templates/{filename:.+}")
    public ResponseEntity<Resource> getPdfTemplate(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(pdfTemplatesDir).resolve(filename).normalize();
            Resource resource = new FileSystemResource(filePath);
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("PDF 파일을 찾을 수 없음: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("PDF 파일 서비스 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 파일 확장자에 따른 Content-Type 결정
     */
    private String getContentType(String filename) {
        try {
            Path path = Paths.get(filename);
            String contentType = Files.probeContentType(path);
            
            if (contentType != null) {
                return contentType;
            }
            
            // 확장자 기반 fallback
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                case "pdf":
                    return "application/pdf";
                default:
                    return "application/octet-stream";
            }
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
} 