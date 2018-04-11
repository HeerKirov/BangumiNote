package com.heerkirov.bangumi.controller.ktml.data.author

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AuthorDetailView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("作者 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "作者", link: "${proxyURL("web_data_author_list")}"},
    {title: "详情", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newdetail({url: "${proxyURL("api_content_author_detail", "id" to attrSafe("id"))}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "作者",
            deleteUrl: "${proxyURL("web_data_author_list")}",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "名字", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "原名", field: "origin_name", type: "text", typeInfo: {length: 32, allowBlank: true, allowNull: true}},
                {header: "创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
})