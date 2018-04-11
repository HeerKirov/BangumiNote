<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <%@include file="../template/header.html"%>
  <title>注册 - BangumiNote</title>
</head>
<body>
<%@include file="../template/page_front_simple.html"%>
<div class="container">
  <div class="row m-4"></div>
  <div class="row">
    <div class="col-sm-2"></div>
    <!-- 成功注册的面板 -->
    <div class="col-sm-8 jumbotron" id="register-success" style="display: none;">
      <div class="row mt-4 mb-4">
        <label class="col-lg-12 text-center">注册成功!</label>
        <button type="button" class="btn btn-link text-center" onclick="do_turn()">转到主页</button>
      </div>
    </div>
    <!-- 注册面板 -->
    <div class="col-sm-8 jumbotron" id="register-panel">
      <div class="row mb-4">
        <div class="col-sm-2"></div>
        <div class="col-sm-8">
          <label class="col-lg-12 text-center">新用户注册</label>
          <form method="post" id="form-register">
            <div id="div-username" class="form-group row">
              <label class="col-lg-3 form-control-label" for="register-username">ID</label>
              <div class="col-lg-9">
                <input type="text" maxlength="16" class="form-control" id="register-username" name="username" placeholder="请输入用户ID">
              </div>
            </div>
            <div id="div-name" class="form-group row">
              <label class="col-lg-3 form-control-label" for="register-name">用户名</label>
              <div class="col-lg-9">
                <input type="text" maxlength="16" class="form-control" id="register-name" name="username" placeholder="请输入用户名">
              </div>
            </div>
            <div id="div-password" class="form-group row">
              <label class="col-lg-3 form-control-label" for="register-password">密码</label>
              <div class="col-lg-9">
                <input type="password" maxlength="32" class="form-control" id="register-password" name="password" placeholder="请输入密码">
              </div>
            </div>
            <div id="div-password-check" class="form-group row">
              <label class="col-lg-3 form-control-label" for="register-password-check">确认密码</label>
              <div class="col-lg-9">
                <input type="password" maxlength="32" class="form-control" id="register-password-check" name="password" placeholder="请再次输入密码">
              </div>
            </div>
          </form>
        </div>
        <div class="col-sm-2"></div>
      </div>
      <div class="row mt-4 mb-4">
        <div class="col-lg-4"></div>
        <div class="col-lg-4">
          <button id="btn-register" class="btn btn-primary col" onclick="do_register()">确认</button>
        </div>
        <div class="col-lg-4"></div>
      </div>
      <div id="register-alert" class="alert alert-danger" style="display: none;">
      </div>
    </div>
    <div class="col-sm-2"></div>
  </div>
</div>
<%@include file="../template/page_end.html"%>
</body>
<script>
    function do_register() {
        hide_alert();
        var username = $("#register-username").val();
        var name = $("#register-name").val();
        var password = $("#register-password").val();
        var password_check = $("#register-password-check").val();
        if(username.length==0){
            show_alert("用户ID不能为空。");
        }else if(password!=password_check){
            show_alert("两次输入的密码不相同。");
        }else{
            var data = {id: username, name: name, password: password};
            $.ajax({
                type: "POST",
                async: false,
                url: "/api/user/register.json",
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
        location.href = "/web/home/home"
    }
    function hide_alert() {
        $("#register-alert").hide().text("")
    }
    function show_alert(message) {
        $("#register-alert").show().text(message)
    }
    function show_success() {
        $("#register-success").show();
        $("#register-panel").hide();
    }

</script>
</html>
