<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../template/header.html"%>
    <title>数据库 - BangumiNote</title>
</head>
<body>
<%@include file="../template/page_front.html"%>
<div class="container">
    <div class="row">
        <div class="col-sm-1"></div>
        <div class="col-sm-10">
            <div class="row m-2" id="nav-bar">

            </div>
            <div class="row p-5 bg-light">
                <div class="col">
                    <div class="row mb-4">
                        <h2>数据库</h2>
                    </div>
                    <div class="row list-group">
                        <a href="/web/data/series" class="list-group-item list-group-item-action">系列</a>
                        <a href="/web/data/authors" class="list-group-item list-group-item-action">作者</a>
                        <a href="/web/data/companies" class="list-group-item list-group-item-action">制作公司</a>
                        <a href="/web/data/animes" class="list-group-item list-group-item-action">番組</a>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-sm-1"></div>
    </div>
</div>
<%@include file="../template/page_end.html"%>
</body>
<%@include file="../template/restlist_js.html"%>
<script>
    $(document).ready(function () {
        build_navbar($("#nav-bar"), [
            {title: "数据库", link: "/web/data"},
        ]);
    })
</script>
</html>
