package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkPreviewResponse {
    private String stagingId;
    private String templateName;
    private String originalFilename;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private String status; // "UPLOADED" 등
    private String message;
    private List<String> warnings;
    
    public static BulkPreviewResponse uploaded(String stagingId, String templateName, String filename, 
                                             int totalRows, int validRows, int invalidRows, List<String> warnings) {
        return BulkPreviewResponse.builder()
                .stagingId(stagingId)
                .templateName(templateName)
                .originalFilename(filename)
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(invalidRows)
                .status("UPLOADED")
                .message("파일이 성공적으로 업로드되어 임시 저장되었습니다.")
                .warnings(warnings)
                .build();
    }
}
