package com.heerkirov.bangumi.controller.ktml.data.company

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CompanyDetailView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("制作公司 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "制作公司", link: "${proxyURL("web_data_company_list")}"},
    {title: "详情", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newdetail({url: "${proxyURL("api_content_company_detail", "id" to attrSafe("id"))}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "制作公司",
            deleteUrl: "${proxyURL("web_data_company_list")}",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "原名", field: "origin_name", type: "text", typeInfo: {length: 32, allowBlank: true, allowNull: true}},
                "hr",
                {header: "番剧", field: null, type: "constLink", typeInfo: {text: "所有下属番剧 >>", link: function(json) {return "${proxyURL("web_data_bangumi_list")}?company_id__eq="+json.id}}},
                "hr",
                {header: "条目创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改时间", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
})