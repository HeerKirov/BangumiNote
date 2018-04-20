package com.heerkirov.bangumi.controller.ktml.data.bangumi.episode

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EpisodeListView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("单话 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "番剧", link: "${proxyURL("web_data_bangumi_list")}"},
    {title: "详情", link: "${proxyURL("web_data_bangumi_detail", "id" to attrSafe("parentId"))}"},
    {title: "单话", link: "#"}""".trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newlist({url: "${proxyURL("api_content_bangumi_episode", "parentId" to attrSafe("parentId"))}"}).appendparams(requestparams);
        build_list(${'$'}("#api-panel")).info({
            title: "单话",
            createUrl: "${proxyURL("web_data_bangumi_episode_create", "parentId" to attrSafe("parentId"))}",
            visibleControl: true,
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "${proxyURL("web_data_bangumi_episode_detail", "parentId" to attrSafe("parentId"))}" + i.id}, visible: false},
                {header: "序列号", field: "serial", sortable: true},
                {header: "标题", field: "name", sortable: true, link: function (i) {return "${proxyURL("web_data_bangumi_episode_detail", "parentId" to attrSafe("parentId"))}" + i.id}},

                {header: "发布时间", field: "publish_time", sortable: true, type: "datetime"},
                {header: "看完时间", field: "finished_time", sortable: true, type: "datetime"},

                {header: "条目创建时间", field: "create_time", sortable: true, type: "datetime"},
                {header: "最后修改时间", field: "update_time", sortable: true, type: "datetime", visible: false}
            ]
        }).rest(rest.request).build();
    """.trimIndent()))
})