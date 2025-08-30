package com.hiswork.backend.dto;

import com.hiswork.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailRequest {
    private String to;
    private String message;

    public static MailRequest of(String to, String message) {
        return MailRequest.builder()
                .to(to)
                .message(message)
                .build();
    }

    public static List<MailRequest> from(List<User> users, String message) {
        return users.stream()
                .map(user -> {
                    MailRequest req = new MailRequest();
                    req.setTo(user.getEmail());
                    req.setMessage(message);
                    return req;
                })
                .toList();
    }
}
