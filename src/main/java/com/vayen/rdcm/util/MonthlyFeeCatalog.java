package com.vayen.rdcm.util;

import com.vayen.rdcm.dto.MonthlyFeeSchemaResponse;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class MonthlyFeeCatalog {

    private static final List<CategoryDef> CATEGORY_DEFS = List.of(
            new CategoryDef("labor", "人员人工费用", List.of(
                    new ItemDef("salary", "工资薪金"),
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
            new CategoryDef("deprec", "折旧费用与长期待摊费用", List.of(
                    new ItemDef("instrument_depreciation", "用于研发活动的仪器的折旧费"),
                    new ItemDef("equipment_depreciation", "用于研发活动的设备的折旧费"),
                    new ItemDef("building_depreciation", "用于研发活动的在用建筑物的折旧费"),
                    new ItemDef("renovation_amortization", "研发设施改建、改装、装修和修理过程中发生的长期待摊费用")
            )),
            new CategoryDef("intangible", "无形资产摊销费用", List.of(
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
            new CategoryDef("equip", "装备调试费用与试验费用", List.of(
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

    public static List<String> categoryCodes() {
        return CATEGORY_DEFS.stream().map(CategoryDef::code).toList();
    }

    public static String categoryLabel(String categoryCode) {
        return CATEGORY_DEFS.stream()
                .filter(item -> item.code().equals(categoryCode))
                .map(CategoryDef::label)
                .findFirst()
                .orElse(categoryCode);
    }

    public static String itemLabel(String categoryCode, String itemCode) {
        return CATEGORY_DEFS.stream()
                .filter(item -> item.code().equals(categoryCode))
                .flatMap(item -> item.items().stream())
                .filter(item -> item.code().equals(itemCode))
                .map(ItemDef::label)
                .findFirst()
                .orElse(itemCode);
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

    private record CategoryDef(String code, String label, List<ItemDef> items) {}

    private record ItemDef(String code, String label) {}
}
