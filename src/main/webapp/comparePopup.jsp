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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
    <meta charset="UTF-8">
    <title>Product Comparison</title>

    <style>
        body {
            font-family: Arial, sans-serif;
            padding: 20px;
        }

        h2 {
            margin-bottom: 20px;
        }

        table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 15px;
        }

        table, th, td {
            border: 1px solid #888;
        }

        th, td {
            padding: 10px;
            text-align: center;
        }

        th {
            background: #f0f0f0;
            font-weight: bold;
        }

        .label {
            background: #fafafa;
            font-weight: bold;
        }

        .feedback-box {
            margin-top: 20px;
            padding: 15px;
            border: 1px solid #777;
            background: #f9f9f9;
        }
    </style>

</head>
<body>

<%
    String id1 = request.getParameter("id1");
    String id2 = request.getParameter("id2");

    // ⭐ JPetStore 더미 상품 데이터 (나중에 DB 값으로 교체 가능)
    class ProductInfo {
        String price, characteristics, suitability, pros, cons;

        ProductInfo(String price, String characteristics, String suitability,
                    String pros, String cons) {
            this.price = price;
            this.characteristics = characteristics;
            this.suitability = suitability;
            this.pros = pros;
            this.cons = cons;
        }
    }

    // 예시로 상품 1, 2 더미 데이터
    ProductInfo p1 = new ProductInfo(
        "$16.50",
        "Small freshwater fish",
        "Best for beginners",
        "Low maintenance",
        "Small tank needed"
    );

    ProductInfo p2 = new ProductInfo(
        "$18.50",
        "Medium reptile breed",
        "Intermediate difficulty",
        "Unique appearance",
        "Requires heat lamp"
    );
%>

<h2>Product Comparison</h2>

<table>
    <tr>
        <th>Comparison Item</th>
        <th>Product 1 ( <%= id1 %> )</th>
        <th>Product 2 ( <%= id2 %> )</th>
    </tr>

    <tr>
        <td class="label">Price</td>
        <td><%= p1.price %></td>
        <td><%= p2.price %></td>
    </tr>

    <tr>
        <td class="label">Characteristics</td>
        <td><%= p1.characteristics %></td>
        <td><%= p2.characteristics %></td>
    </tr>

    <tr>
        <td class="label">Suitability</td>
        <td><%= p1.suitability %></td>
        <td><%= p2.suitability %></td>
    </tr>

    <tr>
        <td class="label">Pros</td>
        <td><%= p1.pros %></td>
        <td><%= p2.pros %></td>
    </tr>

    <tr>
        <td class="label">Cons</td>
        <td><%= p1.cons %></td>
        <td><%= p2.cons %></td>
    </tr>
</table>

<div class="feedback-box">
    <h3>Final Feedback</h3>
    <p>— 여기에 GPT 추천 결과 들어갈 자리 —</p>
</div>

</body>
</html>

