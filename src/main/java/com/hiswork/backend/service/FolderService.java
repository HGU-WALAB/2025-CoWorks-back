package com.hiswork.backend.service;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.Folder;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.*;
import com.hiswork.backend.repository.DocumentRepository;
import com.hiswork.backend.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FolderService {
    
    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    
    /**
     * 폴더 접근 권한 확인
     */
    private void checkFolderAccess(User user) {
        if (!user.canAccessFolders()) {
            throw new IllegalArgumentException("폴더 접근 권한이 없습니다.");
        }
    }
    
    /**
     * 루트 폴더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(User user) {
        checkFolderAccess(user);
        
        List<Folder> rootFolders = folderRepository.findByParentIsNullOrderByNameAsc();
        return rootFolders.stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 폴더 트리 구조로 조회 (자식 폴더 포함)
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderTree(User user) {
        checkFolderAccess(user);
        
        List<Folder> rootFolders = folderRepository.findRootFoldersWithChildren();
        return rootFolders.stream()
                .map(FolderResponse::fromWithChildren)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 폴더 조회
     */
    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID folderId, User user) {
        checkFolderAccess(user);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        return FolderResponse.fromWithChildren(folder);
    }
    
    /**
     * 폴더 생성
     */
    public FolderResponse createFolder(FolderCreateRequest request, User user) {
        checkFolderAccess(user);
        
        Folder parent = null;
        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 폴더를 찾을 수 없습니다: " + request.getParentId()));
        }
        
        // 같은 부모 하위에 동일한 이름의 폴더가 있는지 확인
        if (parent != null) {
            if (folderRepository.findByParentAndName(parent, request.getName()).isPresent()) {
                throw new IllegalArgumentException("같은 폴더 내에 동일한 이름의 폴더가 이미 존재합니다.");
            }
        } else {
            if (folderRepository.findByParentIsNullAndName(request.getName()).isPresent()) {
                throw new IllegalArgumentException("루트 폴더에 동일한 이름의 폴더가 이미 존재합니다.");
            }
        }
        
        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .createdBy(user)
                .build();
        
        folder = folderRepository.save(folder);
        
        if (parent != null) {
            parent.addChild(folder);
        }
        
        log.info("폴더 생성됨: {} by {}", folder.getName(), user.getName());
        return FolderResponse.from(folder);
    }
    
    /**
     * 폴더 수정
     */
    public FolderResponse updateFolder(UUID folderId, FolderUpdateRequest request, User user) {
        checkFolderAccess(user);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        // 이름이 제공된 경우에만 이름 변경
        if (request.getName() != null) {
            // 같은 부모 하위에 동일한 이름의 다른 폴더가 있는지 확인
            if (folder.getParent() != null) {
                folderRepository.findByParentAndName(folder.getParent(), request.getName())
                        .ifPresent(existingFolder -> {
                            if (!existingFolder.getId().equals(folderId)) {
                                throw new IllegalArgumentException("같은 폴더 내에 동일한 이름의 폴더가 이미 존재합니다.");
                            }
                        });
            } else {
                folderRepository.findByParentIsNullAndName(request.getName())
                        .ifPresent(existingFolder -> {
                            if (!existingFolder.getId().equals(folderId)) {
                                throw new IllegalArgumentException("루트 폴더에 동일한 이름의 폴더가 이미 존재합니다.");
                            }
                        });
            }
            folder.setName(request.getName());
        }
        
        // 부모 폴더 변경 (이동)
        if (request.getParentId() != null || (request.getParentId() == null && folder.getParent() != null)) {
            Folder newParent = null;
            if (request.getParentId() != null) {
                newParent = folderRepository.findById(request.getParentId())
                        .orElseThrow(() -> new IllegalArgumentException("대상 폴더를 찾을 수 없습니다: " + request.getParentId()));
                
                // 순환 참조 확인
                if (isDescendant(newParent, folder)) {
                    throw new IllegalArgumentException("폴더를 자신의 하위 폴더로 이동할 수 없습니다.");
                }
            }
            
            // 새로운 부모 하위에 동일한 이름의 폴더가 있는지 확인
            String folderName = request.getName() != null ? request.getName() : folder.getName();
            if (newParent != null) {
                folderRepository.findByParentAndName(newParent, folderName)
                        .ifPresent(existingFolder -> {
                            if (!existingFolder.getId().equals(folderId)) {
                                throw new IllegalArgumentException("대상 폴더에 동일한 이름의 폴더가 이미 존재합니다.");
                            }
                        });
            } else {
                folderRepository.findByParentIsNullAndName(folderName)
                        .ifPresent(existingFolder -> {
                            if (!existingFolder.getId().equals(folderId)) {
                                throw new IllegalArgumentException("루트 폴더에 동일한 이름의 폴더가 이미 존재합니다.");
                            }
                        });
            }
            
            folder.setParent(newParent);
        }
        
        folder = folderRepository.save(folder);
        
        log.info("폴더 수정됨: {} by {}", folder.getName(), user.getName());
        return FolderResponse.from(folder);
    }
    
    /**
     * 폴더가 다른 폴더의 하위 폴더인지 확인 (순환 참조 방지)
     */
    private boolean isDescendant(Folder ancestor, Folder folder) {
        Folder current = ancestor;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
    
    /**
     * 폴더 삭제
     */
    public void deleteFolder(UUID folderId, User user) {
        checkFolderAccess(user);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        // 폴더에 문서가 있는지 확인
        long documentCount = documentRepository.countByFolder(folder);
        if (documentCount > 0) {
            throw new IllegalArgumentException("폴더에 문서가 존재하여 삭제할 수 없습니다. 먼저 문서를 이동하거나 삭제해주세요.");
        }
        
        // 자식 폴더가 있는지 확인
        if (!folder.getChildren().isEmpty()) {
            throw new IllegalArgumentException("하위 폴더가 존재하여 삭제할 수 없습니다. 먼저 하위 폴더를 삭제해주세요.");
        }
        
        folderRepository.delete(folder);
        
        log.info("폴더 삭제됨: {} by {}", folder.getName(), user.getName());
    }
    
    /**
     * 폴더 내 문서 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getFolderDocuments(UUID folderId, User user) {
        checkFolderAccess(user);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        List<Document> documents = documentRepository.findByFolderOrderByCreatedAtDesc(folder);
        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 미분류 문서 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getUnclassifiedDocuments(User user) {
        checkFolderAccess(user);
        
        List<Document> documents = documentRepository.findByFolderIsNullOrderByCreatedAtDesc();
        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 문서를 폴더로 이동
     */
    public void moveDocumentToFolder(Long documentId, UUID folderId, User user) {
        checkFolderAccess(user);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        // 기존 폴더에서 제거
        if (document.getFolder() != null) {
            document.getFolder().removeDocument(document);
        }
        
        // 새 폴더에 추가
        folder.addDocument(document);
        documentRepository.save(document);
        
        log.info("문서 이동됨: {} -> {} by {}", document.getId(), folder.getName(), user.getName());
    }
    
    /**
     * 문서를 폴더에서 제거 (미분류로 이동)
     */
    public void removeDocumentFromFolder(Long documentId, User user) {
        checkFolderAccess(user);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));
        
        if (document.getFolder() != null) {
            document.getFolder().removeDocument(document);
            documentRepository.save(document);
            
            log.info("문서 폴더에서 제거됨: {} by {}", document.getId(), user.getName());
        }
    }
    
    /**
     * 특정 폴더의 자식 폴더들 조회
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getChildFolders(UUID folderId, User user) {
        checkFolderAccess(user);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다: " + folderId));
        
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        return children.stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList());
    }
}