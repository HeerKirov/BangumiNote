<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../../template/header.html"%>
    <title>系列详情 - 数据库 - BangumiNote</title>
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
            {title: "详情", link: "#"}
        ]);
        var rest = restful.newdetail({url: "/api/content/series/${id}"});
        build_detail($("#api-panel")).info({
            title: "系列",
            deleteUrl: "/web/data/series",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    })
</script>
</html>
