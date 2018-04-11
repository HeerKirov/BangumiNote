package com.heerkirov.bangumi.controller.ktml.login

import com.heerkirov.bangumi.controller.ktml.SimpleBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WebRegisterView(@Autowired p: ConstProxy): HtmlView(SimpleBasic::class, p, {
    impl("TITLE", text("用户注册 - ${proxyStr("val_logo")}"))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-4")
        div(clazz = "row") {
            div(clazz = "col-sm-2")
            div(clazz = "col-sm-8 jumbotron", id = "register-success", style = "display: none;") {
                div(clazz = "row mt-4 mb-4") {
                    label_(clazz = "col-lg-12 text-center") {"注册成功!"}
                    button_(type = "button", clazz = "btn btn-link text-center", onclick = "do_turn()") { "转到主页" }
                }
            }
            div(clazz = "col-sm-8 jumbotron", id = "register-panel") {
                div(clazz = "row mb-4") {
                    div(clazz = "col-sm-2")
                    div(clazz = "col-sm-8") {
                        label_(clazz = "col-lg-12 text-center") {"新用户注册"}
                        div(id = "div-username", clazz = "form-group row") {
                            label_(clazz = "col-lg-3 form-control-label", forz = "register-username") {"ID"}
                            div(clazz = "col-lg-9") {
                                input(type = "text", maxlength = 16, clazz = "form-control", id = "register-username", name = "username", placeholder = "请输入用户ID")
                            }
                        }
                        div(id = "div-name", clazz = "form-group row") {
                            label_(clazz = "col-lg-3 form-control-label", forz = "register-name") {"用户名"}
                            div(clazz = "col-lg-9") {
                                input(type = "text", maxlength = 16, clazz = "form-control", id = "register-name", name = "name", placeholder = "请输入用户名")
                            }
                        }
                        div(id = "div-password", clazz = "form-group row") {
                            label_(clazz = "col-lg-3 form-control-label", forz = "register-password") {"密码"}
                            div(clazz = "col-lg-9") {
                                input(type = "password", maxlength = 32, clazz = "form-control", id = "register-password", name = "password", placeholder = "请输入密码")
                            }
                        }
                        div(id = "div-password-check", clazz = "form-group row") {
                            label_(clazz = "col-lg-3 form-control-label", forz = "register-password-check") {"确认密码"}
                            div(clazz = "col-lg-9") {
                                input(type = "password", maxlength = 32, clazz = "form-control", id = "register-password-check", name = "password-check", placeholder = "请再次输入密码")
                            }
                        }
                    }
                    div(clazz = "col-sm-2")
                }
                div(clazz = "row mt-4 mb-4") {
                    div(clazz = "col-lg-4")
                    div(clazz = "col-lg-4") {
                        button_(id = "btn-register", clazz = "btn btn-primary col", onclick = "do_register()") {"确认"}
                    }
                    div(clazz = "col-lg-4")
                }
                div(id = "register-alert", clazz = "alert alert-danger", style = "display: none;")
            }
            div(clazz = "col-sm-2")
        }
    })
    impl("SCRIPT", script_ {"""
    function do_register() {
        hide_alert();
        var username = ${'$'}("#register-username").val();
        var name = ${'$'}("#register-name").val();
        var password = ${'$'}("#register-password").val();
        var password_check = ${'$'}("#register-password-check").val();
        if(username.length==0){
            show_alert("用户ID不能为空。");
        }else if(password!=password_check){
            show_alert("两次输入的密码不相同。");
        }else{
            var data = {id: username, name: name, password: password};
            ${'$'}.ajax({
                type: "POST",
                async: false,
                url: "${proxyURL("api_user_register")}",
                data: {json: JSON.stringify(data)},
                dataType: 'json',
                success: function() {
                    show_success();
                },
                error: function (ret) {
                    switch(ret.responseJSON.keyword) {
                        case "USER_EXISTS":
                            show_alert("该用户ID已存在。");
                            break;
                        case "NOT_ENOUGH":
                            show_alert("注册信息不完善。");
                            break;
                        case "REGISTER_FORBIDDEN":
                            show_alert("注册功能已被管理员禁用。");
                            break;
                    }
                }
            });
        }
    }
    function do_turn() {
        location.href = "${proxyURL("web_home")}"
    }
    function hide_alert() {
        ${'$'}("#register-alert").hide().text("")
    }
    function show_alert(message) {
        ${'$'}("#register-alert").show().text(message)
    }
    function show_success() {
        ${'$'}("#register-success").show();
        ${'$'}("#register-panel").hide();
    }""".trimIndent()})
})