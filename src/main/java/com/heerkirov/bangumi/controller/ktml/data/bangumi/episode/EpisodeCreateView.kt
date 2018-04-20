package com.heerkirov.bangumi.controller.ktml.data.bangumi.episode

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EpisodeCreateView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("新建单话 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "番剧", link: "${proxyURL("web_data_bangumi_list")}"},
    {title: "详情", link: "${proxyURL("web_data_bangumi_detail", "id" to attrSafe("parentId"))}"},
    {title: "单话", link: "${proxyURL("web_data_bangumi_episode_list", "parentId" to attrSafe("parentId"))}}"},
    {title: "新建", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newcreate({url: "${proxyURL("api_content_bangumi_episode", "parentId" to attrSafe("parentId"))}"});
        build_create(${'$'}("#api-panel")).info({
            title: "单话",
            successUrl: "${proxyURL("web_data_bangumi_episode_list", "parentId" to attrSafe("parentId"))}",
            content: [
                {header: "序列号", field: "serial", type: "number", typeInfo: {min: 1, arrowButton: true, allowBlank: false}},
                {header: "标题", field: "name", type: "text", typeInfo: {length: 128, allowBlank: false}},
                "hr",
                {header: "发布时间", field: "publish_time", optionFlag: true, type: "datetime", typeInfo: {allowNull: true, format: "yyyy-mm-dd", view: "day"}},
                {header: "看完时间", field: "finished_time", optionFlag: true, type: "datetime", typeInfo: {allowNull: true, format: "yyyy-mm-dd", view: "day"}},
            ]
        }).rest(rest.request);
    """.trimIndent()))
})