package com.hiswork.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMoveRequest {
    
    private UUID folderId; // null이면 폴더에서 제거 (미분류로 이동)
}