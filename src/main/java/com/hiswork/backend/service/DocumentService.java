package com.hiswork.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hiswork.backend.domain.Document;
import com.hiswork.backend.domain.DocumentRole;
import com.hiswork.backend.domain.Template;
import com.hiswork.backend.domain.TasksLog;
import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.BulkDocumentCreateResponse;
import com.hiswork.backend.dto.DocumentUpdateRequest;
import com.hiswork.backend.dto.MailRequest;
import com.hiswork.backend.repository.DocumentRepository;
import com.hiswork.backend.repository.DocumentRoleRepository;
import com.hiswork.backend.repository.TemplateRepository;
import com.hiswork.backend.repository.TasksLogRepository;
import com.hiswork.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.hiswork.backend.domain.Position;
import com.hiswork.backend.domain.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final MailService mailService;
    private final DocumentRepository documentRepository;
    private final TemplateRepository templateRepository;
    private final DocumentRoleRepository documentRoleRepository;
    private final TasksLogRepository tasksLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
  
    public Document createDocument(Long templateId, User creator, String editorEmail, String title) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        // 새로운 구조와 기존 구조 모두 지원
        ObjectNode initialData = initializeDocumentData(template);
        
        Document document = Document.builder()
                .template(template)
                .title(title)
                .data(initialData)
                .status(Document.DocumentStatus.DRAFT)
                .folder(template.getDefaultFolder())  // 템플릿의 기본 폴더 적용
                .build();
        
        document = documentRepository.save(document);
        
        // 생성자 역할 할당
        DocumentRole creatorRole = DocumentRole.builder()
                .document(document)
                .assignedUser(creator)
                .taskRole(DocumentRole.TaskRole.CREATOR)
                .canAssignReviewer(true) // 생성자에게 검토자 지정 권한 부여
                .build();
        
        documentRoleRepository.save(creatorRole);
        
        // 생성자 작업 로그
        TasksLog creatorTask = TasksLog.builder()
                .document(document)
                .assignedBy(creator)
                .assignedUser(creator)
                .status(TasksLog.TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        
        tasksLogRepository.save(creatorTask);

        User editor = null;
        
        // 편집자가 지정된 경우 편집자 역할 할당
        if (editorEmail != null && !editorEmail.trim().isEmpty()) {
            editor = getUserOrCreate(editorEmail, "Editor User");
            
            DocumentRole editorRole = DocumentRole.builder()
                    .document(document)
                    .assignedUser(editor)
                    .taskRole(DocumentRole.TaskRole.EDITOR)
                    .canAssignReviewer(true) // 편집자에게 검토자 지정 권한 부여
                    .build();
            
            documentRoleRepository.save(editorRole);
            
            // 편집자 작업 로그
            TasksLog editorTask = TasksLog.builder()
                    .document(document)
                    .assignedBy(creator)
                    .assignedUser(editor)
                    .status(TasksLog.TaskStatus.PENDING)
                    .build();
            
            tasksLogRepository.save(editorTask);
            
            // 문서 상태를 EDITING으로 변경
            document.setStatus(Document.DocumentStatus.EDITING);
            document = documentRepository.save(document);
        }

        mailService.sendAssignEditorNotification(MailRequest.EditorAssignmentEmailCommand.builder()
                        .documentTitle(template.getName())
                        .creatorName(creator.getName())
                        .editorEmail(editorEmail)
                        .editorName(editor != null ? editor.getName() : "미지정")
                        .dueDate(document.getDeadline() != null ? document.getDeadline().atZone(java.time.ZoneId.systemDefault()) : null)
                        .projectName("Hiswork")
                .build());

        return document;
    }
    
    private ObjectNode initializeDocumentData(Template template) {
        ObjectNode data = objectMapper.createObjectNode();
        
        // 템플릿에서 coordinateFields 복사 (레거시 지원용)
        if (template.getCoordinateFields() != null && !template.getCoordinateFields().trim().isEmpty()) {
            try {
                JsonNode coordinateFieldsJson = objectMapper.readTree(template.getCoordinateFields());
                if (coordinateFieldsJson.isArray()) {
                    // coordinateFields를 값만 빈 상태로 복사
                    ArrayNode fieldsArray = objectMapper.createArrayNode();
                    for (JsonNode field : coordinateFieldsJson) {
                        ObjectNode fieldCopy = field.deepCopy();
                        fieldCopy.put("value", ""); // 값은 빈 문자열로 초기화
                        fieldsArray.add(fieldCopy);
                    }
                    data.set("coordinateFields", fieldsArray);
                    log.info("문서 생성 시 템플릿의 coordinateFields 복사: {} 개 필드", fieldsArray.size());
                }
            } catch (Exception e) {
                log.warn("템플릿 coordinateFields 파싱 실패: {}", e.getMessage());
            }
        }
        
        return data;
    }
    
    public Document updateDocumentData(Long documentId, DocumentUpdateRequest request, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 권한 확인 - 편집자만 수정 가능 (생성자는 편집 불가)
        if (!isEditor(document, user)) {
            throw new RuntimeException("문서를 수정할 권한이 없습니다");
        }
        
        // 문서 데이터 업데이트
        document.setData(request.getData());
        document = documentRepository.save(document);
        
        // 작업 로그 추가
        TasksLog updateLog = TasksLog.builder()
                .document(document)
                .assignedBy(user)
                .assignedUser(user)
                .status(TasksLog.TaskStatus.IN_PROGRESS)
                .build();
        
        tasksLogRepository.save(updateLog);

        return document;
    }
    
    public Document startEditing(Long documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 편집자만 편집 시작 가능
        if (!isEditor(document, user)) {
            throw new RuntimeException("편집할 권한이 없습니다");
        }
        
        // 문서가 DRAFT 상태인 경우만 EDITING으로 변경
        if (document.getStatus() == Document.DocumentStatus.DRAFT) {
            document.setStatus(Document.DocumentStatus.EDITING);
            document = documentRepository.save(document);
            
            // 편집 시작 로그 추가
            TasksLog editingLog = TasksLog.builder()
                    .document(document)
                    .assignedBy(user)
                    .assignedUser(user)
                    .status(TasksLog.TaskStatus.IN_PROGRESS)
                    .build();
            
            tasksLogRepository.save(editingLog);
            
            log.info("문서 편집 시작 - 문서 ID: {}, 사용자: {}, 상태: {} -> EDITING", 
                    documentId, user.getId(), "DRAFT");
        }
        
        return document;
    }
    
    public Document submitForReview(Long documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 편집자 또는 생성자만 검토 요청 가능
        if (!isEditor(document, user) && !isCreator(document, user)) {
            throw new RuntimeException("검토 요청할 권한이 없습니다");
        }
        
        // 현재 상태가 EDITING이어야 함
        if (document.getStatus() != Document.DocumentStatus.EDITING && document.getStatus() != Document.DocumentStatus.REJECTED) {
            throw new RuntimeException("문서가 편집 상태가 아닙니다");
        }
        
        // 상태를 READY_FOR_REVIEW로 변경
        document.setStatus(Document.DocumentStatus.READY_FOR_REVIEW);
        document = documentRepository.save(document);
        
        // 작업 로그 추가
        TasksLog reviewRequestLog = TasksLog.builder()
                .document(document)
                .assignedBy(user)
                .assignedUser(user)
                .status(TasksLog.TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        
        tasksLogRepository.save(reviewRequestLog);
        
        return document;
    }
    
    public Document assignEditor(Long documentId, String editorEmail, User assignedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User editor = getUserOrCreate(editorEmail, "Editor User");
        
        // 기존 편집자 역할이 있다면 제거
        documentRoleRepository.findByDocumentAndRole(documentId, DocumentRole.TaskRole.EDITOR)
                .ifPresent(existingRole -> documentRoleRepository.delete(existingRole));
        
        // 새로운 편집자 역할 할당
        DocumentRole editorRole = DocumentRole.builder()
                .document(document)
                .assignedUser(editor)
                .taskRole(DocumentRole.TaskRole.EDITOR)
                .build();
        
        documentRoleRepository.save(editorRole);
        
        // 작업 로그 추가
        TasksLog editorTask = TasksLog.builder()
                .document(document)
                .assignedBy(assignedBy)
                .assignedUser(editor)
                .status(TasksLog.TaskStatus.PENDING)
                .build();
        
        tasksLogRepository.save(editorTask);
        
        // 문서 상태를 EDITING으로 변경
        document.setStatus(Document.DocumentStatus.EDITING);
        document = documentRepository.save(document);
        
        return document;
    }
    
    public Document assignReviewer(Long documentId, String reviewerEmail, User assignedBy) {
        log.info("검토자 할당 요청 - 문서 ID: {}, 검토자 이메일: {}, 요청자: {}", 
                documentId, reviewerEmail, assignedBy.getId());

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        log.info("문서 정보 - ID: {}, 상태: {}, 생성자: {}", 
                document.getId(), document.getStatus(), document.getTemplate().getCreatedBy().getId());
        
        // 검토자 할당 권한 확인
        boolean isCreator = isCreator(document, assignedBy);
        boolean hasAssignReviewerPermission = false;
        
        if (isCreator) {
            // 생성자는 항상 검토자 할당 가능
            hasAssignReviewerPermission = true;
        } else {
            // 편집자인 경우 canAssignReviewer 권한 확인
            Optional<DocumentRole> editorRole = documentRoleRepository.findByDocumentAndUserAndRole(
                    documentId, assignedBy.getId(), DocumentRole.TaskRole.EDITOR);
            
            if (editorRole.isPresent() && Boolean.TRUE.equals(editorRole.get().getCanAssignReviewer())) {
                hasAssignReviewerPermission = true;
            }
        }
        
        log.info("권한 확인 - 요청자: {}, 생성자 여부: {}, 검토자 할당 권한: {}", 
                assignedBy.getId(), isCreator, hasAssignReviewerPermission);
        
        if (!hasAssignReviewerPermission) {
            throw new RuntimeException("검토자를 할당할 권한이 없습니다. 생성자이거나 검토자 지정 권한이 있는 편집자만 가능합니다.");
        }
        
        User reviewer = getUserOrCreate(reviewerEmail, "Reviewer User");
        
        // 기존 검토자 역할이 있다면 제거
        documentRoleRepository.findByDocumentAndRole(documentId, DocumentRole.TaskRole.REVIEWER)
                .ifPresent(existingRole -> documentRoleRepository.delete(existingRole));
        
        // 새로운 검토자 역할 할당
        DocumentRole reviewerRole = DocumentRole.builder()
                .document(document)
                .assignedUser(reviewer)
                .taskRole(DocumentRole.TaskRole.REVIEWER)
                .canAssignReviewer(false) // 검토자는 기본적으로 검토자 지정 권한 없음
                .build();
        
        documentRoleRepository.save(reviewerRole);
        
        // 작업 로그 추가
        TasksLog reviewerTask = TasksLog.builder()
                .document(document)
                .assignedBy(assignedBy)
                .assignedUser(reviewer)
                .status(TasksLog.TaskStatus.PENDING)
                .build();

        tasksLogRepository.save(reviewerTask);

        mailService.sendAssignReviewerNotification(MailRequest.ReviewerAssignmentEmailCommand.builder()
                        .documentTitle(document.getTemplate().getName()) // 문서 제목도 관리 해야함.
                        .editorName(assignedBy.getName())
                        .reviewerEmail(reviewerEmail)
                        .reviewerName(reviewer.getName())
                        .reviewDueDate(document.getDeadline() != null ? document.getDeadline().atZone(java.time.ZoneId.systemDefault()) : null)
                        .projectName("Hiswork") // 프로젝트 이름 따로 관리해야할듯. 지금은 고정값
                .build());

        document.setStatus(Document.DocumentStatus.REVIEWING);
        documentRepository.save(document);
        
        return document;
    }
    
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findDocumentsByUserId(user.getId());
    }
    
    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    private boolean isCreator(Document document, User user) {
        return documentRoleRepository.findByDocumentAndUserAndRole(
                document.getId(), user.getId(), DocumentRole.TaskRole.CREATOR
        ).isPresent();
    }
    
    private boolean isEditor(Document document, User user) {
        return documentRoleRepository.findByDocumentAndUserAndRole(
                document.getId(), user.getId(), DocumentRole.TaskRole.EDITOR
        ).isPresent();
    }
    
    private boolean isReviewer(Document document, User user) {
        return documentRoleRepository.findByDocumentAndUserAndRole(
                document.getId(), user.getId(), DocumentRole.TaskRole.REVIEWER
        ).isPresent();
    }
    
    private User getUserOrCreate(String email, String defaultName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .id(java.util.UUID.randomUUID().toString()) // UUID를 String으로 생성
                            .name(defaultName)
                            .email(email)
                            .password(passwordEncoder.encode("defaultPassword123"))
                            .position(Position.교직원)
                            .role(Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });
    }
    
    public Document completeEditing(Long documentId, User user) {
        log.info("편집 완료 처리 시작 - 문서 ID: {}, 사용자: {}", documentId, user.getId());
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        log.info("문서 정보 - ID: {}, 상태: {}, 템플릿 ID: {}", 
                document.getId(), document.getStatus(), document.getTemplate().getId());
        
        // 편집자만 편집 완료 가능
        boolean isEditor = isEditor(document, user);
        
        if (!isEditor) {
            throw new RuntimeException("편집을 완료할 권한이 없습니다");
        }
        
        // 현재 상태가 EDITING이어야 함
        if (document.getStatus() != Document.DocumentStatus.EDITING && document.getStatus() != Document.DocumentStatus.REJECTED) {
            log.warn("문서 상태 오류 - 현재 상태: {}, 예상 상태: EDITING", document.getStatus());
            throw new RuntimeException("문서가 편집 상태가 아닙니다");
        }
        
        log.info("필수 필드 검증 시작");
        // 필수 필드 검증
        validateRequiredFields(document);
        log.info("필수 필드 검증 완료");
        
        // 상태를 READY_FOR_REVIEW로 변경
        document.setStatus(Document.DocumentStatus.READY_FOR_REVIEW);
        document = documentRepository.save(document);
        
        // 작업 로그 추가
        TasksLog completeLog = TasksLog.builder()
                .document(document)
                .assignedBy(user)
                .assignedUser(user)
                .status(TasksLog.TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        
        tasksLogRepository.save(completeLog);
        
        return document;
    }
    
    public Document approveDocument(Long documentId, User user, String signatureData) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 검토자만 승인 가능
        if (!isReviewer(document, user)) {
            throw new RuntimeException("문서를 승인할 권한이 없습니다");
        }
        
        // 현재 상태가 REVIEWING 이어야 함
        if (document.getStatus() != Document.DocumentStatus.REVIEWING) {
            throw new RuntimeException("문서가 검토 대기 상태가 아닙니다");
        }
        
        // 서명 데이터를 문서 데이터에 추가
        if (signatureData != null && document.getData() != null) {
            ObjectNode data = (ObjectNode) document.getData();
            ObjectNode signatures = data.has("signatures") ? 
                    (ObjectNode) data.get("signatures") : objectMapper.createObjectNode();
            signatures.put(user.getEmail(), signatureData);
            data.set("signatures", signatures);
            document.setData(data);
        }
        
        // 상태를 COMPLETED로 변경
        document.setStatus(Document.DocumentStatus.COMPLETED);
        document = documentRepository.save(document);
        
        // 작업 로그 추가
        TasksLog approveLog = TasksLog.builder()
                .document(document)
                .assignedBy(user)
                .assignedUser(user)
                .status(TasksLog.TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        
        tasksLogRepository.save(approveLog);
        
        return document;
    }
    
    public Document rejectDocument(Long documentId, User user, String reason) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // 검토자만 거부 가능
        if (!isReviewer(document, user)) {
            throw new RuntimeException("문서를 거부할 권한이 없습니다");
        }
        
        // 현재 상태가 REVIEWING 이어야 함
        if (document.getStatus() != Document.DocumentStatus.REVIEWING) {
            throw new RuntimeException("문서가 검토 대기 상태가 아닙니다");
        }
        
        // 상태를 REJECTED로 변경
        document.setStatus(Document.DocumentStatus.REJECTED);
        document = documentRepository.save(document);
        
        // 작업 로그 추가
        TasksLog rejectLog = TasksLog.builder()
                .document(document)
                .assignedBy(user)
                .assignedUser(user)
                .status(TasksLog.TaskStatus.REJECTED)
                .rejectionReason(reason)
                .completedAt(LocalDateTime.now())
                .build();

        documentRoleRepository.findByDocumentAndRole(documentId, DocumentRole.TaskRole.REVIEWER)
                .ifPresent(existingRole -> documentRoleRepository.delete(existingRole));
        
        tasksLogRepository.save(rejectLog);
        
        return document;
    }
    
    public boolean canReview(Long documentId, User user) {
        try {
            Document document = getDocumentById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // 검토자이고 문서가 검토 대기 상태인지 확인
            return isReviewer(document, user) && 
                   document.getStatus() == Document.DocumentStatus.READY_FOR_REVIEW;
        } catch (Exception e) {
            log.error("Error checking review permission for document {} and user {}", documentId, user.getId(), e);
            return false;
        }
    }
    
    private void validateRequiredFields(Document document) {
        try {
            log.info("필수 필드 검증 시작 - 문서 ID: {}", document.getId());
            
            // 문서 데이터가 없으면 검증하지 않음
            if (document.getData() == null) {
                log.info("문서 데이터가 없어 필수 필드 검증을 건너뜁니다");
                return;
            }
            
            JsonNode documentData = document.getData();
            JsonNode coordinateFields = documentData.get("coordinateFields");
            
            if (coordinateFields == null || !coordinateFields.isArray()) {
                log.info("coordinateFields가 없거나 배열이 아닙니다");
                return;
            }
            
            log.info("검증할 필드 수: {}", coordinateFields.size());
            
            List<String> missingFields = new ArrayList<>();
            
            for (JsonNode field : coordinateFields) {
                JsonNode requiredNode = field.get("required");
                JsonNode valueNode = field.get("value");
                JsonNode labelNode = field.get("label");
                JsonNode idNode = field.get("id");
                
                String fieldId = idNode != null ? idNode.asText() : "unknown";
                String fieldLabel = labelNode != null ? labelNode.asText() : fieldId;
                boolean isRequired = requiredNode != null && requiredNode.asBoolean();
                String value = valueNode != null ? valueNode.asText() : "";
                
                log.debug("필드 검증 - ID: {}, Label: {}, Required: {}, Value: '{}'", 
                         fieldId, fieldLabel, isRequired, value);
                
                // required가 true이고 value가 비어있으면 필수 필드 누락
                if (isRequired) {
                    if (value == null || value.trim().isEmpty()) {
                        String fieldName = labelNode != null ? labelNode.asText() : 
                                         (idNode != null ? "필드 " + idNode.asText() : "알 수 없는 필드");
                        missingFields.add(fieldName);
                        log.warn("필수 필드 누락 - {}", fieldName);
                    }
                }
            }
            
            if (!missingFields.isEmpty()) {
                String errorMessage = "다음 필수 필드를 채워주세요: " + String.join(", ", missingFields);
                log.error("필수 필드 검증 실패: {}", errorMessage);
                throw new RuntimeException(errorMessage);
            }
            
            log.info("필수 필드 검증 완료 - 모든 필수 필드가 채워져 있습니다");
            
        } catch (Exception e) {
            if (e.getMessage().contains("필수 필드")) {
                throw e; // 필수 필드 검증 오류는 그대로 전파
            }
            log.warn("필수 필드 검증 중 오류 발생: {}", e.getMessage());
        }
    }
    
    @Transactional
    public BulkDocumentCreateResponse createBulkDocuments(
            Long templateId, User creator, List<ExcelParsingService.StudentRecord> records) {
        
        int created = 0; // 실제로 문서가 생성된 사용자 수
        int pending = 0; // 가입을 하지 않아 할당되지 못한 사용자 수
        List<BulkDocumentCreateResponse.UserInfo> createdUsers = new ArrayList<>();
        List<BulkDocumentCreateResponse.UserInfo> pendingUsers = new ArrayList<>();
        List<BulkDocumentCreateResponse.ErrorItem> errors = new ArrayList<>();
        
        // 템플릿 조회
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다: " + templateId));
        
        for (int i = 0; i < records.size(); i++) {
            ExcelParsingService.StudentRecord record = records.get(i);
            int rowNumber = i + 2; // Excel에서 첫 번째 행은 헤더이므로 +2
            
            try {
                // 1. 입력 데이터 유효성 검사
                if (!record.isValid()) {
                    errors.add(BulkDocumentCreateResponse.ErrorItem.builder()
                        .row(rowNumber)
                        .reason(record.getValidationError())
                        .build());
                    continue;
                }
                
                // 2. 문서 제목 생성
                String title = record.getName() + "_" + record.getCourse() + " 근무일지";
                
                // 3. 중복 제목 확인
                if (documentRepository.existsByTitle(title)) {
                    errors.add(BulkDocumentCreateResponse.ErrorItem.builder()
                        .row(rowNumber)
                        .reason("중복된 제목: " + title)
                        .build());
                    continue;
                }
                
                // 4. 사용자 검색 (이메일 또는 학번으로)
                Optional<User> existingUser = findUserByEmailOrId(record.getEmail(), record.getStudentId());
                
                // 5. 문서 생성
                Document document = Document.builder()
                    .title(title)
                    .template(template)
                    .status(Document.DocumentStatus.EDITING)
                    .data(objectMapper.createObjectNode())
                    .build();
                
                document = documentRepository.save(document);
                
                // 6. 문서 역할 할당
                DocumentRole.DocumentRoleBuilder roleBuilder = DocumentRole.builder()
                    .document(document)
                    .taskRole(DocumentRole.TaskRole.EDITOR);
                
                if (existingUser.isPresent()) {
                    // 등록된 사용자 - ACTIVE 할당
                    roleBuilder
                        .assignedUser(existingUser.get())
                        .assignmentStatus(DocumentRole.AssignmentStatus.ACTIVE);
                    created++;
                    
                    // 생성된 사용자 정보 추가
                    createdUsers.add(BulkDocumentCreateResponse.UserInfo.builder()
                        .id(record.getStudentId())
                        .name(record.getName())
                        .email(record.getEmail())
                        .course(record.getCourse())
                        .build());
                    
                    log.info("등록된 사용자에게 문서 할당: {} -> {}", title, existingUser.get().getEmail());
                } else {
                    // 미등록 사용자 - PENDING 임시 할당
                    roleBuilder
                        .assignedUser(null)
                        .pendingUserId(record.getStudentId())
                        .pendingEmail(record.getEmail())
                        .pendingName(record.getName())
                        .assignmentStatus(DocumentRole.AssignmentStatus.PENDING)
                        .claimStatus(DocumentRole.ClaimStatus.PENDING);
                    pending++;
                    
                    // 대기 중인 사용자 정보 추가
                    pendingUsers.add(BulkDocumentCreateResponse.UserInfo.builder()
                        .id(record.getStudentId())
                        .name(record.getName())
                        .email(record.getEmail())
                        .course(record.getCourse())
                        .build());
                    
                    log.info("미등록 사용자에게 문서 임시 할당: {} -> {} ({})", title, record.getEmail(), record.getStudentId());
                }
                
                DocumentRole documentRole = roleBuilder.build();
                documentRoleRepository.save(documentRole);
                
                // 7. 작업 로그 생성
                TasksLog tasksLog = TasksLog.builder()
                    .document(document)
                    .assignedUser(existingUser.orElse(null))
                    .assignedBy(creator)
                    .status(TasksLog.TaskStatus.PENDING)
                    .build();
                
                tasksLogRepository.save(tasksLog);
                
            } catch (Exception e) {
                log.error("문서 생성 실패 - 행 {}: {}", rowNumber, e.getMessage(), e);
                errors.add(com.hiswork.backend.dto.BulkDocumentCreateResponse.ErrorItem.builder()
                    .row(rowNumber)
                    .reason(e.getMessage())
                    .build());
            }
        }
        
        log.info("대량 문서 생성 완료 - 생성: {}, 임시할당: {}, 오류: {}", created, pending, errors.size());
        
        return BulkDocumentCreateResponse.builder()
            .created(created)
            .pending(pending)
            .createdUsers(createdUsers)
            .pendingUsers(pendingUsers)
            .errors(errors)
            .build();
    }
    
    /**
     * 이메일 또는 ID로 사용자 검색
     */
    private Optional<User> findUserByEmailOrId(String email, String studentId) {
        // 먼저 이메일로 검색
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            return userByEmail;
        }
        
        // 학번이 있으면 ID로도 검색
        if (studentId != null && !studentId.trim().isEmpty()) {
            return userRepository.findById(studentId);
        }
        
        return Optional.empty();
    }
    
    /**
     * 회원가입 시 임시 할당된 문서들을 실제 사용자에게 연결
     */
    @Transactional
    public int linkPendingDocuments(User newUser) {
        List<DocumentRole> pendingRoles = new ArrayList<>();
        
        // 이메일로 임시 할당된 문서들 검색
        pendingRoles.addAll(documentRoleRepository.findByPendingEmail(newUser.getEmail()));
        
        // ID(학번)로 임시 할당된 문서들 검색
        if (newUser.getId() != null) {
            pendingRoles.addAll(documentRoleRepository.findByPendingUserId(newUser.getId()));
        }
        
        int linkedCount = 0;
        for (DocumentRole role : pendingRoles) {
            // 임시 할당을 실제 사용자로 전환
            role.setAssignedUser(newUser);
            role.setAssignmentStatus(DocumentRole.AssignmentStatus.ACTIVE);
            role.setClaimStatus(DocumentRole.ClaimStatus.CLAIMED);
            role.setPendingUserId(null);
            role.setPendingEmail(null);
            role.setPendingName(null);
            
            documentRoleRepository.save(role);
            
            // 관련 TasksLog도 업데이트
            List<TasksLog> relatedLogs = tasksLogRepository.findByDocumentIdAndAssignedUserIsNull(role.getDocument().getId());
            for (TasksLog log : relatedLogs) {
                log.setAssignedUser(newUser);
                tasksLogRepository.save(log);
            }
            
            linkedCount++;
            log.info("임시 할당 문서를 실제 사용자에게 연결: {} -> {}", role.getDocument().getTitle(), newUser.getEmail());
        }
        
        return linkedCount;
    }
    
     // 폴더 관리 권한 확인 (hasAccessFolders=true)
    public void validateFolderManagePermission(User user) {
        log.info("폴더 관리 권한 검증 - 사용자: {}, 권한: {}", user.getEmail(), user.canAccessFolders());
         if (!user.canAccessFolders()) {
             throw new RuntimeException("폴더 관리 권한이 없습니다. 관리자에게 문의하세요.");
         }
    }
} 