package com.hiswork.backend.repository;

import com.hiswork.backend.domain.Folder;
import com.hiswork.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    
    /**
     * 루트 폴더들(부모가 없는 폴더들) 조회
     */
    List<Folder> findByParentIsNullOrderByNameAsc();
    
    /**
     * 특정 폴더의 자식 폴더들 조회
     */
    List<Folder> findByParentOrderByNameAsc(Folder parent);
    
    /**
     * 특정 사용자가 생성한 폴더들 조회
     */
    List<Folder> findByCreatedByOrderByCreatedAtDesc(User createdBy);
    
    /**
     * 이름으로 폴더 검색 (대소문자 구분 없음)
     */
    List<Folder> findByNameContainingIgnoreCase(String name);
    
    /**
     * 특정 부모 폴더 하위에서 이름으로 폴더 찾기
     */
    Optional<Folder> findByParentAndName(Folder parent, String name);
    
    /**
     * 루트 폴더에서 이름으로 폴더 찾기
     */
    Optional<Folder> findByParentIsNullAndName(String name);
    
    /**
     * 폴더 계층 구조를 포함한 조회 (N+1 문제 해결)
     */
    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.children WHERE f.parent IS NULL ORDER BY f.name")
    List<Folder> findRootFoldersWithChildren();
    
    /**
     * 특정 폴더의 전체 하위 폴더 개수 조회
     */
    @Query("SELECT COUNT(f) FROM Folder f WHERE f.parent = :parent")
    long countByParent(@Param("parent") Folder parent);
    
    /**
     * 특정 폴더의 문서 개수 조회
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.folder = :folder")
    long countDocumentsByFolder(@Param("folder") Folder folder);
}