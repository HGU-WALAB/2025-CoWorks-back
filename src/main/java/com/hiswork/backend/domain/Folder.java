package com.hiswork.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "folders")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"parent", "children", "documents"})
@EqualsAndHashCode(of = "id")
public class Folder {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    // 부모 폴더와의 관계 (계층 구조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;
    
    // 자식 폴더들
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Folder> children = new ArrayList<>();
    
    // 폴더에 속한 문서들
    @OneToMany(mappedBy = "folder", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
    
    // 폴더 생성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 생성자
    public Folder(String name, User createdBy) {
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.children = new ArrayList<>();
        this.documents = new ArrayList<>();
    }
    
    public Folder(String name, Folder parent, User createdBy) {
        this.name = name;
        this.parent = parent;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.children = new ArrayList<>();
        this.documents = new ArrayList<>();
    }
    
    // 헬퍼 메서드들
    public void addChild(Folder child) {
        children.add(child);
        child.setParent(this);
    }
    
    public void removeChild(Folder child) {
        children.remove(child);
        child.setParent(null);
    }
    
    public void addDocument(Document document) {
        documents.add(document);
        document.setFolder(this);
    }
    
    public void removeDocument(Document document) {
        documents.remove(document);
        document.setFolder(null);
    }
    
    // 루트 폴더인지 확인
    public boolean isRoot() {
        return parent == null;
    }
    
    // 폴더의 전체 경로 구하기
    public String getFullPath() {
        if (isRoot()) {
            return name;
        }
        return parent.getFullPath() + "/" + name;
    }
    
    // 생명주기 메서드
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}