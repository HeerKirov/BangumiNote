package com.heerkirov.bangumi.controller.ktml.data.tag

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TagDetailView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("标签 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "标签", link: "${proxyURL("web_data_tag_list")}"},
    {title: "详情", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var tag_rest = restful.newlist({url: "${proxyURL("api_content_tag")}"});
        var tag_type = {
            allowNull: true,
            foreignRequest: function(delegate) { tag_rest.request(null, function(success, status, data) { if(success)delegate(data.content) }) },
            foreignHeader: function(json) { return json.name },
            foreignValue: function(json) { return json.id },
            allowCustom: false,
            showContent: function (json) { return json.name },
            link: function(json) { return "${proxyURL("web_data_tag_detail")}" + json.id }
        };
        var rest = restful.newdetail({url: "${proxyURL("api_content_tag_detail", "id" to attrSafe("id"))}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "标签",
            deleteUrl: "${proxyURL("web_data_tag_list")}",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "标签", field: "name", type: "text", typeInfo: {length: 8, allowBlank: false}},
                {header: "描述", field: "description", type: "text", typeInfo: {length: 128, allowBlank: true, area: true}},
                "hr",
                {header: "父标签", field: "parent", type: "foreignChoice", typeInfo: tag_type},
                {header: "子标签", field: null, type: "constLink", typeInfo: {text: "所有子标签 >>", link: function(json){return "${proxyURL("web_data_tag_list")}?parent__eq="+json.id}}},
                "hr",
                {header: "番组", field: null, type: "constLink", typeInfo: {text: "所有标记有此标签的番组 >>", link: function(json){return "${proxyURL("web_data_anime_list")}?tag_id__eq="+json.id}}},
                {header: "番剧", field: null, type: "constLink", typeInfo: {text: "所有标记有此标签的番剧 >>", link: function(json){return "${proxyURL("web_data_bangumi_list")}?tag_id__eq="+json.id}}},
                "hr",
                {header: "条目创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改时间", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
})