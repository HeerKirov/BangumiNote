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
    })
    impl("SCRIPT", script_ { """

    """.trimIndent() })
})