package com.viecultures.backend.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.common.annotation.AuditLog;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.modules.gamification.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AOP Aspect tu dong ghi nhan thao tac quan tri (Audit Log) khi phuong thuc
 * duoc gan @AuditLog.
 * Thu thap thong tin Admin/Moderator, IP, User-Agent, tham so goi va ket qua
 * thuc thi vao user_activity_logs.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAuditAspect {

    private final UserActivityLogService userActivityLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object auditAdminAction(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            status = "FAILED";
            errorMessage = t.getMessage();
            throw t;
        } finally {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            try {
                recordAuditLog(joinPoint, auditLog, result, status, errorMessage, executionTimeMs);
            } catch (Throwable auditEx) {
                log.error("[AdminAuditAspect] Unexpected error occurred while capturing audit log: {}",
                        auditEx.getMessage(), auditEx);
            }
        }
    }

    private void recordAuditLog(
            ProceedingJoinPoint joinPoint,
            AuditLog auditLog,
            Object result,
            String status,
            String errorMessage,
            long executionTimeMs) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String path = request != null ? request.getRequestURI() : null;
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // Xac dinh thong tin nguoi dung thuc hien tu SecurityContextHolder
        String userId = null;
        String userEmail = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                userId = user.getId();
                userEmail = user.getEmail();
            } else if (principal instanceof UserDetails userDetails) {
                userEmail = userDetails.getUsername();
            } else {
                userEmail = authentication.getName();
            }
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        String targetEntityId = null;
        Map<String, Object> paramSummary = new LinkedHashMap<>();

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String paramName = paramNames[i];
                Object arg = args[i];

                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                        || arg instanceof BindingResult || arg instanceof Principal
                        || arg instanceof Authentication || arg instanceof UserDetails) {
                    continue;
                }

                if (arg instanceof MultipartFile multipartFile) {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("filename", multipartFile.getOriginalFilename());
                    fileInfo.put("sizeBytes", multipartFile.getSize());
                    fileInfo.put("contentType", multipartFile.getContentType());
                    paramSummary.put(paramName, fileInfo);
                    continue;
                }

                if ("id".equalsIgnoreCase(paramName) || paramName.toLowerCase().endsWith("id")) {
                    if (arg != null) {
                        targetEntityId = String.valueOf(arg);
                    }
                }

                // Masking thong tin nhay cam neu co truong chua password hoac secret
                if (paramName.toLowerCase().contains("password")
                        || paramName.toLowerCase().contains("secret")
                        || paramName.toLowerCase().contains("apikey")
                        || paramName.toLowerCase().contains("token")) {
                    paramSummary.put(paramName, "******");
                } else {
                    paramSummary.put(paramName, sanitizeArgument(arg));
                }
            }
        }

        // Trich xuat entityId tu ket qua tra ve neu chua tim thay trong param
        if (!StringUtils.hasText(targetEntityId) && result != null) {
            targetEntityId = extractIdFromResult(result);
        }

        // Xay dung JSON Payload chi tiet
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("action", auditLog.action());
        if (StringUtils.hasText(auditLog.entity())) {
            payloadMap.put("entity", auditLog.entity());
        }
        if (StringUtils.hasText(auditLog.description())) {
            payloadMap.put("description", auditLog.description());
        }
        if (StringUtils.hasText(targetEntityId)) {
            payloadMap.put("targetEntityId", targetEntityId);
        }
        payloadMap.put("method", signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        payloadMap.put("status", status);
        if (StringUtils.hasText(errorMessage)) {
            payloadMap.put("errorMessage", errorMessage);
        }
        payloadMap.put("executionTimeMs", executionTimeMs);
        if (!paramSummary.isEmpty()) {
            payloadMap.put("params", paramSummary);
        }

        if (result instanceof ResponseEntity<?> responseEntity) {
            payloadMap.put("httpStatusCode", responseEntity.getStatusCode().value());
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.warn("[AdminAuditAspect] Could not serialize audit payload to JSON: {}", e.getMessage());
            payloadJson = payloadMap.toString();
        }

        userActivityLogService.logAdminAction(
                userId,
                userEmail,
                auditLog.action(),
                auditLog.entity(),
                path,
                payloadJson,
                ipAddress,
                userAgent);
    }

    private Object sanitizeArgument(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof Number || arg instanceof Boolean || arg instanceof String || arg instanceof Enum<?>) {
            return arg;
        }
        try {
            String json = objectMapper.writeValueAsString(arg);
            if (json.length() > 2000) {
                return json.substring(0, 2000) + "... [truncated]";
            }
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return arg.toString();
        }
    }

    private String extractIdFromResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Object target = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                target = responseEntity.getBody();
            }
            if (target == null) {
                return null;
            }

            // Neu co ApiResponse, lay data
            try {
                var methodGetData = target.getClass().getMethod("getData");
                Object innerData = methodGetData.invoke(target);
                if (innerData != null) {
                    target = innerData;
                }
            } catch (NoSuchMethodException ignored) {
            }

            // Thu tim method getId()
            try {
                var methodGetId = target.getClass().getMethod("getId");
                Object idObj = methodGetId.invoke(target);
                if (idObj != null) {
                    return String.valueOf(idObj);
                }
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception ex) {
            log.trace("[AdminAuditAspect] Could not inspect result for ID: {}", ex.getMessage());
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String[] headerCandidates = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "X-Real-IP"
        };
        for (String header : headerCandidates) {
            String ipList = request.getHeader(header);
            if (StringUtils.hasText(ipList) && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
