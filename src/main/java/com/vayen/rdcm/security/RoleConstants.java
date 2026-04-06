package com.vayen.rdcm.security;

public final class RoleConstants {

    public static final String ADMIN = "admin";
    public static final String BRANCH_ADMIN = "branch_admin";
    public static final String USER = "user";
    public static final String LEGACY_EMPLOYEE = "employee";

    private RoleConstants() {
    }

    public static String normalize(String role) {
        if (LEGACY_EMPLOYEE.equals(role)) {
            return USER;
        }
        return role;
    }
}
