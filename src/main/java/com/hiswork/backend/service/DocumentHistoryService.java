package com.hiswork.backend.service;

import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.DocumentRole;
import com.hiswork.backend.domain.TasksLog;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.DocumentHistoryResponse;
import com.hiswork.backend.repository.DocumentRepository;
import com.hiswork.backend.repository.DocumentRoleRepository;
import com.hiswork.backend.repository.TasksLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DocumentHistoryService {
    
    private final TasksLogRepository tasksLogRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRoleRepository documentRoleRepository;
    
    public List<DocumentHistoryResponse> getDocumentHistory(Long documentId, User user) {
        log.info("문서 히스토리 조회 - 문서 ID: {}, 요청자: {}", documentId, user.getEmail());
        
        // 문서 존재 확인
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 사용자가 해당 문서에 접근 권한이 있는지 확인
        // (생성자, 편집자, 검토자 중 하나라도 해당되면 접근 가능)
        boolean hasAccess = tasksLogRepository.existsByDocumentIdAndAssignedUserEmail(documentId, user.getEmail());
        
        if (!hasAccess) {
            log.warn("문서 히스토리 접근 권한 없음 - 문서 ID: {}, 사용자: {}", documentId, user.getEmail());
            throw new RuntimeException("문서 히스토리에 접근할 권한이 없습니다.");
        }
        
        // 문서의 모든 작업 로그 조회
        List<TasksLog> tasksLogs = tasksLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
        
        // 문서의 역할 정보 조회 (사용자별 역할 매핑)
        List<DocumentRole> documentRoles = documentRoleRepository.findByDocumentId(documentId);
        Map<String, DocumentRole.TaskRole> userRoleMap = documentRoles.stream()
                .collect(Collectors.toMap(
                        role -> role.getAssignedUser().getEmail(),
                        DocumentRole::getTaskRole
                ));
        
        log.info("문서 히스토리 조회 완료 - 문서 ID: {}, 로그 수: {}, 역할 수: {}", 
                documentId, tasksLogs.size(), documentRoles.size());
        
        return tasksLogs.stream()
                .map(tasksLog -> DocumentHistoryResponse.from(tasksLog, userRoleMap))
                .collect(Collectors.toList());
    }
} 