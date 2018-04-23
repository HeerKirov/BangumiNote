<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../header.html"%>
    <title><c:out value="${url} - API视图 - BangumiNote"/></title>
</head>
<body>
<%@include file="../page_front.html"%>
<div class="container">
    <div class="row m-4"></div>
    <div class="row">
        <div class="col-sm-3"></div>
        <div class="col-sm-6">
            <div class="row jumbotron">
                <div class="col">
                    <pre id="api-code"><c:out value="${content}"/></pre>
                    <label for="api-url">API</label>
                    <input id="api-url" type="text" class="form-control"/>
                    <label for="content">JSON</label>
                    <textarea class="form-control" rows="5" id="content"></textarea>
                    <label for="method">METHOD</label>
                    <input class="form-control" id="method" value="GET"/>
                    <button class="btn btn-secondary" id="btn-submit">提交</button>
                </div>
            </div>
        </div>
        <div class="col-sm-3"></div>
    </div>
</div>
<%@include file="../page_end.html"%>
</body>
<script>
    function toJson(code) {
        return JSON.stringify(JSON.parse(code), null, 4)
    }
    $(document).ready(function () {
        $("#api-url").text(window.location.href);
        var code = $("#api-code");
        code.text(toJson(code.text()));
        $("#btn-submit").click(function () {
            var method = $("#method").val();
            var url = $("#api-url").val();
            var content = $("#content").val();
            var data = (method==="GET"||method==="POST")?{json: content}:content;
            $.ajax({
                type: method,
                async: true,
                url: url,
                dataType: "json",
                data: data,
                success: function(data, textStatus, xhr) {
                    $("#api-code").text(JSON.stringify(data, null, 4))
                },
                error: function (xhr, textStatus, errorThown) {
                    $("#api-code").text(toJson(xhr.responseText))
                }
            });
        })
    })
</script>
</html>
