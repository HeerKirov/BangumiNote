<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <%@include file="../template/header.html"%>
    <title>BangumiNote</title>
</head>
<body>
<%@include file="../template/page_front_simple.html"%>
<div class="container">
    <div class="row m-4"></div>
    <div class="row">
        <div class="col-md-4 col-lg-8"></div>
        <div class="col-md-8 col-lg-4 bg-light jumbotron">
            <div class="row mb-4">
                <label class="col-lg-12 text-center">用户登录</label>
            </div>
            <form method="post" id="form-login">
                <div id="div-username" class="form-group row">
                    <label class="col-lg-3 form-control-label" for="login-username">ID</label>
                    <div class="col-lg-9">
                        <input type="text" maxlength="16" class="form-control" id="login-username" name="username" placeholder="请输入用户ID">
                    </div>
                </div>
                <div id="div-password" class="form-group row">
                    <label class="col-lg-3 form-control-label" for="login-password">密码</label>
                    <div class="col-lg-9">
                        <input type="password" maxlength="32" class="form-control" id="login-password" name="password" placeholder="请输入密码">
                    </div>
                </div>
            </form>
            <div class="row mt-4 mb-4">
                <div class="col-lg-3"></div>
                <div class="col-lg-6 btn-group">
                    <button id="btn-login" class="btn btn-primary col" onclick="do_login()">登录</button>
                    <button id="btn-register" class="btn btn-secondary col" onclick="register()">注册</button>
                </div>
                <div class="col-lg-3"></div>
            </div>
            <div id="login-alert" class="alert alert-danger" style="display: none;">
                <strong>错误</strong> 登录失败。请检查用户名和密码。
            </div>
        </div>
    </div>
</div>
<%@include file="../template/page_end.html"%>
</body>
<script>
    function do_login(){
        var data = {
            username: $("#login-username").val(),
            password: $("#login-password").val()
        };
        $.ajax({
            type: "POST",
            async: false,
            url: "/api/user/login.json",
            data: {json: JSON.stringify(data)},
            dataType: 'json',
            success: function() {
                location.href = "/web/home/home"
            },
            error: function () {
                $("#login-alert").show()
            }
        });
    }
    function register() {
        location.href = "/web/register"
    }
    $(document).ready(function () {
        $("#login-username").keypress(function (event) {
            if(event.keyCode===13)$("#login-password")[0].focus();
        })
        $("#login-password").keypress(function (event) {
           if(event.keyCode===13)do_login();
        });
    })
</script>
</html>