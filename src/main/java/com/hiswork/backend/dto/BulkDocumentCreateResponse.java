package com.hiswork.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkDocumentCreateResponse {
    private int created;   // 등록된 사용자에게 할당된 문서 수
    private int pending;   // 미등록 사용자에게 임시 할당된 문서 수
    private List<UserInfo> createdUsers; // 생성완료된 사용자들의 정보
    private List<UserInfo> pendingUsers; // 가입대기 사용자들의 정보
    private List<ErrorItem> errors; // 실패한 항목들
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;      // 학번
        private String name;    // 이름
        private String email;   // 이메일
        private String course;  // 과목/학과
    }
    
    @Data
    @Builder
    public static class ErrorItem {
        private int row;      // 행 번호 (Excel에서)
        private String reason; // 실패 이유
    }
}
