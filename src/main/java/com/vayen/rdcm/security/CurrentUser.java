package com.vayen.rdcm.security;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentUser {
    Long id;
    Long companyId;
    String username;
    String name;
    String role;
    boolean active;

    public boolean isAdmin() {
        return RoleConstants.ADMIN.equals(role);
    }
}
