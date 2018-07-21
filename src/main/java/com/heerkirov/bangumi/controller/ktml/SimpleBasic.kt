package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.ktml.builder.HtmlCacheView
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*

/**二级根页面。提供了顶栏、底部标记条的实现。
 */
class SimpleBasic : HtmlView(Meta::class, {
    impl("TOP_BAR", nav(clazz = "navbar navbar-expand-sm bg-dark navbar-dark"){
        a_(clazz = "navbar-brand", href = "/BangumiNote/") { proxyStr("val_logo")}
    })
    impl("BOTTOM_BAR", div(clazz = "container-fluid") {
        div(clazz = "row mt-5") {
            div(clazz = "col") {
                label_(clazz = "col-lg-12 text-center") {"BangumiNote@HeerKirov"}
            }
        }
    })
})