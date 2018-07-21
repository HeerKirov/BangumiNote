package com.heerkirov.bangumi.controller.ktml.login

import com.heerkirov.bangumi.controller.ktml.SimpleBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WebLoginView(@Autowired p: ConstProxy): HtmlView(SimpleBasic::class, p, {
    impl("TITLE", text(proxyStr("val_logo")))
    impl("BODY", div(clazz = "container") {
        div(clazz = "row m-4")
        div(clazz = "row") {
            div(clazz = "col-md-4 col-lg-8")
            div(clazz = "col-md-8 col-lg-4 bg-light jumbotron") {
                div(clazz = "row mb-4") {
                    label_(clazz = "col-lg-12 text-center") { "用户登录" }
                }
                div(id = "div-username", clazz = "form-group row") {
                    label_(clazz = "col-lg-3 form-control-label", forz = "login-username") { "ID" }
                    div(clazz = "col-lg-9") {
                        input(type = "text", maxlength = 16, clazz = "form-control", id = "login-username", name = "username", placeholder = "请输入用户ID")
                    }
                }
                div(id = "div-password", clazz = "form-group row") {
                    label_(clazz = "col-lg-3 form-control-label", forz = "login-password") { "密码" }
                    div(clazz = "col-lg-9") {
                        input(type = "password", maxlength = 32, clazz = "form-control", id = "login-password", name = "password", placeholder = "请输入密码")
                    }
                }
                div(clazz = "row mt-4 mb-4") {
                    div(clazz = "col-lg-3")
                    div(clazz = "col-lg-6 btn-group") {
                        button_(id="btn-login", clazz = "btn btn-primary col", onclick = "do_login()") { "登录" }
                        button_(id="btn-register", clazz = "btn btn-secondary col", onclick = "register()") { "注册" }
                    }
                    div(clazz = "col-lg-3")
                }
                div(id = "login-alert", clazz = "alert alert-danger", style = "display: none") {
                    strong_ { "错误" }; text("登录失败。请检查用户名和密码。")
                }
            }
        }
    })
    impl("SCRIPT", script_ { """
    function do_login(){
        var data = {
            username: ${'$'}("#login-username").val(),
            password: ${'$'}("#login-password").val()
        };
        ${'$'}.ajax({
            type: "POST",
            async: false,
            url: "${proxyURL("api_user_login")}",
            data: {json: JSON.stringify(data)},
            dataType: 'json',
            success: function() {
                location.href = "${proxyURL("web_home")}"
            },
            error: function () {
                ${'$'}("#login-alert").show()
            }
        });
    }
    function register() {
        location.href = "${proxyURL("web_register")}"
    }
    ${'$'}(document).ready(function () {
        ${'$'}("#login-username").keypress(function (event) {
            if(event.keyCode===13)${'$'}("#login-password")[0].focus();
        })
        ${'$'}("#login-password").keypress(function (event) {
           if(event.keyCode===13)do_login();
        });
    })"""})
})