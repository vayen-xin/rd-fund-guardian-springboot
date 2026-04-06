package com.vayen.rdcm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, String> LEGACY_CATEGORY_MAPPING = Map.of(
            "direct_material", "direct",
            "direct_fuel", "direct",
            "direct_rental", "direct",
            "depreciation", "deprec",
            "amortization", "intangible",
            "commissioning", "equip",
            "outsourced", "outsource"
    );

    /**
     * 统一解析月度费用 JSON。
     * 这里兼容两套结构：
     * 1. 旧后端结构
     * 2. 新前端 8 类费用结构
     */
    public static CostData parseCostData(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new CostData(emptyData());
            }
            Map<String, Object> root = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return new CostData(normalize(root));
        } catch (JsonProcessingException e) {
            log.error("解析 JSON 失败: {}", json, e);
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    /**
     * 继承上月数据时，把金额字段清零，但保留条目结构。
     */
    public static String zeroOutAmounts(String json) {
        try {
            if (json == null || json.isBlank()) {
                return MAPPER.writeValueAsString(emptyData());
            }
            Map<String, Object> root = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, List<Map<String, Object>>> normalized = normalize(root);
            normalized.values().forEach(items -> items.forEach(JsonUtils::zeroAmount));
            return MAPPER.writeValueAsString(toFrontendShape(normalized));
        } catch (JsonProcessingException e) {
            log.error("JSON 处理失败: {}", json, e);
            throw new RuntimeException("JSON 处理失败", e);
        }
    }

    public static double calculateGrandTotal(String json) {
        return parseCostData(json).calculateTotal();
    }

    /**
     * 把任意兼容结构统一转成前端更容易消费的 fees 结构。
     */
    public static Map<String, Object> toFrontendFees(String json) {
        return toFrontendShape(parseCostData(json).getData());
    }

    public static class CostData {
        private final Map<String, List<Map<String, Object>>> data;

        public CostData(Map<String, List<Map<String, Object>>> data) {
            this.data = data;
        }

        public Map<String, List<Map<String, Object>>> getData() {
            return data;
        }

        public List<Map<String, Object>> getItems(String category) {
            return data.getOrDefault(category, List.of());
        }

        public double calculateTotal() {
            return data.values().stream()
                    .flatMap(List::stream)
                    .mapToDouble(JsonUtils::readAmount)
                    .sum();
        }

        public double getCategoryTotal(String category) {
            return getItems(category).stream()
                    .mapToDouble(JsonUtils::readAmount)
                    .sum();
        }
    }

    /**
     * 自动识别当前 JSON 属于“新前端结构”还是“旧后端结构”。
     */
    private static Map<String, List<Map<String, Object>>> normalize(Map<String, Object> root) {
        if (root.isEmpty()) {
            return emptyData();
        }
        Object firstValue = root.values().iterator().next();
        if (firstValue instanceof Map<?, ?>) {
            return normalizeFrontendShape(root);
        }
        return normalizeLegacyShape(root);
    }

    /**
     * 把新前端 fees 结构归一化成统一内部结构，便于后端计算总金额。
     */
    private static Map<String, List<Map<String, Object>>> normalizeFrontendShape(Map<String, Object> root) {
        Map<String, List<Map<String, Object>>> normalized = emptyData();
        for (String category : MonthlyFeeCatalog.categoryCodes()) {
            Object categoryObj = root.get(category);
            if (!(categoryObj instanceof Map<?, ?> categoryMap)) {
                continue;
            }
            List<Map<String, Object>> merged = new ArrayList<>();
            merged.addAll(copyItems(category, categoryMap.get("systemItems"), "system"));
            merged.addAll(copyItems(category, categoryMap.get("manualItems"), "manual"));
            normalized.put(category, merged);
        }
        return normalized;
    }

    /**
     * 把旧后端结构归一化，并映射到新的 8 类费用编码。
     */
    private static Map<String, List<Map<String, Object>>> normalizeLegacyShape(Map<String, Object> root) {
        Map<String, List<Map<String, Object>>> normalized = emptyData();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String category = LEGACY_CATEGORY_MAPPING.getOrDefault(entry.getKey(), entry.getKey());
            List<Map<String, Object>> items = copyItems(category, entry.getValue(), "legacy");
            normalized.computeIfAbsent(category, key -> new ArrayList<>()).addAll(items);
        }
        return normalized;
    }

    private static List<Map<String, Object>> copyItems(String categoryCode, Object rawItems, String sourceType) {
        List<Map<String, Object>> copied = new ArrayList<>();
        if (!(rawItems instanceof List<?> list)) {
            return copied;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> itemMap) {
                Map<String, Object> copiedItem = new LinkedHashMap<>();
                itemMap.forEach((key, value) -> copiedItem.put(String.valueOf(key), value));
                copiedItem.putIfAbsent("sourceType", sourceType);
                copiedItem.putIfAbsent("categoryCode", categoryCode);
                copiedItem.putIfAbsent("categoryLabel", MonthlyFeeCatalog.categoryLabel(categoryCode));
                Object itemCode = copiedItem.get("itemCode");
                if (itemCode instanceof String code && !code.isBlank()) {
                    copiedItem.putIfAbsent("itemLabel", MonthlyFeeCatalog.itemLabel(categoryCode, code));
                }
                if (!copiedItem.containsKey("label") && copiedItem.get("itemLabel") instanceof String itemLabel) {
                    copiedItem.put("label", itemLabel);
                }
                copied.add(copiedItem);
            }
        }
        return copied;
    }

    private static Map<String, List<Map<String, Object>>> emptyData() {
        return MonthlyFeeCatalog.emptyData();
    }

    /**
     * 把统一内部结构再转回前端更好消费的 fees 形状。
     */
    private static Map<String, Object> toFrontendShape(Map<String, List<Map<String, Object>>> normalized) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String category : MonthlyFeeCatalog.categoryCodes()) {
            Map<String, Object> categoryValue = new LinkedHashMap<>();
            List<Map<String, Object>> systemItems = new ArrayList<>();
            List<Map<String, Object>> manualItems = new ArrayList<>();
            for (Map<String, Object> item : normalized.getOrDefault(category, List.of())) {
                String sourceType = String.valueOf(item.getOrDefault("sourceType", "manual"));
                Map<String, Object> copiedItem = new LinkedHashMap<>(item);
                copiedItem.remove("sourceType");
                if ("system".equals(sourceType)) {
                    systemItems.add(copiedItem);
                } else {
                    manualItems.add(copiedItem);
                }
            }
            categoryValue.put("systemItems", systemItems);
            categoryValue.put("manualItems", manualItems);
            result.put(category, categoryValue);
        }
        return result;
    }

    private static void zeroAmount(Map<String, Object> item) {
        item.put("amount", 0.0);
        if (item.containsKey("voucherIds")) {
            item.put("voucherIds", new ArrayList<>());
        }
        if (item.containsKey("vouchers")) {
            item.put("vouchers", new ArrayList<>());
        }
        for (Map.Entry<String, Object> entry : new ArrayList<>(item.entrySet())) {
            if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                Map<String, Object> copied = new LinkedHashMap<>();
                nestedMap.forEach((key, value) -> copied.put(String.valueOf(key), value));
                copied.put("amount", 0.0);
                item.put(entry.getKey(), copied);
            }
        }
    }

    private static double readAmount(Map<String, Object> item) {
        Object amount = item.get("amount");
        if (amount instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
