package com.heerkirov.bangumi.controller.ktml.home

import com.heerkirov.bangumi.controller.ktml.StdBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlCacheView
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.div
import com.heerkirov.ktml.element.script_
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class HomeView(@Autowired p: ConstProxy): HtmlView(StdBasic::class, p, {
    impl("TITLE", text(proxyStr("val_logo")))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-4")
        div(clazz = "row") {
            text("Welcome!")
        }
        div(clazz = "row") {
            div(clazz = "col-12 col-md-2 p-4 m-sm-2 mb-2 bg-light", id = "nav_list")
            div(clazz = "col-12 col-md p-4 m-sm-2 bg-light", id = "api_panel")
        }
    })
    impl("SCRIPT", text("""
        build_navlist(${'$'}("#nav_list"), [
            {title: "日记", link: "${proxyURL("web_diary")}"},
            {title: "数据库", link: "${proxyURL("web_data")}"},
            {title: "&nbsp;&nbsp;&nbsp;&nbsp;系列", link: "${proxyURL("web_data_series_list")}"},
            {title: "&nbsp;&nbsp;&nbsp;&nbsp;番组", link: "${proxyURL("web_data_anime_list")}"},
            {title: "&nbsp;&nbsp;&nbsp;&nbsp;番剧", link: "${proxyURL("web_data_bangumi_list")}"},
            {title: "统计", link: "${proxyURL("web_statistics")}"}
        ]);
        var weekdayRequest = restful.newlist({url: "${proxyURL("api_diary")}"});
        var table = diary.weekdayTable(weekdayRequest);
        ${'$'}("#api_panel").append(table);
    """.trimIndent()))
})