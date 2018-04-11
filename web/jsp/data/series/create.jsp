<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../../template/header.html"%>
    <title>新建系列 - 数据库 - BangumiNote</title>
</head>
<body>
<%@include file="../../template/page_front.html"%>
<div class="container">
    <div class="row m-2">
        <div class="col-md-2"></div>
        <div class="col" id="nav-bar">

        </div>
    </div>
    <div class="row">
        <div class="col-md-2 pr-2 pt-2" id="nav-list">

        </div>
        <div class="col p-5 bg-light" id="api-panel">

        </div>
        <div class="col-md-2"></div>
    </div>
</div>
<%@include file="../../template/page_end.html"%>
</body>
<%@include file="../../template/restlist_js.html"%>
<script>
    $(document).ready(function () {
        <%@include file="../build_navlist.html"%>
        build_navbar($("#nav-bar"), [
            {title: "数据库", link: "/web/data"},
            {title: "系列", link: "/web/data/series"},
            {title: "新建", link: "/web/data/series/create"}
        ]);
        var rest = restful.newcreate({url: "/api/content/series"});
        build_create($("#api-panel")).info({
            title: "系列",
            successUrl: "/web/data/series",
            content: [
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}}
            ]
        }).rest(rest.request);
    })
</script>
</html>
