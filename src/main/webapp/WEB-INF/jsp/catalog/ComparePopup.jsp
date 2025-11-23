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
    <title>Product Comparison</title>
    <link rel="stylesheet" href="<c:url value='/css/jpetstore.css'/>" type="text/css"/>

    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f3f4f3;
        }

        /* 콘텐츠 카드 */
        .compare-card {
            background: white;
            border: 2px solid #3c6e47;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.15);
        }

        table.compare-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
        }

        table.compare-table th,
        table.compare-table td {
            border: 1px solid #cccccc;
            padding: 10px 12px;
            font-size: 14px;
        }

        table.compare-table th {
            background-color: #e5f2e5;
            font-weight: bold;
            width: 30%;
        }

        .feedback-box {
            margin-top: 25px;
            padding: 18px;
            background: #fafff8;
            border-left: 6px solid #3c6e47;
            border-radius: 6px;
        }

        .feedback-box h3 {
            margin-top: 0;
            margin-bottom: 10px;
            font-size: 18px;
            color: #2f4f2f;
        }

        .close-btn-area {
            text-align: right;
            margin-top: 20px;
        }

        .close-btn-area button {
            background: #3c6e47;
            color: white;
            border: none;
            padding: 8px 18px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
        }
        .close-btn-area button:hover {
            background: #274c32;
        }
    </style>
</head>

<body>

<div class="compare-card">

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
                <td>${actionBean.item1.product.productId} - ${actionBean.item1.product.name}</td>
                <td>${actionBean.item2.product.productId} - ${actionBean.item2.product.name}</td>
            </tr>

            <tr>
                <th>Price</th>
                <td><fmt:formatNumber value="${actionBean.item1.listPrice}" pattern="$#,##0.00"/></td>
                <td><fmt:formatNumber value="${actionBean.item2.listPrice}" pattern="$#,##0.00"/></td>
            </tr>

            <tr>
                <th>Residence Environment</th>
                <td colspan="2">${actionBean.residenceEnv}</td>
            </tr>

            <tr>
                <th>Preferred Pet Size</th>
                <td colspan="2">${actionBean.petSizePref}</td>
            </tr>

            <tr>
                <th>Activity Time</th>
                <td colspan="2">${actionBean.activityTime}</td>
            </tr>
        </table>

        <c:if test="${not empty actionBean.finalFeedback}">
            <div class="feedback-box">
                <h3>Final Recommendation</h3>
                <p>${actionBean.finalFeedback}</p>
            </div>
        </c:if>

    </c:if>



</div>

</body>
</html>
