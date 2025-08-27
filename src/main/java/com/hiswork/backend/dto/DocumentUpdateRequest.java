package com.hiswork.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentUpdateRequest {
    
    @NotNull(message = "문서 데이터는 필수입니다")
    private JsonNode data;
} 