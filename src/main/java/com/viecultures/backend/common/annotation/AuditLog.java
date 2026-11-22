package com.viecultures.backend.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation danh dau cac phuong thuc can ghi nhan nhat ky thao tac quan tri
 * (Admin Audit Log).
 * Tu dong ghi lai thong tin nguoi thuc hien (Admin/Moderator), IP, User-Agent,
 * tham so va ket qua vao bang user_activity_logs.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * Ten hanh dong hoac ma su kien (vi du: "CREATE_POST", "UPDATE_USER_ROLE",
     * "DELETE_VOCAB").
     */
    String action();

    /**
     * Ten thuc the chiu tac dong (vi du: "POST", "USER", "VOCABULARY",
     * "SUBSCRIPTION", "AI_CONFIG").
     */
    String entity() default "";

    /**
     * Mo ta tom tat ngu canh cua hanh dong quan tri.
     */
    String description() default "";
}
