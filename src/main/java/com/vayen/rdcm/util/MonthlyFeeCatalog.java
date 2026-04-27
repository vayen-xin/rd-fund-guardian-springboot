package com.vayen.rdcm.util;

import com.vayen.rdcm.dto.MonthlyFeeSchemaResponse;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@UtilityClass
public class MonthlyFeeCatalog {

    private static final List<CategoryDef> CATEGORY_DEFS = List.of(
            new CategoryDef("labor", "人员人工费用", List.of(
                    new ItemDef("salary", "直接从事研发活动人员的工资薪金"),
                    new ItemDef("five_social_housing", "直接从事研发活动人员的五险一金"),
                    new ItemDef("external_labor", "外聘研发人员的劳务费用"),
                    new ItemDef("equity_incentive", "从事研发活动人员的股权激励支出")
            )),
            new CategoryDef("direct", "直接投入费用", List.of(
                    new ItemDef("material", "研发活动直接消耗的材料费用"),
                    new ItemDef("fuel", "研发活动直接消耗的燃料费用"),
                    new ItemDef("power", "研发活动直接消耗的动力费用"),
                    new ItemDef("mold_and_process", "用于中间试验和产品试制的模具、工艺装备开发及制造费"),
                    new ItemDef("sample_and_test", "不构成固定资产的样品、样机及一般测试手段购置费"),
                    new ItemDef("inspection", "试制产品的检验费"),
                    new ItemDef("maintenance", "用于研发活动的仪器、设备的运行维护、调整、检验、维修等费用"),
                    new ItemDef("equipment_lease", "通过经营租赁方式租入的用于研发活动的仪器、设备租赁费"),
                    new ItemDef("building_lease", "通过经营租赁方式租入的用于研发活动的在用建筑物租赁费")
            )),
            new CategoryDef("deprec", "折旧费用", List.of(
                    new ItemDef("instrument_depreciation", "用于研发活动的仪器的折旧费"),
                    new ItemDef("equipment_depreciation", "用于研发活动的设备的折旧费"),
                    new ItemDef("building_depreciation", "用于研发活动的在用建筑物的折旧费")
            )),
            new CategoryDef("long_deferred", "长期待摊费用", List.of(
                    new ItemDef("renovation_amortization", "研发设施改建、改装、装修和修理过程中发生的长期待摊费用")
            )),
            new CategoryDef("intangible", "无形资产摊销", List.of(
                    new ItemDef("software", "用于研发活动的软件的摊销费用"),
                    new ItemDef("patent", "用于研发活动的专利权的摊销费用"),
                    new ItemDef("non_patent", "用于研发活动的非专利技术的摊销费用")
            )),
            new CategoryDef("design", "新产品设计费等", List.of(
                    new ItemDef("product_design", "新产品设计费"),
                    new ItemDef("process_spec", "新工艺规程制定费"),
                    new ItemDef("clinical_trial", "新药研制的临床试验费"),
                    new ItemDef("field_test", "勘探开发技术的现场试验费")
            )),
            new CategoryDef("equip", "装备调试费用", List.of(
                    new ItemDef("special_machine", "研制特殊、专用的生产机器"),
                    new ItemDef("field_trial", "田间试验费")
            )),
            new CategoryDef("other", "其他相关费用", List.of(
                    new ItemDef("technical_docs", "技术图书资料费、资料翻译费、专家咨询费、高新科技研发保险费"),
                    new ItemDef("achievement_review", "研发成果的检索、分析、评议、论证、鉴定、评审、评估、验收费用"),
                    new ItemDef("ip_service", "知识产权的申请费、注册费、代理费"),
                    new ItemDef("welfare_insurance", "职工福利费、补充养老保险费、补充医疗保险费(不可计入高新可计入加计扣除)"),
                    new ItemDef("travel_meeting", "差旅费、会议费"),
                    new ItemDef("communication", "通讯费等(可计入高新不可加计扣除)"),
                    new ItemDef("commercial_insurance", "商业保险(不可计入高新和加计扣除)"),
                    new ItemDef("severance", "辞退福利(不可计入高新和加计扣除)"),
                    new ItemDef("courier", "快递运输费用(不可计入高新和加计扣除)"),
                    new ItemDef("labor_protection", "劳动保护费用(不可计入高新和加计扣除)"),
                    new ItemDef("office", "办公费用(不可计入高新和加计扣除)"),
                    new ItemDef("entertainment", "招待费用(不可计入高新和加计扣除)"),
                    new ItemDef("training", "培训费用(不可计入高新和加计扣除)"),
                    new ItemDef("city_transport", "市内交通费用(不可计入高新和加计扣除)"),
                    new ItemDef("vehicle", "汽车相关费用(不可计入高新和加计扣除)"),
                    new ItemDef("network_cloud", "网络宽带云服务费用(不可计入高新和加计扣除)"),
                    new ItemDef("property_service", "物业服务费用(不可计入高新和加计扣除)"),
                    new ItemDef("other_non_deductible", "其他费用(不可计入高新和加计扣除)")
            )),
            new CategoryDef("outsource", "委托研发费用", List.of(
                    new ItemDef("entrusted_domestic", "委托境内机构或个人进行研发活动所发生的费用"),
                    new ItemDef("entrusted_overseas_org", "委托境外机构进行研发活动所发生的费用"),
                    new ItemDef("entrusted_overseas_individual", "委托境外个人进行研发活动所发生的费用")
            ))
    );

