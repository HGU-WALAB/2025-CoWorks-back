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
public class FolderCreateRequest {
    
    private String name;
    private UUID parentId; // null이면 루트 폴더
}