package com.heerkirov.bangumi.controller.ktml.data.tag

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TagListView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("标签 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "标签", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newlist({url: "${proxyURL("api_content_tag")}"}).appendparams(requestparams);
        build_list(${'$'}("#api-panel")).info({
            title: "标签",
            createUrl: "${proxyURL("web_data_tag_create")}",
            visibleControl: true,
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "${proxyURL("web_data_tag_detail")}" + i.id}},
                {header: "标签", field: "name", sortable: true, link: function (i) {return "${proxyURL("web_data_tag_detail")}" + i.id}},
                {header: "描述", field: "description", visible: false},
                {header: "条目创建时间", field: "create_time", sortable: true, type: "datetime"},
                {header: "最后修改时间", field: "update_time", sortable: true, type: "datetime", visible: false}
            ]
        }).rest(rest.request).build();
    """.trimIndent()))
})