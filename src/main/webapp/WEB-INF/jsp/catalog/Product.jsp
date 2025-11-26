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
<%@ include file="../common/IncludeTop.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!-- ★★★ 1. 비교하기 버튼 (우측 상단 고정) ★★★ -->

<jsp:useBean id="catalog"
             class="org.mybatis.jpetstore.web.actions.CatalogActionBean" />

<div id="BackLink">
    <%-- ★★★ 2. [복구] "ALL" 카테고리일 때 메인 메뉴로 돌아가는 로직 ★★★ --%>
    <c:choose>
        <c:when test="${actionBean.product.categoryId == 'ALL'}">
            <stripes:link beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean">
                Return to Main Menu
            </stripes:link>
        </c:when>
        <c:otherwise>
            <stripes:link beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean" event="viewCategory">
                <stripes:param name="categoryId" value="${actionBean.product.categoryId}" />
                Return to ${actionBean.product.categoryId}
            </stripes:link>
        </c:otherwise>
    </c:choose>
</div>

<div id="Catalog">

    <h2>${actionBean.product.name}</h2>

<div class="table-container"
     style="
         position: relative;
         width: max-content;
         margin: 0 auto;
     ">

    <!-- ★ Compare 버튼: 표 오른쪽 끝에 정확히 붙음 ★ -->
    <button id="compareBtn"
            class="compare-btn-fixed"
            disabled
            onclick="openComparisonPopup()"
            style="
                position:absolute;
                top:-35px;
                right:0;
            ">
        Compare
    </button>




    <table class="itemList">
        <tr>
            <th>Item ID</th>
            <th>Product ID</th>
            <th>Description</th>
            <th>List Price</th>
            <th>&nbsp;</th>
        </tr>
        <c:forEach var="item" items="${actionBean.itemList}">
            <tr>
                <td>
                        <%-- 팝업 링크 구조 --%>
                    <stripes:link
                            beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean"
                            event="viewItem"
                            class="item-link">
                        <stripes:param name="itemId" value="${item.itemId}" />
                        ${item.itemId}

                        <%-- 이미지 팝업 --%>
                        <div class="image-popup">
                            <img src="/jpetstore/images/placeholder.gif" alt="Item Image" />
                            <div class="recommend-text">
                                <%-- 각 아이템의 productId별로 추천 메시지 가져오기 --%>
                                <c:set var="currentProductId" value="${item.product.productId}" />
                                <c:choose>
                                    <%-- ALL 카테고리가 아니고 productId가 있으면 actionBean의 메시지 사용 --%>
                                    <c:when test="${actionBean.product.categoryId != 'ALL' && not empty actionBean.productRecommendationMessage}">
                                        <div class="ai-copy ${actionBean.productRecommendationMessage.recommended ? 'RECOMMEND' : 'NOT_RECOMMEND'}">
                                            <div class="ai-copy-body">
                                                <c:out value="${actionBean.productRecommendationMessage.message}" escapeXml="false" />
                                            </div>
                                        </div>
                                    </c:when>
                                    <%-- ALL 카테고리: itemRecommendationMessageMap에서 각 아이템의 productId로 메시지 가져오기 --%>
                                    <c:when test="${actionBean.product.categoryId == 'ALL' && not empty actionBean.itemRecommendationMessageMap}">
                                        <c:set var="itemRecommendation" value="${actionBean.itemRecommendationMessageMap[currentProductId]}" />
                                        <c:choose>
                                            <c:when test="${not empty itemRecommendation}">
                                                <div class="ai-copy ${itemRecommendation.recommended ? 'RECOMMEND' : 'NOT_RECOMMEND'}">
                                                    <div class="ai-copy-body">
                                                        <c:out value="${itemRecommendation.message}" escapeXml="false" />
                                                    </div>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <c:if test="${sessionScope.accountBean.authenticated}">
                                                    <div class="ai-copy neutral">
                                                        설문 답변을 반영한 추천 문구를 준비하는 중입니다.
                                                    </div>
                                                </c:if>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <c:if test="${sessionScope.accountBean.authenticated}">
                                            <div class="ai-copy neutral">
                                                설문 답변을 반영한 추천 문구를 준비하는 중입니다.
                                            </div>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <%-- 데이터 숨김 (이미지 경로용) --%>
                        <span class="popup-data" style="display: none;" data-id="${item.itemId}">
                             <c:out value="${item.product.description}" escapeXml="false" />
                        </span>
                    </stripes:link>
                </td>
                <td>${item.product.productId}</td>
                <td>${item.attribute1} ${item.attribute2} ${item.attribute3}
                        ${item.attribute4} ${item.attribute5} ${item.product.name}</td>
                <td><fmt:formatNumber value="${item.listPrice}"
                                      pattern="$#,##0.00" /></td>
                <td><stripes:link class="Button"
                                  beanclass="org.mybatis.jpetstore.web.actions.CartActionBean"
                                  event="addItemToCart">
                    <stripes:param name="workingItemId" value="${item.itemId}" />
                    Add to Cart
                </stripes:link></td>
            </tr>
        </c:forEach>
        <%-- ★★★ 3. [삭제됨] 여기에 있던 빈 <tr> 태그를 제거했습니다. (이상한 체크박스 원인) ★★★ --%>
    </table>

