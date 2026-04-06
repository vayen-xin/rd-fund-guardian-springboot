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
                    new ItemDef("pension", "基本养老保险费"),
                    new ItemDef("medical", "基本医疗保险费"),
                    new ItemDef("unemployment", "失业保险费"),
                    new ItemDef("injury", "工伤保险费"),
                    new ItemDef("maternity", "生育保险费"),
                    new ItemDef("housing_fund", "住房公积金"),
                    new ItemDef("external_labor", "外聘科技人员劳务费")
            )),
            new CategoryDef("direct", "直接投入费用", List.of(
                    new ItemDef("material", "直接消耗材料费"),
                    new ItemDef("fuel_power", "燃料和动力费用"),
                    new ItemDef("mold_and_process", "模具及工艺装备开发制造费"),
                    new ItemDef("sample_and_test", "样品样机及测试手段购置费"),
                    new ItemDef("inspection", "试制产品检验费"),
                    new ItemDef("maintenance", "仪器设备运行维护费"),
                    new ItemDef("lease", "研发固定资产租赁费")
            )),
            new CategoryDef("deprec", "折旧费用与长期待摊费用", List.of(
                    new ItemDef("equipment_depreciation", "仪器设备折旧费"),
                    new ItemDef("building_depreciation", "在用建筑物折旧费"),
                    new ItemDef("long_term_deferred", "长期待摊费用")
            )),
            new CategoryDef("intangible", "无形资产摊销费用", List.of(
                    new ItemDef("software", "软件摊销费"),
                    new ItemDef("ip", "知识产权摊销费"),
                    new ItemDef("non_patent", "非专利技术摊销费")
            )),
            new CategoryDef("design", "设计费用", List.of(
                    new ItemDef("design_cost", "设计费用")
            )),
            new CategoryDef("equip", "装备调试费用与试验费用", List.of(
                    new ItemDef("debugging", "装备调试费用"),
                    new ItemDef("testing", "试验费用")
            )),
            new CategoryDef("outsource", "委托外部研究开发费用", List.of(
                    new ItemDef("outsourced_rnd", "委托外部研究开发费用")
            )),
            new CategoryDef("other", "其他费用", List.of(
                    new ItemDef("books", "技术图书资料费"),
                    new ItemDef("translation", "资料翻译费"),
                    new ItemDef("consulting", "专家咨询费"),
                    new ItemDef("insurance", "高新科技研发保险费"),
                    new ItemDef("evaluation", "研发成果检索论证评审鉴定验收费"),
                    new ItemDef("ip_service", "知识产权申请注册代理费"),
                    new ItemDef("meeting", "会议费"),
                    new ItemDef("travel", "差旅费"),
                    new ItemDef("communication", "通讯费"),
                    new ItemDef("other_misc", "其他直接相关费用")
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
