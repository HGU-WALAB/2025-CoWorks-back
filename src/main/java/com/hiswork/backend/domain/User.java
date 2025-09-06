package com.hiswork.backend.domain;

import com.hiswork.backend.dto.AuthDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import java.util.UUID;

@Entity
@Table(name = "users")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    // 학번
    @Column(name = "unique_id", nullable = false, length = 50)
    private String uniqueId;

    // 이름
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 이메일
    @Column(name = "email", length = 320)
    private String email;

    // 학년
    @Column(name = "grade")
    private Integer grade;

    // 학기
    @Column(name = "semester")
    private Integer semester;

    // 학과
    @Column(name = "department", length = 50)
    private String department;

    // 전공1
    @Column(name = "major1", length = 50)
    private String major1;

    // 전공2
    @Column(name = "major2", length = 50)
    private String major2;

    // 역할 (USER, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // 직책 (교직원, 교수, 학생, 연구원, 행정직원, 기타)
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private Position position;

    // 프로필 이미지 URL
    private String profileImageUrl;

    // 서명 이미지 URL
    private String signatureImageUrl;


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
                .role(Role.USER) // 기본 상태를 USER로 설정
                .position(Position.학생) // 기본값을 학생으로 설정
                .build();
    }
}