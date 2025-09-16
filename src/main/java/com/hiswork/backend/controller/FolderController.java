package com.hiswork.backend.controller;

import com.hiswork.backend.annotation.RequireFolderAccess;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.*;
import com.hiswork.backend.service.FolderService;
import com.hiswork.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
public class FolderController {
    
    private final FolderService folderService;
    private final AuthUtil authUtil;
    
    /**
     * 폴더 접근 권한 확인
     * GET /api/folders/access-check
     */
    @GetMapping("/access-check")
    public ResponseEntity<Boolean> checkFolderAccess(HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            boolean hasAccess = user.canAccessFolders();
            return ResponseEntity.ok(hasAccess);
        } catch (RuntimeException e) {
            log.warn("권한 확인 실패: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }
    
    /**
     * 루트 폴더 목록 조회
     * GET /api/folders
     */
    @GetMapping
    @RequireFolderAccess
    public ResponseEntity<List<FolderResponse>> getRootFolders(HttpServletRequest request) {
        User user = authUtil.getCurrentUser(request);
        List<FolderResponse> folders = folderService.getRootFolders(user);
        return ResponseEntity.ok(folders);
    }
    
    /**
     * 폴더 트리 구조 조회
     * GET /api/folders/tree
     */
    @GetMapping("/tree")
    @RequireFolderAccess
    public ResponseEntity<List<FolderResponse>> getFolderTree(HttpServletRequest request) {
        User user = authUtil.getCurrentUser(request);
        List<FolderResponse> tree = folderService.getFolderTree(user);
        return ResponseEntity.ok(tree);
    }
    
    /**
     * 특정 폴더 조회
     * GET /api/folders/{id}
     */
    @GetMapping("/{id}")
    @RequireFolderAccess
    public ResponseEntity<FolderResponse> getFolder(
            @PathVariable UUID id,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            FolderResponse folder = folderService.getFolder(id, user);
            return ResponseEntity.ok(folder);
        } catch (IllegalArgumentException e) {
            log.error("폴더 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 새 폴더 생성
     * POST /api/folders
     */
    @PostMapping
    @RequireFolderAccess
    public ResponseEntity<FolderResponse> createFolder(
            @RequestBody FolderCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User user = authUtil.getCurrentUser(httpRequest);
            FolderResponse folder = folderService.createFolder(request, user);
            return ResponseEntity.ok(folder);
        } catch (IllegalArgumentException e) {
            log.error("폴더 생성 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 폴더 수정
     * PUT /api/folders/{id}
     */
    @PutMapping("/{id}")
    @RequireFolderAccess
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable UUID id,
            @RequestBody FolderUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User user = authUtil.getCurrentUser(httpRequest);
            FolderResponse folder = folderService.updateFolder(id, request, user);
            return ResponseEntity.ok(folder);
        } catch (IllegalArgumentException e) {
            log.error("폴더 수정 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 폴더 삭제
     * DELETE /api/folders/{id}
     */
    @DeleteMapping("/{id}")
    @RequireFolderAccess
    public ResponseEntity<Void> deleteFolder(
            @PathVariable UUID id,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            folderService.deleteFolder(id, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("폴더 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 특정 폴더의 자식 폴더들 조회
     * GET /api/folders/{id}/children
     */
    @GetMapping("/{id}/children")
    @RequireFolderAccess
    public ResponseEntity<List<FolderResponse>> getChildFolders(
            @PathVariable UUID id,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            List<FolderResponse> children = folderService.getChildFolders(id, user);
            return ResponseEntity.ok(children);
        } catch (IllegalArgumentException e) {
            log.error("자식 폴더 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 폴더 내 문서 목록 조회
     * GET /api/folders/{id}/documents
     */
    @GetMapping("/{id}/documents")
    @RequireFolderAccess
    public ResponseEntity<List<DocumentResponse>> getFolderDocuments(
            @PathVariable UUID id,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            List<DocumentResponse> documents = folderService.getFolderDocuments(id, user);
            return ResponseEntity.ok(documents);
        } catch (IllegalArgumentException e) {
            log.error("폴더 문서 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 미분류 문서 목록 조회
     * GET /api/folders/unclassified/documents
     */
    @GetMapping("/unclassified/documents")
    @RequireFolderAccess
    public ResponseEntity<List<DocumentResponse>> getUnclassifiedDocuments(HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            List<DocumentResponse> documents = folderService.getUnclassifiedDocuments(user);
            return ResponseEntity.ok(documents);
        } catch (IllegalArgumentException e) {
            log.error("미분류 문서 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }
    
    /**
     * 문서를 폴더로 이동
     * POST /api/folders/{folderId}/documents/{documentId}
     */
    @PostMapping("/{folderId}/documents/{documentId}")
    @RequireFolderAccess
    public ResponseEntity<Void> moveDocumentToFolder(
            @PathVariable UUID folderId,
            @PathVariable Long documentId,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            folderService.moveDocumentToFolder(documentId, folderId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("문서 이동 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 문서를 폴더에서 제거 (미분류로 이동)
     * DELETE /api/folders/documents/{documentId}
     */
    @DeleteMapping("/documents/{documentId}")
    @RequireFolderAccess
    public ResponseEntity<Void> removeDocumentFromFolder(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        try {
            User user = authUtil.getCurrentUser(request);
            folderService.removeDocumentFromFolder(documentId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("문서 제거 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 문서 이동 (통합 API)
     * PUT /api/folders/documents/{documentId}/move
     */
    @PutMapping("/documents/{documentId}/move")
    @RequireFolderAccess
    public ResponseEntity<Void> moveDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentMoveRequest request,
            HttpServletRequest httpRequest) {
        try {
            User user = authUtil.getCurrentUser(httpRequest);
            if (request.getFolderId() != null) {
                folderService.moveDocumentToFolder(documentId, request.getFolderId(), user);
            } else {
                folderService.removeDocumentFromFolder(documentId, user);
            }
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("문서 이동 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
}