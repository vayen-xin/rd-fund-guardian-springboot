package com.vayen.rdcm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类（基于 Jackson）
 */
@Slf4j
@UtilityClass
public class JsonUtils {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * 解析月度成本数据JSON
     */
    public static CostData parseCostData(String json) {
        try {
            Map<String, List<Map<String, Object>>> data = MAPPER.readValue(json, 
                new TypeReference<Map<String, List<Map<String, Object>>>>(){});
            return new CostData(data);
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败: {}", json, e);
            throw new RuntimeException("JSON解析失败", e);
        }
    }
    
    /**
     * 将成本数据转换为JSON字符串（用于继承时的金额清零）
     */
    public static String zeroOutAmounts(String json) {
        try {
            Map<String, List<Map<String, Object>>> data = MAPPER.readValue(json, 
                new TypeReference<Map<String, List<Map<String, Object>>>>(){});
            
            // 遍历8大类，将每个条目的amount设为0
            String[] categories = {
                "labor", "direct_material", "direct_fuel", "direct_rental",
                "depreciation", "amortization", "design", "commissioning",
                "outsourced", "other"
            };
            
            for (String category : categories) {
                List<Map<String, Object>> items = data.get(category);
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        // 将常见金额字段置零
                        item.put("amount", 0.0);
                        // 嵌套对象
                        if (item.containsKey("salary")) {
                            Map<String, Object> salary = (Map<String, Object>) item.get("salary");
                            salary.put("amount", 0.0);
                        }
                        if (item.containsKey("social_security")) {
                            Map<String, Object> ss = (Map<String, Object>) item.get("social_security");
                            ss.put("amount", 0.0);
                        }
                        if (item.containsKey("housing_fund")) {
                            Map<String, Object> hf = (Map<String, Object>) item.get("housing_fund");
                            hf.put("amount", 0.0);
                        }
                        if (item.containsKey("depreciation")) {
                            Map<String, Object> dep = (Map<String, Object>) item.get("depreciation");
                            dep.put("amount", 0.0);
                        }
                    }
                }
            }
            
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("JSON处理失败: {}", json, e);
            throw new RuntimeException("JSON处理失败", e);
        }
    }
    
    /**
     * 计算总计金额（从JSON中累加所有类别的amount总和）
     */
    public static double calculateGrandTotal(String json) {
        CostData data = parseCostData(json);
        return data.calculateTotal();
    }
    
    /**
     * 成本数据封装类
     */
    public static class CostData {
        private Map<String, List<Map<String, Object>>> data;
        
        public CostData(Map<String, List<Map<String, Object>>> data) {
            this.data = data;
        }
        
        public Map<String, List<Map<String, Object>>> getData() {
            return data;
        }
        
        public double calculateTotal() {
            double total = 0.0;
            for (Map.Entry<String, List<Map<String, Object>>> entry : data.entrySet()) {
                for (Map<String, Object> item : entry.getValue()) {
                    total += ((Number) item.getOrDefault("amount", 0)).doubleValue();
                }
            }
            return total;
        }
        
        public double getCategoryTotal(String category) {
            List<Map<String, Object>> items = data.get(category);
            if (items == null) return 0.0;
            return items.stream()
                .mapToDouble(item -> ((Number) item.getOrDefault("amount", 0)).doubleValue())
                .sum();
        }
    }
}
