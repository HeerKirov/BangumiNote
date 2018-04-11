package com.heerkirov.bangumi.controller.ktml.data.series

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SeriesDetailView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("系列 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "系列", link: "${proxyURL("web_data_series_list")}"},
    {title: "详情", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newdetail({url: "${proxyURL("api_content_series_detail", "id" to attrSafe("id"))}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "系列",
            deleteUrl: "${proxyURL("web_data_series_list")}",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
})