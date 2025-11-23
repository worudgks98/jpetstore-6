<%--

       Copyright 2010-2025 the original author or authors.

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

          https://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>

<stripes:useActionBean beanclass="org.mybatis.jpetstore.web.actions.ComparePopupActionBean" var="actionBean"/>

<!DOCTYPE html>
<html>
<head>
    <title>Compare Items</title>
    <link rel="stylesheet" href="<c:url value='/css/jpetstore.css'/>" type="text/css"/>

    <style>
        body { margin: 10px; }
        h2 { margin-bottom: 15px; }

        table.compare-table {
            width: 100%;
            border-collapse: collapse;
        }
        table.compare-table th,
        table.compare-table td {
            border: 1px solid #cccccc;
            padding: 6px 10px;
            text-align: left;
        }
        table.compare-table th {
            background-color: #e5f2e5;
            width: 30%;
        }
        table.compare-table td {
            background-color: #ffffff;
        }
        .feedback-box {
            margin-top: 20px;
            padding: 12px;
            border: 1px solid #cccccc;
            background-color: #f9f9f9;
        }
        .feedback-box h3 {
            margin-top: 0;
            margin-bottom: 8px;
        }
        .close-btn-area {
            text-align: right;
            margin-top: 10px;
        }
    </style>
</head>

<body>

<h2>Compare Selected Items</h2>

<c:if test="${empty actionBean.item1 or empty actionBean.item2}">
    <p>Items could not be loaded. Please close this window and try again.</p>
</c:if>

<c:if test="${not empty actionBean.item1 and not empty actionBean.item2}">
    <table class="compare-table">
        <tr>
            <th>Item ID</th>
            <td>${actionBean.item1.itemId}</td>
            <td>${actionBean.item2.itemId}</td>
        </tr>

        <tr>
            <th>Product</th>
            <td>
                <c:out value="${actionBean.item1.product.productId}"/> -
                <c:out value="${actionBean.item1.product.name}"/>
            </td>
            <td>
                <c:out value="${actionBean.item2.product.productId}"/> -
                <c:out value="${actionBean.item2.product.name}"/>
            </td>
        </tr>

        <tr>
            <th>Price</th>
            <td><fmt:formatNumber value="${actionBean.item1.listPrice}" pattern="$#,##0.00"/></td>
            <td><fmt:formatNumber value="${actionBean.item2.listPrice}" pattern="$#,##0.00"/></td>
        </tr>

        <tr>
            <th>Residence Environment</th>
            <td colspan="2">
                <c:out value="${actionBean.residenceEnv}"/>
            </td>
        </tr>

        <tr>
            <th>Preferred Pet Size</th>
            <td colspan="2">
                <c:out value="${actionBean.petSizePref}"/>
            </td>
        </tr>

        <tr>
            <th>Activity Time</th>
            <td colspan="2">
                <c:out value="${actionBean.activityTime}"/>
            </td>
        </tr>
    </table>

    <c:if test="${not empty actionBean.finalFeedback}">
        <div class="feedback-box">
            <h3>Final Recommendation</h3>
            <p>${actionBean.finalFeedback}</p>
        </div>
    </c:if>
</c:if>

<div class="close-btn-area">
    <button type="button" onclick="window.close();">Close</button>
</div>

</body>
</html>
