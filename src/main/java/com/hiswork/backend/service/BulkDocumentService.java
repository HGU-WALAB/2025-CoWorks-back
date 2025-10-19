package com.hiswork.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hiswork.backend.domain.*;
import com.hiswork.backend.dto.*;
import com.hiswork.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkDocumentService {
    
    private static final int MAX_ROWS = 500;
    
    private final ExcelParsingService excelParsingService;
    private final BulkStagingRepository bulkStagingRepository;
    private final BulkStagingItemRepository bulkStagingItemRepository;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRoleRepository documentRoleRepository;
    private final DocumentStatusLogRepository documentStatusLogRepository;
    private final ObjectMapper objectMapper;
    private final MailService mailService;

    // 엑셀 파일 업로드 및 데이터 임시저장
    public BulkPreviewResponse createPreview(MultipartFile file, Long templateId, User creator) {
        log.info("대량 업로드 시작 - 사용자 메일: {}, 템플릿: {}", creator.getEmail(), templateId);
        
        // 1. 템플릿 조회
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다: " + templateId));
        
        // 2. 파일 파싱
        List<ExcelParsingService.StudentRecord> records;
        try {
            records = excelParsingService.parseFile(file);
        } catch (Exception e) {
            throw new RuntimeException("파일 파싱 실패: " + e.getMessage(), e);
        }
        
        if (records.isEmpty()) {
            throw new RuntimeException("파일에 유효한 데이터가 없습니다");
        }
        
        if (records.size() > MAX_ROWS) {
            throw new RuntimeException(String.format("최대 %d행까지 처리할 수 있습니다. 현재: %d행", MAX_ROWS, records.size()));
        }
        
        // 3. 스테이징 생성
        String stagingId = UUID.randomUUID().toString();
        
        BulkStaging staging = BulkStaging.builder()
                .stagingId(stagingId)
                .creator(creator)
                .template(template)
                .originalFilename(file.getOriginalFilename())
                .totalRows(records.size())
                .build();
        
        // 4. 각 레코드 검증 및 스테이징 아이템 생성
        List<BulkStagingItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        
        for (int i = 0; i < records.size(); i++) {
            ExcelParsingService.StudentRecord record = records.get(i);
            int rowNumber = i + 2; // Excel 행 번호 (헤더 제외)
            
            String documentTitle = generateDocumentTitle(record);
            boolean isValid = record.isValid();
            String validationError = null;
            
            // 유효성 검증
            if (!isValid) {
                validationError = record.getValidationError();
            } 
            
            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
            }
            
            BulkStagingItem item = BulkStagingItem.builder()
                    .staging(staging)
                    .rowNumber(rowNumber)
                    .studentId(record.getStudentId())
                    .name(record.getName())
                    .email(record.getEmail())
                    .course(record.getCourse())
                    .documentTitle(documentTitle)
                    .isValid(isValid)
                    .validationError(validationError)
                    .build();
            
            items.add(item);
        }
        
        // 중복 제목 허용: 경고 추가 없음
        
        // 5. 스테이징 정보 업데이트 및 저장
        staging.setValidRows(validCount);
        staging.setInvalidRows(invalidCount);
        staging = bulkStagingRepository.save(staging);
        
        // 아이템들 저장
        bulkStagingItemRepository.saveAll(items);
        
        log.info("파일 업로드 완료 - 스테이징 ID: {}, 전체: {}, 유효: {}, 무효: {}", 
                stagingId, records.size(), validCount, invalidCount);
        
        // 6. 응답 생성
        return BulkPreviewResponse.uploaded(
                stagingId,
                template.getName(),
                file.getOriginalFilename(),
                records.size(),
                validCount,
                invalidCount,
                warnings
        );
    }

    // 엑셀 파일 문서 생성 확정 (preview -> commit)
    public BulkCommitResponse commitBulkCreation(BulkCommitRequest request, User creator) {
        log.info("대량 문서 생성 확정 시작 - 스테이징 ID: {}, 사용자: {}", request.getStagingId(), creator.getEmail());
        
        // 1. 스테이징 조회 및 권한 확인
        log.info("스테이징 조회 시도 - 스테이징 ID: {}, 사용자 ID: {}", request.getStagingId(), creator.getId());
        
        Optional<BulkStaging> stagingOpt = bulkStagingRepository.findByStagingIdAndCreatorId(request.getStagingId(), creator.getId());
        
        if (stagingOpt.isEmpty()) {
            // 스테이징이 존재하는지 확인
            Optional<BulkStaging> anyStaging = bulkStagingRepository.findById(request.getStagingId());
            if (anyStaging.isEmpty()) {
                log.error("스테이징이 존재하지 않습니다 - 스테이징 ID: {}", request.getStagingId());
                throw new RuntimeException("스테이징을 찾을 수 없습니다: " + request.getStagingId());
            } else {
                log.error("스테이징은 존재하지만 권한이 없습니다 - 스테이징 ID: {}, 요청자 ID: {}, 소유자 ID: {}", 
                        request.getStagingId(), creator.getId(), anyStaging.get().getCreator().getId());
                throw new RuntimeException("해당 스테이징에 대한 권한이 없습니다");
            }
        }
        
        BulkStaging staging = stagingOpt.get();
        
        if (!staging.canCommit()) {
            throw new RuntimeException("커밋할 수 없는 상태입니다. 상태: " + staging.getStatus());
        }
        
        // 2. 처리 가능한 아이템들 조회
        List<BulkStagingItem> processableItems = bulkStagingItemRepository.findProcessableItems(request.getStagingId());
        
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<BulkCommitResponse.CommitItem> commitItems = new ArrayList<>();
        
        // 3. 각 아이템 처리
        for (BulkStagingItem item : processableItems) {
            try {
                BulkCommitResponse.CommitItem commitItem = processItem(item, staging.getTemplate(), creator, request.getOnDuplicate(), request.getDeadline());
                commitItems.add(commitItem);
                
                switch (commitItem.getStatus()) {
                    case CREATED -> created++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
                
            } catch (Exception e) {
                log.error("아이템 처리 실패 - 행 {}: {}", item.getRowNumber(), e.getMessage(), e);
                
                item.setProcessingStatus(BulkStagingItem.ProcessingStatus.FAILED);
                item.setProcessingReason(e.getMessage());
                bulkStagingItemRepository.save(item);
                
                commitItems.add(BulkCommitResponse.CommitItem.builder()
                        .row(item.getRowNumber())
                        .studentId(item.getStudentId())
                        .name(item.getName())
                        .email(item.getEmail())
                        .course(item.getCourse())
                        .documentTitle(item.getDocumentTitle())
                        .status(BulkCommitResponse.CommitItem.CommitStatus.FAILED)
                        .reason(e.getMessage())
                        .build());
                
                failed++;
            }
        }
        
        // 4. 스테이징 상태 업데이트
        staging.setStatus(BulkStaging.StagingStatus.COMMITTED);
        bulkStagingRepository.save(staging);

        log.info("대량 문서 생성 완료 - 생성: {}, 건너뜀: {}, 실패: {}", created, skipped, failed);
        
        return BulkCommitResponse.builder()
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .items(commitItems)
                .build();
    }

    // staging에 있는 아이템들 조회
    public BulkStagingItemsResponse getStagingItems(String stagingId, User creator) {
        log.info("스테이징 아이템 조회 - 스테이징 ID: {}, 사용자: {}", stagingId, creator.getEmail());
        
        // 1. 스테이징 조회 및 권한 확인
        BulkStaging staging = bulkStagingRepository.findByStagingIdAndCreatorId(stagingId, creator.getId())
                .orElseThrow(() -> new RuntimeException("스테이징을 찾을 수 없거나 권한이 없습니다"));
        
        // 2. 스테이징 아이템들 조회
        List<BulkStagingItem> stagingItems = bulkStagingItemRepository.findByStagingIdOrderByRowNumber(stagingId);
        
        // 3. 응답 아이템들 생성
        List<BulkStagingItemsResponse.StagingItem> items = stagingItems.stream()
                .map(this::convertToStagingItem)
                .collect(Collectors.toList());
        
        log.info("스테이징 아이템 조회 완료 - 스테이징 ID: {}, 아이템 수: {}", stagingId, items.size());
        
        return BulkStagingItemsResponse.builder()
                .stagingId(stagingId)
                .templateName(staging.getTemplate().getName())
                .originalFilename(staging.getOriginalFilename())
                .totalRows(staging.getTotalRows())
                .validRows(staging.getValidRows())
                .invalidRows(staging.getInvalidRows())
                .items(items)
                .build();
    }

    // 파일 업로드 취소 (preview -> cancel)
    public BulkCancelResponse cancelBulkUpload(BulkCancelRequest request, User creator) {
        log.info("대량 업로드 취소 - 스테이징 ID: {}, 사용자: {}", request.getStagingId(), creator.getEmail());
        
        // 스테이징 조회 및 권한 확인
        BulkStaging staging = bulkStagingRepository.findByStagingIdAndCreatorId(request.getStagingId(), creator.getId())
                .orElseThrow(() -> new RuntimeException("스테이징을 찾을 수 없거나 권한이 없습니다"));
        
        if (!staging.canCancel()) {
            throw new RuntimeException("취소할 수 없는 상태입니다. 상태: " + staging.getStatus());
        }
        
        // 스테이징과 관련 아이템들 삭제
        bulkStagingRepository.delete(staging);
        
        log.info("대량 업로드 취소 완료 - 스테이징 ID: {}", request.getStagingId());
        
        return BulkCancelResponse.canceled();
    }
    
    // 학생 개별 정보 처리
    private BulkCommitResponse.CommitItem processItem(BulkStagingItem item, Template template, User creator,
                                                     BulkCommitRequest.OnDuplicateAction onDuplicate, java.time.LocalDateTime deadline) {
        
        String documentTitle = item.getDocumentTitle();
        
        // 문서 생성
        ObjectNode initialData = initializeDocumentData(template);
        
        // deadline이 제공되면 사용하고, 없으면 템플릿의 deadline 사용
        java.time.LocalDateTime finalDeadline = deadline != null ? deadline : template.getDeadline();

        Document document = Document.builder()
                .title(documentTitle)
                .template(template)
                .status(Document.DocumentStatus.EDITING)
                .data(initialData)
                .deadline(finalDeadline) // 요청된 마감일 또는 템플릿의 만료일
                .folder(template.getDefaultFolder())
                .build();
        
        document = documentRepository.save(document);
        
        // 초기 상태 로그 생성 (EDITING 상태로 시작)
        DocumentStatusLog initialStatusLog = DocumentStatusLog.builder()
                .document(document)
                .status(Document.DocumentStatus.EDITING)
                .changedByEmail(creator.getEmail())
                .changedByName(creator.getName())
                .comment("대량 업로드를 통한 문서 생성")
                .build();
        documentStatusLogRepository.save(initialStatusLog);
        log.info("문서 초기 상태 로그 생성 - 문서 ID: {}, 상태: EDITING", document.getId());
        
        // 사용자 검색 (이메일 또는 학번으로)
        Optional<User> existingUser = findUserByEmailOrId(item.getEmail(), item.getStudentId());
        
        // 문서 역할 할당
        DocumentRole.DocumentRoleBuilder roleBuilder = DocumentRole.builder()
                .document(document)
                .taskRole(DocumentRole.TaskRole.EDITOR);
        
        if (existingUser.isPresent()) {
            // 등록된 사용자 - 즉시 할당
            roleBuilder
                    .assignedUserId(existingUser.get().getId());
        } else {
            // 미등록 사용자 - 가입 대기 할당
            roleBuilder
                    .assignedUserId(item.getStudentId())
                    .pendingEmail(item.getEmail())
                    .pendingName(item.getName());
        }
        
        DocumentRole documentRole = roleBuilder.build();
        documentRoleRepository.save(documentRole);
        
        // 문서 상태 로그 기록 (편집자 할당으로 EDITING 상태)
        logBulkDocumentStatusChange(document, Document.DocumentStatus.EDITING, 
                existingUser.orElse(null), item, 
                "편집자 할당 - 대량 문서 생성");
        
        log.info("대량 문서 생성 - 문서 ID: {}, 제목: {}, 편집자: {} ({})", 
                document.getId(), documentTitle, item.getName(), item.getEmail());
        
        // 모든 사용자에게 편집자 할당 메일 전송 (등록/미등록 모두)
        try {
            String editorEmail = item.getEmail();
            String editorName = existingUser.isPresent() ? existingUser.get().getName() : item.getName();
            
            mailService.sendAssignEditorNotification(MailRequest.EditorAssignmentEmailCommand.builder()
                    .documentTitle(document.getTitle())
                    .creatorName(creator.getName())
                    .editorEmail(editorEmail)
                    .editorName(editorName)
                    .dueDate(document.getDeadline() != null ? document.getDeadline().atZone(java.time.ZoneId.systemDefault()) : null)
                    .projectName("CoWorks")
                    .build());
            
            if (existingUser.isPresent()) {
                log.info("편집자 할당 메일 전송 완료 (등록 사용자) - 문서: {}, 편집자: {}", document.getTitle(), editorEmail);
            } else {
                log.info("편집자 할당 메일 전송 완료 (미등록 사용자) - 문서: {}, 편집자: {}", document.getTitle(), editorEmail);
            }
        } catch (Exception e) {
            log.error("편집자 할당 메일 전송 실패 - 문서: {}, 편집자: {}", document.getTitle(), item.getEmail(), e);
            // 메일 전송 실패는 문서 생성에 영향을 주지 않도록 로그만 남김
        }
        
        // 아이템 상태 업데이트
        item.setProcessingStatus(BulkStagingItem.ProcessingStatus.CREATED);
        item.setCreatedDocumentId(document.getId());
        bulkStagingItemRepository.save(item);
        
        return BulkCommitResponse.CommitItem.builder()
                .row(item.getRowNumber())
                .studentId(item.getStudentId())
                .name(item.getName())
                .email(item.getEmail())
                .course(item.getCourse())
                .documentTitle(documentTitle)
                .status(BulkCommitResponse.CommitItem.CommitStatus.CREATED)
                .documentId(document.getId())
                .build();
    }
    
    
    // === 유틸리티 메서드들 ===

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

    private String generateDocumentTitle(ExcelParsingService.StudentRecord record) {
        return record.getName() + "_" + record.getCourse() + "_근무일지" ;
    }

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
     * 대량 문서 생성 시 상태 변경 로그 기록
     */
    private void logBulkDocumentStatusChange(Document document, Document.DocumentStatus status, 
                                            User assignedUser, BulkStagingItem item, String comment) {
        DocumentStatusLog statusLog = DocumentStatusLog.builder()
                .document(document)
                .status(status)
                .changedByEmail(assignedUser != null ? assignedUser.getEmail() : item.getEmail())
                .changedByName(assignedUser != null ? assignedUser.getName() : item.getName())
                .comment(comment)
                .build();
        
        documentStatusLogRepository.save(statusLog);
        log.info("대량 문서 상태 로그 생성 - 문서ID: {}, 상태: {}, 편집자: {} ({})", 
                document.getId(), status, item.getName(), item.getEmail());
    }
    
    // BulkStagingItem을 응답용 StagingItem으로 변환
    private BulkStagingItemsResponse.StagingItem convertToStagingItem(BulkStagingItem item) {
        // 사용자 등록 상태 확인
        Optional<User> existingUser = findUserByEmailOrId(item.getEmail(), item.getStudentId());
        BulkStagingItemsResponse.StagingItem.UserStatus userStatus = existingUser.isPresent() 
                ? BulkStagingItemsResponse.StagingItem.UserStatus.REGISTERED 
                : BulkStagingItemsResponse.StagingItem.UserStatus.UNREGISTERED;
        
        return BulkStagingItemsResponse.StagingItem.builder()
                .rowNumber(item.getRowNumber())
                .studentId(item.getStudentId())
                .name(item.getName())
                .email(item.getEmail())
                .course(item.getCourse())
                .documentTitle(item.getDocumentTitle())
                .isValid(item.isValid())
                .validationError(item.getValidationError())
                .processingStatus(item.getProcessingStatus() != null ? item.getProcessingStatus().name() : "PENDING")
                .processingReason(item.getProcessingReason())
                .createdDocumentId(item.getCreatedDocumentId())
                .userStatus(userStatus)
                .build();
    }
    
}