    private static final Map<String, CategoryDef> CATEGORY_INDEX = buildCategoryIndex();
    private static final Map<String, Map<String, ItemDef>> ITEM_INDEX = buildItemIndex();

    public static List<String> categoryCodes() {
        return CATEGORY_DEFS.stream().map(CategoryDef::code).toList();
    }

    public static String categoryLabel(String categoryCode) {
        CategoryDef category = CATEGORY_INDEX.get(categoryCode);
        return category == null ? categoryCode : category.label();
    }

    public static String itemLabel(String categoryCode, String itemCode) {
        ItemDef item = ITEM_INDEX.getOrDefault(categoryCode, Map.of()).get(itemCode);
        return item == null ? itemCode : item.label();
    }

    public static boolean isIncludedInScope(String itemLabel, String scopeCode) {
        if (itemLabel == null || itemLabel.isBlank() || scopeCode == null || scopeCode.isBlank()) {
            return true;
        }
        return switch (scopeCode) {
            case "deduction" -> !itemLabel.contains("不可加计扣除");
            case "hightech" -> !itemLabel.contains("不可计入高新");
            default -> true;
        };
    }

    public static String resolveCategoryCode(String rawCategoryCode, String rawItemCode, String rawItemLabel) {
        String categoryCode = switch (safe(rawCategoryCode)) {
            case "direct_material", "direct_fuel", "direct_rental", "direct" -> "direct";
            case "depreciation", "deprec" -> "deprec";
            case "long_deferred" -> "long_deferred";
            case "amortization", "intangible" -> "intangible";
            case "commissioning", "equip" -> "equip";
            case "outsourced", "outsource" -> "outsource";
            default -> safe(rawCategoryCode);
        };
        String itemLabel = safe(rawItemLabel);
        String itemCode = safe(rawItemCode);

        if ("deprec".equals(categoryCode) && ("renovation_amortization".equals(itemCode) || containsAny(itemLabel, "长期待摊", "装修费摊销"))) {
            return "long_deferred";
        }
        if (CATEGORY_INDEX.containsKey(categoryCode)) {
            return categoryCode;
        }
        return "other";
    }

    public static String resolveItemCode(String categoryCode, String rawItemCode, String rawItemLabel) {
        String itemCode = safe(rawItemCode);
        if (!itemCode.isBlank() && ITEM_INDEX.getOrDefault(categoryCode, Map.of()).containsKey(itemCode)) {
            return itemCode;
        }

        itemCode = switch (itemCode) {
            case "direct_material" -> "material";
            case "direct_fuel" -> "fuel";
            case "direct_rental" -> "equipment_lease";
            case "commissioning" -> "special_machine";
            case "outsourced" -> "entrusted_domestic";
            case "amortization" -> "software";
            case "depreciation" -> "instrument_depreciation";
            case "long_deferred" -> "renovation_amortization";
            default -> itemCode;
        };
        if (!itemCode.isBlank() && ITEM_INDEX.getOrDefault(categoryCode, Map.of()).containsKey(itemCode)) {
            return itemCode;
        }

        String label = safe(rawItemLabel);
        if (label.isBlank()) {
            return itemCode;
        }
        return switch (categoryCode) {
            case "labor" -> matchLaborItem(label);
            case "direct" -> matchDirectItem(label);
            case "deprec" -> matchDepreciationItem(label);
            case "long_deferred" -> "renovation_amortization";
            case "intangible" -> matchIntangibleItem(label);
            case "design" -> matchDesignItem(label);
            case "equip" -> containsAny(label, "田间") ? "field_trial" : "special_machine";
            case "other" -> matchOtherItem(label);
            case "outsource" -> matchOutsourceItem(label);
            default -> itemCode;
        };
    }

    public static MonthlyFeeSchemaResponse buildResponse() {
        MonthlyFeeSchemaResponse response = new MonthlyFeeSchemaResponse();
        response.setCategories(CATEGORY_DEFS.stream().map(MonthlyFeeCatalog::toResponse).toList());
        return response;
    }

    public static Map<String, List<Map<String, Object>>> emptyData() {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        categoryCodes().forEach(category -> data.put(category, new ArrayList<>()));
        return data;
    }

    private static MonthlyFeeSchemaResponse.FeeCategory toResponse(CategoryDef source) {
        MonthlyFeeSchemaResponse.FeeCategory category = new MonthlyFeeSchemaResponse.FeeCategory();
        category.setCode(source.code());
        category.setLabel(source.label());
        category.setItems(source.items().stream().map(MonthlyFeeCatalog::toResponse).toList());
        return category;
    }

