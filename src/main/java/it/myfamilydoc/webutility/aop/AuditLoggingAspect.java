package it.myfamilydoc.webutility.aop;

import it.myfamilydoc.webutility.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLoggingAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private HttpServletRequest request;

    @Pointcut("@annotation(auditable)")
    public void auditableMethods(Auditable auditable) {}

    @AfterReturning(pointcut = "auditableMethods(auditable)", returning = "result")
    public void logAuditableActionSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
        String username = extractUsername();
        String ip = request.getRemoteAddr();
        String methodName = joinPoint.getSignature().toShortString();
        String action = auditable.action();
        String entityType = auditable.entityType();
        String entityId = extractEntityId(auditable.entityIdParam(), joinPoint);

        auditLogService.log(
            action,
            username,
            ip,
            entityType,
            entityId,
            "SUCCESS",
            methodName + " completato con successo"
        );
    }

    @AfterThrowing(pointcut = "auditableMethods(auditable)", throwing = "ex")
    public void logAuditableActionFailure(JoinPoint joinPoint, Auditable auditable, Throwable ex) {
        String username = extractUsername();
        String ip = request.getRemoteAddr();
        String methodName = joinPoint.getSignature().toShortString();
        String action = auditable.action();
        String entityType = auditable.entityType();
        String entityId = extractEntityId(auditable.entityIdParam(), joinPoint);

        auditLogService.log(
            action,
            username,
            ip,
            entityType,
            entityId,
            "FAILURE",
            methodName + " fallito. Errore: " + ex.getMessage()
        );
    }

    private String extractUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String extractEntityId(String paramName, JoinPoint joinPoint) {
        if (paramName == null || paramName.isEmpty()) return null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && args[i] != null) {
                return args[i].toString();
            }
        }

        return null;
    }
}