<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>DB 저장 결과</title>
</head>
<body>
    <h1>DB 저장 결과</h1>
<%
    // SaveServlet이 request에 담아 보낸 "message"를 받습니다.
    String message = (String) request.getAttribute("message");

    if (message != null) {
        out.println("<h2>" + message + "</h2>");
    } else {
        out.println("<h2>처리된 결과가 없습니다.</h2>");
    }
%>
    <br>
    <a href="index.jsp">첫 페이지로 돌아가기</a>
</body>
</html>