package com.hiswork.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hiswork.backend.dto.AuthDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @Column(name = "unique_id", nullable = false, length = 50)
    private String uniqueId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Role role;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "major1", length = 50)
    private String major1;

    @Column(name = "major2", length = 50)
    private String major2;

    public static User from(AuthDto dto) {
        return User.builder()
                .uniqueId(dto.getUniqueId())
                .name(dto.getName())
                .email(dto.getEmail())
                .department(dto.getDepartment())
                .major1(dto.getMajor1())
                .major2(dto.getMajor2())
                .grade(dto.getGrade())
                .semester(dto.getSemester())
                .role(Role.USER) // 기본 상태를 ACTIVE로 설정
                .build();
    }

//    @Id
//    @GeneratedValue(generator = "uuid2")
//    @GenericGenerator(name = "uuid2", strategy = "uuid2")
//    @Column(columnDefinition = "UUID")
//    private UUID id;
//
//    @Column(unique = true, nullable = false)
//    private String email;
//
//    @Column(nullable = false)
//    @JsonIgnore
//    private String password;
//
//    @Column(nullable = false)
//    private String name;
//
//    @Enumerated(EnumType.STRING)
//    private Position position;
//
//    private String profileImage;
//
//    @Enumerated(EnumType.STRING)
//    private Role role;
//
//    private String signatureImageUrl;
    
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;
    
    public enum Role {
        ADMIN, USER
    }
    
    public enum Position {
        교직원, 교수, 학생, 연구원, 행정직원, 기타
    }
} 