package com.company.material.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Set;

public class PermissionUtil {

    private static final Set<String> ADMIN_ROLES = Set.of("管理员", "财务", "超级管理员");
    private static final Set<String> FINANCE_ROLES = Set.of("财务", "超级管理员");

    public static boolean hasAdminPermission(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return role != null && ADMIN_ROLES.contains(role);
    }

    public static boolean hasFinancePermission(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return role != null && FINANCE_ROLES.contains(role);
    }

    public static boolean hasDepreciationPermission(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return role != null && ADMIN_ROLES.contains(role);
    }

    public static boolean hasDisposalPermission(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return role != null && ADMIN_ROLES.contains(role);
    }

    public static ResponseEntity<?> forbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "权限不足，需要管理员或财务角色"));
    }

    public static String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    public static Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
