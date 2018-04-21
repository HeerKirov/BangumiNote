package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.bangumi.controller.ktml.StdBasic
import com.heerkirov.ktml.builder.Block
import com.heerkirov.ktml.builder.block
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.div
import com.heerkirov.ktml.element.script
import com.heerkirov.ktml.element.script_
import com.heerkirov.ktml.element.text

/**再一级的实现页面。实现了标准API web页面的所需框架。
 * 提供了左侧导航栏实现区、右侧功能栏实现区、中间api面板、上面的层级导航栏。
 * 下级需要做的实现：TITLE NAV_LIST* NAV_BAR* SCRIPT* TOOL*
 */
class WideTemplateBasic : HtmlView(StdBasic::class, {
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-2") {
            div(clazz = "col", id = "nav-bar")
        }
        div(clazz = "row") {
            div(clazz = "col p-5 bg-light", id = "api-panel")
        }
    })
    impl("SCRIPT",
        text("""var requestparams = ${attrSafe("request_params")};
            build_navbar(${'$'}("#nav-bar"), ["""),
            Block("NAV_BAR", true),
        text("]);"),
            Block("SCRIPT", true)
    )
})