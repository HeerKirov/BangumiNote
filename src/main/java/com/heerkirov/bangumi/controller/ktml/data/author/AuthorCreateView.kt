package com.heerkirov.bangumi.controller.ktml.data.author

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AuthorCreateView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("新建作者 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "作者", link: "${proxyURL("web_data_author_list")}"},
    {title: "新建", link: "#"},
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newcreate({url: "${proxyURL("api_content_author")}"});
        build_create(${'$'}("#api-panel")).info({
            title: "作者",
            successUrl: "${proxyURL("web_data_author_list")}",
            content: [
                {header: "名字", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "原名", field: "origin_name", optionFlag: true, type: "text", typeInfo: {length: 32, allowBlank: true, allowNull: true}}
            ]
        }).rest(rest.request);
    """.trimIndent()))
})