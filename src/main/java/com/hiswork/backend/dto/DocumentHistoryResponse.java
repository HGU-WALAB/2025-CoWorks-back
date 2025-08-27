package com.hiswork.backend.dto;

import com.hiswork.backend.domain.DocumentRole;
import com.hiswork.backend.domain.TasksLog;
import com.hiswork.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentHistoryResponse {
    private Long id;
    private String status;
    private String action;
    private String description;
    private String performedBy;
    private String performedByName;
    private LocalDateTime createdAt;
    
    public static DocumentHistoryResponse from(TasksLog tasksLog) {
        return DocumentHistoryResponse.builder()
                .id(tasksLog.getId())
                .status(tasksLog.getStatus().name())
                .action(getActionFromStatus(tasksLog.getStatus()))
                .description(getDescriptionFromStatus(tasksLog.getStatus(), tasksLog.getAssignedBy(), tasksLog.getAssignedUser()))
                .performedBy(tasksLog.getAssignedUser().getEmail())
                .performedByName(tasksLog.getAssignedUser().getName())
                .createdAt(tasksLog.getCreatedAt())
                .build();
    }
    
    public static DocumentHistoryResponse from(TasksLog tasksLog, Map<String, DocumentRole.TaskRole> userRoleMap) {
        return DocumentHistoryResponse.builder()
                .id(tasksLog.getId())
                .status(tasksLog.getStatus().name())
                .action(getActionFromStatus(tasksLog.getStatus()))
                .description(getDescriptionFromStatusWithRole(tasksLog.getStatus(), tasksLog.getAssignedBy(), tasksLog.getAssignedUser(), userRoleMap))
                .performedBy(tasksLog.getAssignedUser().getEmail())
                .performedByName(tasksLog.getAssignedUser().getName())
                .createdAt(tasksLog.getCreatedAt())
                .build();
    }
    
    private static String getActionFromStatus(TasksLog.TaskStatus status) {
        switch (status) {
            case PENDING:
                return "TASK_ASSIGNED";
            case COMPLETED:
                return "TASK_COMPLETED";
            case REJECTED:
                return "TASK_REJECTED";
            default:
                return "STATUS_CHANGED";
        }
    }
    
    private static String getDescriptionFromStatus(TasksLog.TaskStatus status, User assignedBy, User assignedUser) {
        switch (status) {
            case PENDING:
                return assignedBy.getName() + "이(가) " + assignedUser.getName() + "에게 작업을 할당했습니다.";
            case COMPLETED:
                return assignedUser.getName() + "이(가) 작업을 완료했습니다.";
            case REJECTED:
                return assignedUser.getName() + "이(가) 작업을 거부했습니다.";
            default:
                return "상태가 변경되었습니다.";
        }
    }
    
    private static String getDescriptionFromStatusWithRole(TasksLog.TaskStatus status, User assignedBy, User assignedUser, Map<String, DocumentRole.TaskRole> userRoleMap) {
        switch (status) {
            case PENDING:
                String assignedByRole = getRoleDisplayName(userRoleMap.get(assignedBy.getEmail()));
                String assignedUserRole = getRoleDisplayName(userRoleMap.get(assignedUser.getEmail()));
                return assignedBy.getName() + "(" + assignedByRole + ")이(가) " + assignedUser.getName() + "(" + assignedUserRole + ")에게 작업을 할당했습니다.";
            case COMPLETED:
                String completedUserRole = getRoleDisplayName(userRoleMap.get(assignedUser.getEmail()));
                return assignedUser.getName() + "(" + completedUserRole + ")이(가) 작업을 완료했습니다.";
            case REJECTED:
                String rejectedUserRole = getRoleDisplayName(userRoleMap.get(assignedUser.getEmail()));
                return assignedUser.getName() + "(" + rejectedUserRole + ")이(가) 작업을 거부했습니다.";
            default:
                return "상태가 변경되었습니다.";
        }
    }
    
    private static String getRoleDisplayName(DocumentRole.TaskRole role) {
        if (role == null) return "미지정";
        switch (role) {
            case CREATOR: return "생성자";
            case EDITOR: return "편집자";
            case REVIEWER: return "검토자";
            default: return "미지정";
        }
    }
} 