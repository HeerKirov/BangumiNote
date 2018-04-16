package com.heerkirov.bangumi.controller.ktml.data.tag

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TagCreateView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("新建标签 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "标签", link: "${proxyURL("web_data_tag_list")}"},
    {title: "新建", link: "#"},
    """.trimIndent()))
    impl("SCRIPT", text("""
        var tag_rest = restful.newlist({url: "${proxyURL("api_content_tag")}"});
        var tag_type_info = {
            allowNull: true,
            foreignRequest: function(delegate) { tag_rest.request(null, function(success, status, data) { if(success)delegate(data.content) }) },
            foreignHeader: function(json) { return json.name },
            foreignValue: function(json) { return json.id },
            allowCustom: false
        };
        var rest = restful.newcreate({url: "${proxyURL("api_content_tag")}"});
        build_create(${'$'}("#api-panel")).info({
            title: "标签",
            successUrl: "${proxyURL("web_data_tag_list")}",
            content: [
                {header: "标签", field: "name", type: "text", typeInfo: {length: 8, allowBlank: false}},
                {header: "描述", field: "description", optionFlag: true, type: "text", typeInfo: {length: 128, allowBlank: true}},
                "hr",
                {header: "父标签", field: "parent", optionFlag: true, type: "foreignChoice", typeInfo: tag_type_info},
            ]
        }).rest(rest.request);
    """.trimIndent()))
})