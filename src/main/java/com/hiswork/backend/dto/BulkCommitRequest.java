package com.hiswork.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BulkCommitRequest {
    
    @NotBlank(message = "stagingId는 필수입니다")
    private String stagingId;
    
    private OnDuplicateAction onDuplicate = OnDuplicateAction.SKIP;
    
    public enum OnDuplicateAction {
        SKIP,         // 중복 건너뛰기
        UPDATE_TITLE, // 제목에 번호 추가 (예: "김철수_컴퓨터공학과 근무일지 (2)")
        ERROR         // 오류로 처리
    }
}
