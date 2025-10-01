package com.hiswork.backend.controller;

import com.hiswork.backend.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/convert-to-image")
    public ResponseEntity<byte[]> convertPdfToImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // PDF를 PNG 이미지로 변환 (150 DPI)
            byte[] imageBytes = pdfService.convertPdfToImage(file.getInputStream(), 150);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/convert-to-images")
    public ResponseEntity<Map<String, Object>> convertPdfToImages(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam(value = "dpi", defaultValue = "150") int dpi) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // PDF의 모든 페이지를 이미지로 변환
            List<byte[]> imageBytesList = pdfService.convertPdfToImages(file.getInputStream(), dpi);

            // 이미지들을 Base64로 인코딩하여 JSON 응답으로 반환
            List<String> base64Images = new ArrayList<>();
            for (byte[] imageBytes : imageBytesList) {
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                base64Images.add("data:image/png;base64," + base64Image);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalPages", imageBytesList.size());
            response.put("pages", base64Images);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "PDF 변환 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
