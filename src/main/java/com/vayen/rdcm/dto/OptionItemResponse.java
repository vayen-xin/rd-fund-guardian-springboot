package com.vayen.rdcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OptionItemResponse {
    private Long id;
    private String code;
    private String name;
    private String extra;
}
