package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkCancelResponse {
    private String status;
    private String message;
    
    public static BulkCancelResponse canceled() {
        return BulkCancelResponse.builder()
                .status("CANCELED")
                .message("업로드가 취소되었습니다")
                .build();
    }
}
