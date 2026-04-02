package com.vayen.rdcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> list;
    private long page;
    private long size;
    private long total;
}
