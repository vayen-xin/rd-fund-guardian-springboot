package com.vayen.rdcm.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonthlyFeeSchemaResponse {
    private List<FeeCategory> categories;

    @Data
    public static class FeeCategory {
        private String code;
        private String label;
        private List<FeeSubItem> items;
    }

    @Data
    public static class FeeSubItem {
        private String code;
        private String label;
    }
}
