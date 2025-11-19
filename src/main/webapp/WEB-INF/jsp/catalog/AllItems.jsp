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
                style="padding: 8px 20px; font-size: 14px; height: 34px;
                       background-color:#215E21; color:white; border: 1px solid #124812;">
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
            background:rgba(0,0,0,0.45);
            backdrop-filter: blur(2px);
            z-index:9998;">
</div>

<!-- JPetStore 스타일 모달 -->
<div id="compareModal"
     style="display:none;
            position:fixed;
            top:50%; left:50%;
            transform:translate(-50%, -50%);
            width:720px;
            background:white;
            border-radius:12px;
            border:2px solid #2f4f2f; /* JPetStore 테마색 */
            box-shadow:0px 8px 25px rgba(0,0,0,0.35);
            z-index:9999;
            overflow:hidden;
            animation: fadeIn 0.25s ease-out;">

    <!-- 헤더(녹색바) -->
    <div style="background:#3c6e47;
                padding:12px 16px;
                color:white;
                font-size:18px;
                font-weight:bold;
                display:flex;
                justify-content:space-between;
                align-items:center;">

        <span>Product Comparison</span>

        <button id="closeModalBtn"
                style="background:#244026;
                       color:white;
                       border:none;
                       padding:5px 12px;
                       border-radius:4px;
                       cursor:pointer;
                       font-weight:bold;">
            X
        </button>
    </div>

    <!-- iframe 박스 -->
    <iframe id="compareFrame" src=""
            style="width:100%; height:520px; border:none;"></iframe>
</div>

<!-- 모달 fade-in 애니메이션 -->
<style>
@keyframes fadeIn {
  from { opacity: 0; transform:translate(-50%, -48%); }
  to   { opacity: 1; transform:translate(-50%, -50%); }
}
</style>


<!-- ============================= -->
<!-- ⭐ Compare 버튼 자바스크립트 ⭐ -->
<!-- ============================= -->

<script>
document.getElementById("compareBtn").addEventListener("click", function() {
    const modal = document.getElementById("compareModal");
    const overlay = document.getElementById("compareModalOverlay");
    const iframe = document.getElementById("compareFrame");

    // 이미 열려있으면 닫기
    if (modal.style.display === "block") {
        modal.style.display = "none";
        overlay.style.display = "none";
        return;
    }

    const checked = document.querySelectorAll("input[name='selectedItems']:checked");

    if (checked.length !== 2) {
        alert("Please select exactly 2 items to compare!");
        return;
    }

    const id1 = checked[0].value;
    const id2 = checked[1].value;

    const base = "${pageContext.request.contextPath}";
    const url = base + "/comparePopup.jsp?id1=" + id1 + "&id2=" + id2;

    // 모달 숨긴 상태에서 iframe 로딩
    iframe.style.opacity = "0";   // 로딩 전 숨김
    iframe.src = url;

    // iframe이 완전히 로드되면 모달을 표시
    iframe.onload = function() {
        overlay.style.display = "block";
        modal.style.display = "block";

        // 부드럽게 나타나는 효과
        setTimeout(() => { iframe.style.opacity = "1"; }, 10);
    };
});

// X 버튼으로 닫기
document.getElementById("closeModalBtn").addEventListener("click", function() {
    document.getElementById("compareModalOverlay").style.display = "none";
    document.getElementById("compareModal").style.display = "none";
});
</script>



<%@ include file="../common/IncludeBottom.jsp"%>
