package com.hiswork.backend.aspect;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FolderAccessAspect {
    
    private final AuthUtil authUtil;
    
    @Around("@annotation(com.hiswork.backend.annotation.RequireFolderAccess)")
    public Object checkFolderAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            User user = authUtil.getCurrentUser(request);
            
            if (!user.canAccessFolders()) {
                log.warn("폴더 접근 권한이 없는 사용자: {} ({})", user.getName(), user.getId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "폴더 접근 권한이 없습니다.");
            }
            
            log.debug("폴더 접근 권한 확인 완료: {} ({})", user.getName(), user.getId());
            return joinPoint.proceed();
            
        } catch (RuntimeException e) {
            log.warn("인증 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
    }
}