    private static MonthlyFeeSchemaResponse.FeeSubItem toResponse(ItemDef source) {
        MonthlyFeeSchemaResponse.FeeSubItem item = new MonthlyFeeSchemaResponse.FeeSubItem();
        item.setCode(source.code());
        item.setLabel(source.label());
        return item;
    }

    private static Map<String, CategoryDef> buildCategoryIndex() {
        Map<String, CategoryDef> index = new LinkedHashMap<>();
        CATEGORY_DEFS.forEach(category -> index.put(category.code(), category));
        return index;
    }

    private static Map<String, Map<String, ItemDef>> buildItemIndex() {
        Map<String, Map<String, ItemDef>> index = new LinkedHashMap<>();
        CATEGORY_DEFS.forEach(category -> {
            Map<String, ItemDef> itemMap = new LinkedHashMap<>();
            category.items().forEach(item -> itemMap.put(item.code(), item));
            index.put(category.code(), itemMap);
        });
        return index;
    }

    private static String matchLaborItem(String label) {
        if (containsAny(label, "五险一金", "医疗保险", "养老保险", "失业保险", "工伤保险", "生育保险", "住房公积金")) {
            return "five_social_housing";
        }
        if (containsAny(label, "劳务")) {
            return "external_labor";
        }
        if (containsAny(label, "股权激励")) {
            return "equity_incentive";
        }
        return "salary";
    }

    private static String matchDirectItem(String label) {
        if (containsAny(label, "燃料")) {
            return "fuel";
        }
        if (containsAny(label, "动力")) {
            return "power";
        }
        if (containsAny(label, "模具", "工艺装备")) {
            return "mold_and_process";
        }
        if (containsAny(label, "样品", "样机", "测试手段")) {
            return "sample_and_test";
        }
        if (containsAny(label, "检验")) {
            return "inspection";
        }
        if (containsAny(label, "运行维护", "维护", "维修", "调整")) {
            return "maintenance";
        }
        if (containsAny(label, "房屋租赁", "建筑物租赁")) {
            return "building_lease";
        }
        if (containsAny(label, "设备租赁", "仪器、设备租赁")) {
            return "equipment_lease";
        }
        return "material";
    }

    private static String matchDepreciationItem(String label) {
        if (containsAny(label, "建筑物", "房屋折旧")) {
            return "building_depreciation";
        }
        if (containsAny(label, "设备")) {
            return "equipment_depreciation";
        }
        return "instrument_depreciation";
    }

    private static String matchIntangibleItem(String label) {
        if (containsAny(label, "非专利")) {
            return "non_patent";
        }
        if (containsAny(label, "专利")) {
            return "patent";
        }
        return "software";
    }

    private static String matchDesignItem(String label) {
        if (containsAny(label, "工艺规程")) {
            return "process_spec";
        }
        if (containsAny(label, "临床试验")) {
            return "clinical_trial";
        }
        if (containsAny(label, "现场试验")) {
            return "field_test";
        }
        return "product_design";
    }

    private static String matchOtherItem(String label) {
        if (containsAny(label, "成果", "评议", "论证", "验收")) {
            return "achievement_review";
        }
        if (containsAny(label, "知识产权", "申请费", "注册费", "代理费")) {
            return "ip_service";
        }
        if (containsAny(label, "福利", "补充养老", "补充医疗")) {
            return "welfare_insurance";
        }
        if (containsAny(label, "差旅", "会议")) {
            return "travel_meeting";
        }
        if (containsAny(label, "通讯")) {
            return "communication";
        }
        if (containsAny(label, "商业保险", "意外险")) {
            return "commercial_insurance";
        }
        if (containsAny(label, "辞退福利")) {
            return "severance";
        }
        if (containsAny(label, "快递", "运输")) {
            return "courier";
        }
        if (containsAny(label, "劳动保护")) {
            return "labor_protection";
        }
        if (containsAny(label, "办公")) {
            return "office";
        }
        if (containsAny(label, "招待")) {
            return "entertainment";
        }
        if (containsAny(label, "培训")) {
            return "training";
        }
        if (containsAny(label, "市内交通")) {
            return "city_transport";
        }
        if (containsAny(label, "汽车")) {
            return "vehicle";
        }
        if (containsAny(label, "网络", "宽带", "云服务")) {
            return "network_cloud";
        }
        if (containsAny(label, "物业")) {
            return "property_service";
        }
        if (containsAny(label, "技术图书资料", "资料翻译", "专家咨询", "研发保险")) {
            return "technical_docs";
        }
        return "other_non_deductible";
    }

    private static String matchOutsourceItem(String label) {
        if (containsAny(label, "境外机构")) {
            return "entrusted_overseas_org";
        }
        if (containsAny(label, "境外个人")) {
            return "entrusted_overseas_individual";
        }
        return "entrusted_domestic";
    }

    private static boolean containsAny(String source, String... keywords) {
        String value = safe(source).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (value.contains(Objects.requireNonNullElse(keyword, "").toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CategoryDef(String code, String label, List<ItemDef> items) {}

    private record ItemDef(String code, String label) {}
}
