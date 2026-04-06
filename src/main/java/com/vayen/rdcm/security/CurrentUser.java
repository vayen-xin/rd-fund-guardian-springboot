package com.vayen.rdcm.security;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder
public class CurrentUser implements Serializable {
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
