package com.heerkirov.bangumi.controller.ktml.diary

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
class DiaryView(@Autowired p: ConstProxy): HtmlView(StdBasic::class, p, {
    impl("TITLE", text("日记 - ${proxyStr("val_logo")}"))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-2")
        div(clazz = "row") {
            div(clazz = "col p-5 bg-light", id = "api-panel")
        }
    })
    impl("SCRIPT", script_ { """
        var restDetail = restful.newlist({url: "${proxyURL("api_diary")}"});
        var root = diary.root(restDetail, function(json) {return restful.newdetail({url: "${proxyURL("api_diary_detail", "id" to "\"+json.id+\"")}"})});
        $("#api-panel").append(root.obj);
    """.trimIndent() })
})