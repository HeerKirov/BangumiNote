package com.heerkirov.bangumi.controller.ktml.data.series

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SeriesListView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("系列 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "系列", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newlist({url: "${proxyURL("api_content_series")}"}).appendparams(requestparams);
        build_list(${'$'}("#api-panel")).info({
            title: "系列",
            createUrl: "${proxyURL("web_data_series_create")}",
            visibleControl: true,
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "${proxyURL("web_data_series_detail")}" + i.id}},
                {header: "名称", field: "name", sortable: true, link: function (i) {return "${proxyURL("web_data_series_detail")}" + i.id}},
                {header: "条目创建时间", field: "create_time", sortable: true, type: "datetime"},
                {header: "最后修改时间", field: "update_time", sortable: true, type: "datetime", visible: false}
            ]
        }).rest(rest.request).build();
    """.trimIndent()))
})