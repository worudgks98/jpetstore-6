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
<%--
    Licensed under the Apache License, Version 2.0
--%>
<%@ include file="../common/IncludeTop.jsp"%>

<jsp:useBean id="catalog"
    class="org.mybatis.jpetstore.web.actions.CatalogActionBean" />

<div id="Catalog">

<h2>All Products</h2>

<div style="position: relative; display: inline-block;">
     <div style="position: absolute; right: 0; top: -40px;">
            <button type="button" id="compareBtn" class="Button"
                style="padding: 8px 20px; font-size: 14px; height: 34px;">
                Compare
            </button>
        </div>

<table>
    <tr>
        <th>Select</th>
        <th>Item ID</th>
        <th>Product ID</th>
        <th>Description</th>
        <th>List Price</th>
        <th>&nbsp;</th>
    </tr>

    <c:forEach var="item" items="${actionBean.itemList}">
        <tr>
            <td>
                <input type="checkbox" name="selectedItems" value="${item.itemId}">
            </td>
            <td>
                <stripes:link
                    beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean"
                    event="viewItem">
                    <stripes:param name="itemId" value="${item.itemId}" />
                    ${item.itemId}
                </stripes:link>
            </td>
            <td>${item.product.productId}</td>
            <td>${item.attribute1} ${item.attribute2} ${item.attribute3}
                ${item.attribute4} ${item.attribute5} ${item.product.name}</td>
            <td><fmt:formatNumber value="${item.listPrice}" pattern="$#,##0.00" /></td>

            <td>
                <stripes:link class="Button"
                    beanclass="org.mybatis.jpetstore.web.actions.CartActionBean"
                    event="addItemToCart">
                    <stripes:param name="workingItemId" value="${item.itemId}" />
                    Add to Cart
                </stripes:link>
            </td>
        </tr>
    </c:forEach>
</table>

</div>

<!-- ============================= -->
<!-- ⭐ 모달 팝업 overlay + box ⭐ -->
<!-- ============================= -->

<!-- 어두운 배경 -->
<div id="compareModalOverlay"
     style="display:none;
            position:fixed; top:0; left:0;
            width:100%; height:100%;
            background:rgba(0,0,0,0.5);
            z-index:9998;">
</div>

<!-- 중앙 모달 박스 -->
<div id="compareModal"
     style="display:none;
            position:fixed;
            top:50%; left:50%;
            transform:translate(-50%, -50%);
            width:700px;
            padding:20px;
            background:white;
            border-radius:12px;
            box-shadow:0 0 20px rgba(0,0,0,0.4);
            z-index:9999;">

    <div style="text-align:right; margin-bottom:5px;">
        <button id="closeModalBtn"
                style="background:#333; color:white; padding:6px 14px;
                       border:none; border-radius:4px; cursor:pointer;">
            X
        </button>
    </div>

    <!-- 비교 페이지를 로드할 iframe -->
    <iframe id="compareFrame" src=""
            style="width:100%; height:500px; border:none;"></iframe>
</div>

<!-- ============================= -->
<!-- ⭐ Compare 버튼 자바스크립트 ⭐ -->
<!-- ============================= -->

<script>
document.getElementById("compareBtn").addEventListener("click", function() {
    const checked = document.querySelectorAll("input[name='selectedItems']:checked");

    if (checked.length !== 2) {
        alert("Please select exactly 2 items to compare!");
        return;
    }

    const id1 = checked[0].value;
    const id2 = checked[1].value;

    const base = "${pageContext.request.contextPath}";
    const url = base + "/comparePopup.jsp?id1=" + id1 + "&id2=" + id2;

    // iframe에 URL 넣기
    document.getElementById("compareFrame").src = url;

    // 모달 열기
    document.getElementById("compareModalOverlay").style.display = "block";
    document.getElementById("compareModal").style.display = "block";
});

// 모달 닫기
document.getElementById("closeModalBtn").addEventListener("click", function() {
    document.getElementById("compareModalOverlay").style.display = "none";
    document.getElementById("compareModal").style.display = "none";
});
</script>

<%@ include file="../common/IncludeBottom.jsp"%>
