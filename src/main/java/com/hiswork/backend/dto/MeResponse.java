package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private String id;
    private String email;
    private String name;
    private String position;
    private String role;
    private boolean hasFolderAccess;

    public static MeResponse from(User user) {
        return MeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .position(user.getPosition() != null ? user.getPosition().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .hasFolderAccess(user.canAccessFolders())
                .build();
    }
}


