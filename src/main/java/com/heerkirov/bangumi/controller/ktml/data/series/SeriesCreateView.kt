package com.heerkirov.bangumi.controller.ktml.data.series

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SeriesCreateView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("新建系列 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "系列", link: "${proxyURL("web_data_series_list")}"},
    {title: "新建", link: "#"},
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newcreate({url: "${proxyURL("api_content_series")}"});
        build_create(${'$'}("#api-panel")).info({
            title: "系列",
            successUrl: "${proxyURL("web_data_series_list")}",
            content: [
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}}
            ]
        }).rest(rest.request);
    """.trimIndent()))
})