</div>

<script>
    // 이미지 경로 추출 함수
    function extractImagePath(desc) {
        if (!desc) return '/jpetstore/images/placeholder.gif';

        // <image src="..."> 또는 <img src="..."> 형식 모두 처리
        let match = desc.match(/<(?:image|img)[^>]+src\s*=\s*["']([^"']+)["']/i);
        if (match && match[1]) {
            let imgPath = match[1];
            // 상대 경로를 절대 경로로 변환
            if (imgPath.startsWith('../')) {
                imgPath = imgPath.replace('../', '/jpetstore/');
            } else if (!imgPath.startsWith('/')) {
                imgPath = '/jpetstore/' + imgPath;
            }
            return imgPath;
        }
        return '/jpetstore/images/placeholder.gif';
    }

    // 팝업 위치를 화면 경계에 맞게 조정하는 함수
    function adjustPopupPosition(link, popup) {
        // 링크의 화면상 위치 정보 가져오기 (getBoundingClientRect는 viewport 기준)
        const linkRect = link.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        // 팝업이 표시된 후 실제 크기 측정
        const popupRect = popup.getBoundingClientRect();
        const popupWidth = popupRect.width || 250;
        const popupHeight = popupRect.height || 300;

        // 화면 경계까지의 거리 계산
        const spaceOnLeft = linkRect.left;
        const spaceOnRight = viewportWidth - linkRect.right;
        const spaceOnTop = linkRect.top;
        const spaceOnBottom = viewportHeight - linkRect.bottom;

        // 기본 위치: 링크 왼쪽, 상단 정렬
        let popupLeft = linkRect.left - popupWidth - 10;
        let popupTop = linkRect.top;

        // 왼쪽/오른쪽 위치 결정
        if (spaceOnLeft >= popupWidth + 10) {
            // 왼쪽에 공간이 충분하면 왼쪽에 표시
            popupLeft = linkRect.left - popupWidth - 10;
        } else if (spaceOnRight >= popupWidth + 10) {
            // 오른쪽에 공간이 충분하면 오른쪽에 표시
            popupLeft = linkRect.right + 10;
        } else {
            // 양쪽 모두 공간이 부족하면 오른쪽에 표시하고 화면 경계에 맞춤
            popupLeft = linkRect.right + 10;
            // 화면 오른쪽 경계를 넘지 않도록 조정
            if (popupLeft + popupWidth > viewportWidth) {
                popupLeft = viewportWidth - popupWidth - 10;
            }
            // 화면 왼쪽 경계를 넘지 않도록 조정
            if (popupLeft < 10) {
                popupLeft = 10;
            }
        }

        // 위/아래 위치 결정
        if (spaceOnBottom >= popupHeight) {
            // 아래에 공간이 충분하면 상단 정렬
            popupTop = linkRect.top;
        } else if (spaceOnTop >= popupHeight) {
            // 위에 공간이 충분하면 하단 정렬
            popupTop = linkRect.bottom - popupHeight;
        } else {
            // 위아래 모두 공간이 부족하면 화면 중앙에 맞춤
            if (spaceOnBottom < spaceOnTop) {
                // 화면 하단에 가까우면 위로 표시
                popupTop = linkRect.bottom - popupHeight;
                // 화면 상단을 넘지 않도록 조정
                if (popupTop < 10) {
                    popupTop = 10;
                }
            } else {
                // 화면 상단에 가까우면 아래로 표시
                popupTop = linkRect.top;
                // 화면 하단을 넘지 않도록 조정
                if (popupTop + popupHeight > viewportHeight - 10) {
                    popupTop = viewportHeight - popupHeight - 10;
                }
            }
        }

        // position: fixed는 viewport 기준이므로 스크롤 위치를 더할 필요 없음
        popup.style.left = popupLeft + 'px';
        popup.style.top = popupTop + 'px';
        popup.style.right = 'auto';
        popup.style.bottom = 'auto';
        popup.style.transform = '';
    }

    document.addEventListener('DOMContentLoaded', function() {
        const links = document.querySelectorAll('.item-link');

        links.forEach(link => {
            const popup = link.querySelector('.image-popup');
            const dataSpan = link.querySelector('.popup-data');
            const imgTag = popup ? popup.querySelector('img') : null;

            if (popup && dataSpan && imgTag) {
                // 이미지 설정 - 데이터가 준비될 때까지 대기
                function setImage() {
                    const description = dataSpan.innerHTML || '';
                    if (description && description.trim() !== '') {
                        imgTag.src = extractImagePath(description);
                    } else {
                        // 데이터가 아직 준비되지 않은 경우 재시도
                        setTimeout(setImage, 100);
                    }
                }
                setImage();
            }

            // 마우스 오버 이벤트
            if (link && popup) {
                link.addEventListener('mouseenter', function() {
                    // 이미지 경로 추출 및 업데이트
                    if (dataSpan && imgTag) {
                        const description = dataSpan.innerHTML || '';
                        if (description) {
                            imgTag.src = extractImagePath(description);
                        }
                    }

                    popup.style.display = 'block';
                    // 팝업이 표시된 후 위치 조정
                    requestAnimationFrame(function() {
                        adjustPopupPosition(link, popup);
                        // 이미지 로드 후에도 위치 재조정
                        const img = popup.querySelector('img');
                        if (img && !img.complete) {
                            img.addEventListener('load', function() {
                                adjustPopupPosition(link, popup);
                            }, { once: true });
                        }
                    });
                });

                link.addEventListener('mouseleave', function() {
                    popup.style.display = 'none';
                });
            }
        });

        // 창 크기 변경 및 스크롤 시에도 위치 재조정
        let resizeTimeout;
        window.addEventListener('resize', function() {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(function() {
                links.forEach(link => {
                    const popup = link.querySelector('.image-popup');
                    if (popup && popup.style.display === 'block') {
                        adjustPopupPosition(link, popup);
                    }
                });
            }, 100);
        });

        window.addEventListener('scroll', function() {
            links.forEach(link => {
                const popup = link.querySelector('.image-popup');
                if (popup && popup.style.display === 'block') {
                    adjustPopupPosition(link, popup);
                }
            });
        }, true);
    });

    // 추가 보험: window.load 후에도 실행 (ALL 카테고리 초기 로드 보장)
    window.addEventListener('load', function() {
        setTimeout(function() {
            const links = document.querySelectorAll('.item-link');
            links.forEach(link => {
                const popup = link.querySelector('.image-popup');
                const dataSpan = link.querySelector('.popup-data');
                const imgTag = popup ? popup.querySelector('img') : null;
                if (popup && dataSpan && imgTag) {
                    const description = dataSpan.innerHTML || '';
                    if (description && description.trim() !== '') {
                        imgTag.src = extractImagePath(description);
                    }
                }
            });
        }, 100);
    });
</script>

<%@ include file="../common/IncludeBottom.jsp"%>
