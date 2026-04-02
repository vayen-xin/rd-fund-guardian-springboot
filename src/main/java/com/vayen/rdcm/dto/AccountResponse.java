package com.vayen.rdcm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountResponse {
    private Long id;
    private Long companyId;
    private String username;
    private String name;
    private String role;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
}
