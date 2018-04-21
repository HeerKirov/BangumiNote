package com.heerkirov.bangumi.controller.ktml.statistics

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
class StatisticsNavView(@Autowired p: ConstProxy): HtmlView(StdBasic::class, p, {
    impl("TITLE", text(proxyStr("val_logo")))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-4")
        div(clazz = "row") {
            div(clazz = "col-12 col-md-2 p-4 m-sm-2 mb-2 bg-light", id = "nav_list")
            div(clazz = "col-12 col-md p-4 m-sm-2 bg-light", id = "api_panel") {
                div(clazz = "alert alert-info") {
                    text("该功能尚未开发。")
                }
            }
        }
    })
    impl("SCRIPT", text("""
        build_navlist(${'$'}("#nav_list"), []);

    """.trimIndent()))
})