package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BulkCancelRequest {
    
    @NotBlank(message = "stagingId는 필수입니다")
    private String stagingId;
}
