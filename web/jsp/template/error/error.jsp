<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <%@include file="../header.html"%>
    <title><c:out value="${error_title} - BangumiNote"/></title>
</head>
<body>
<%@include file="../page_front.html"%>
<div class="container">
    <div class="row m-4"></div>
    <div class="row">
        <div class="col-md-2 pr-2 pt-2" id="nav-list"></div>
        <div class="col p-5 bg-light" id="api-panel">
            <div class="row">
                <div class="col">
                    <h2><c:out value="${error_title}"/> </h2>
                </div>
            </div>
            <div class="row mt-4">
                <div class="col">
                    <c:out value="${error_message}"/>
                </div>
            </div>
        </div>
        <div class="col-md-2"></div>
    </div>
</div>
<%@include file="../page_end.html"%>
</body>
<script>

</script>
</html>
