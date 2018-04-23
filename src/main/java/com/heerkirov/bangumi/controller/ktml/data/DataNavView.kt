package com.heerkirov.bangumi.controller.ktml.data

import com.heerkirov.bangumi.controller.ktml.StdBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DataNavView(@Autowired p: ConstProxy): HtmlView(StdBasic::class, p, {
    impl("TITLE", text("数据库 - ${proxyStr("val_logo")}"))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row") {
            div(clazz = "col-sm-1")
            div(clazz = "col-sm-10") {
                div(clazz = "row m-2", id = "nav-bar")
                div(clazz = "row p-5 bg-light") {
                    div(clazz = "col") {
                        div(clazz = "row mb-4") {
                            h2{text("数据库")}
                        }
                        div(clazz = "row list-group") {
                            a_(href = proxyURL("web_data_series_list"), clazz = "list-group-item list-group-item-action") {"系列"}
                            a_(href = proxyURL("web_data_anime_list"), clazz = "list-group-item list-group-item-action") {"番组"}
                            a_(href = proxyURL("web_data_bangumi_list"), clazz = "list-group-item list-group-item-action") {"番剧"}
                            a_(href = proxyURL("web_data_tag_list"), clazz = "list-group-item list-group-item-action") {"标签"}
                            a_(href = proxyURL("web_data_author_list"), clazz = "list-group-item list-group-item-action") {"作者"}
                            a_(href = proxyURL("web_data_company_list"), clazz = "list-group-item list-group-item-action") {"制作公司"}

                        }
                    }
                }
            }
            div(clazz = "col-sm-1")
        }
    })
    impl("SCRIPT", text("""
        ${'$'}(document).ready(function () {
        build_navbar(${'$'}("#nav-bar"), [
            {title: "数据库", link: "${proxyURL("web_data")}"},
        ]);
    })
    """.trimIndent()))
})