package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.ktml.builder.*
import com.heerkirov.ktml.element.*

/**二级根页面。提供了顶栏、底部标记条的实现。
 */
class StdBasic : HtmlView(Meta::class, {
    impl("TOP_BAR", nav(clazz = "navbar navbar-expand-sm bg-dark navbar-dark") {
        a_(clazz = "navbar-brand", href = "/BangumiNote/") { proxyStr("val_logo") }
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
                ul(clazz = "nav navbar-nav navbar-right d-sm-none d-block") {
                    li(clazz = "nav-item") {
                        button_(clazz = "nav-link btn btn-link", id = "noticeButtonSmall"){"消息"}
                    }
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
            //navbar right
            ul(clazz = "nav navbar-nav navbar-right d-none d-sm-block mr-4") {
                li(clazz = "nav-item") {
                    button(clazz = "nav-link btn btn-link", id = "noticeButton"){
                        i(clazz = "fa fa-comment-o")
                    }
                }
            }
            ul(clazz = "nav navbar-nav navbar-right d-none d-sm-block") {
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
    }, div(clazz = "modal fade", id = "noticeModal") {
        div(clazz = "modal-dialog") {
            div(clazz = "modal-content") {
                div(clazz = "modal-header") {
                    h4(clazz = "modal-title") {text("消息")}
                    button_(clazz = "close", dataDismiss = "modal", type = "button") { "&times;" }
                }
                div(clazz = "modal-body", id = "noticeModalBody") {

                }
                div(clazz = "modal-footer") {

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
                location.href = "/BangumiNote/"
            },
            error: function () {
                alert("登出时发生错误。")
            }
        });
    }
    """.trimIndent() }, script {
        text("""${'$'}(function(){""")
        text("""
            var messageRequest = restful.newgeneral({content: {
                exists: {url: "${proxyURL("api_message_exist")}"},
                unread: {url: "${proxyURL("api_message_unread")}"}
            }});
            analysisNotice({
                noticeButton: ${'$'}("#noticeButton"),
                noticeButtonSmall: ${'$'}("#noticeButtonSmall"),
                noticeModal: ${'$'}("#noticeModal"),
                noticeModalBody: ${'$'}("#noticeModalBody"),
                messageRequest: messageRequest
            });
        """.trimIndent())
        block("SCRIPT", true)
        text("""});""")
    })
})