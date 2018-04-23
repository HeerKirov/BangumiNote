<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../../template/header.html"%>
    <title>系列 - 数据库 - BangumiNote</title>
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
    $(function () {
        <%@include file="../build_navlist.html"%>
        build_navbar($("#nav-bar"), [
            {title: "数据库", link: "/web/data"},
            {title: "系列", link: "/web/data/series"}
        ]);
        var rest = restful.newlist({url: "/api/content/series"}).appendparams(requestparams);
        build_list($("#api-panel")).info({
            title: "系列",
            createUrl: "/web/data/series/create",
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "/web/data/series/info/" + i.id}},
                {header: "名称", field: "name", sortable: true, link: function (i) {return "/web/data/series/info/" + i.id}},
                {header: "创建时间", field: "create_time", sortable: true, type: "datetime"}
            ]
        }).rest(rest.request).build();
    });
</script>
</html>
