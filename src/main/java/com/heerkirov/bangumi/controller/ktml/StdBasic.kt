package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.ktml.builder.*
import com.heerkirov.ktml.element.*

/**二级根页面。提供了顶栏、底部标记条的实现。
 */
class StdBasic : HtmlView(Meta::class, {
    impl("TOP_BAR", nav(clazz = "navbar navbar-expand-sm bg-dark navbar-dark") {
        a_(clazz = "navbar-brand", href = "/") { proxyStr("val_logo") }
        if(attrAs("security_authority")) {
            //toggle/collapse button
            button(clazz = "navbar-toggler", type = "button", dataToggle = "collapse", dataTarget = "#collapsible_navbar") {
                span(clazz = "navbar-toggler-icon")
            }
            //navbar links
            div(clazz = "collapse navbar-collapse", id = "collapsible_navbar") {
                ul(clazz = "navbar-nav") {
                    li(clazz = "nav-item") {
                        a_(clazz = "nav-link", href = proxyURL("web_home")) {"主页"}
                    }
                    li(clazz = "nav-item") {
                        a_(clazz = "nav-link", href = proxyURL("web_diary")) {"日记"}
                    }
                    li(clazz = "nav-item") {
                        a_(clazz = "nav-link", href = proxyURL("web_data")) {"数据库"}
                    }
                    li(clazz = "nav-item") {
                        a_(clazz = "nav-link", href = proxyURL("web_statistics")) {"统计"}
                    }
                }
            }
            //navbar right
            ul(clazz = "nav navbar-nav navbar-right") {
                li(clazz = "dropdown") {
                    a(href = "#", clazz = "nav-link dropdown-toggle", dataToggle = "dropdown") {
                        text(attrAs("security_name"))
                        b(clazz = "caret")
                    }
                    ul(clazz = "dropdown-menu") {
                        li {a_(clazz = "nav-link text-dark", href = proxyURL("web_document")){"资料"}}
                        li {a_(clazz = "nav-link text-dark", href = "javascript:void(0)", onclick = "logout()"){"登出"}}
                    }
                }
            }
        }
    })
    impl("BOTTOM_BAR", div(clazz = "container-fluid") {
        div(clazz = "row mt-5") {
            div(clazz = "col") {
                label_(clazz = "col-lg-12 text-center") { "BangumiNote@HeerKirov" }
            }
        }
    })
    impl("SCRIPT", script_ {"""
    function logout() {
        ${'$'}.ajax({
            type: "POST",
            async: false,
            url: "${proxyURL("api_user_logout")}",
            dataType: 'json',
            success: function() {
                location.href = "/"
            },
            error: function () {
                alert("登出时发生错误。")
            }
        });
    }
    """.trimIndent() }, Block("SCRIPT", true))
})