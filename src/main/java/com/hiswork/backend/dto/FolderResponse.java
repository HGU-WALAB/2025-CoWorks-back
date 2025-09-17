package com.hiswork.backend.dto;

import com.hiswork.backend.domain.Folder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    
    private UUID id;
    private String name;
    private UUID parentId;
    private String parentName;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fullPath;
    private long childrenCount;
    private long documentsCount;
    private List<FolderResponse> children;
    
    public static FolderResponse from(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
                .createdBy(folder.getCreatedBy().getId())
                .createdByName(folder.getCreatedBy().getName())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .fullPath(folder.getFullPath())
                .childrenCount(folder.getChildren().size())
                .documentsCount(folder.getDocuments().size())
                .build();
    }
    
    public static FolderResponse fromWithChildren(Folder folder) {
        FolderResponse response = from(folder);
        response.setChildren(
                folder.getChildren().stream()
                        .map(FolderResponse::fromWithChildren)  // 재귀적으로 하위 폴더도 children 포함
                        .collect(Collectors.toList())
        );
        return response;
    }
}