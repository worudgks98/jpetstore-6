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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<div id="Catalog">

  <h2>AI Recommendations</h2>

  <c:if test="${!sessionScope.accountBean.authenticated}">
    <div class="Message">Please sign in to view recommendations.</div>
  </c:if>

  <c:if test="${sessionScope.accountBean.authenticated}">
    <c:if test="${empty actionBean.recommendedItems}">
      <div class="Message">No recommendations available.</div>
    </c:if>
    <c:if test="${not empty actionBean.recommendedItems}">

      <div class="table-container" style="position: relative; width: max-content; margin: 0 auto;">

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

        <table class="itemList"> <tr>
            <th>Item ID</th>
            <th>Product ID</th>
            <th>Description</th>
            <th>List Price</th>
            <th>&nbsp;</th>
          </tr>
          <c:forEach var="item" items="${actionBean.recommendedItems}">
            <tr>
              <td>
                <%-- ★★★ 팝업을 위한 구조 변경 시작 ★★★ --%>
                <stripes:link
                  beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean"
                  event="viewItem"
                  class="item-link"> <stripes:param name="itemId" value="${item.itemId}" />
                  ${item.itemId}

                  <div class="image-popup">
                      <img src="/jpetstore/images/placeholder.gif" alt="Item Image" />

                      <div class="recommend-text">
                          <c:set var="recommendation" value="${actionBean.recommendationMessageMap[item.product.productId]}" />
                          <c:choose>
                              <c:when test="${not empty recommendation}">
                                  <div class="ai-copy ${recommendation.recommended ? 'RECOMMEND' : 'NOT_RECOMMEND'}">
                                      <div class="ai-copy-body">
                                          <c:out value="${recommendation.message}" escapeXml="false" />
                                      </div>
                                  </div>
                              </c:when>
                              <c:otherwise>
                                  <c:if test="${sessionScope.accountBean.authenticated}">
                                      <div class="ai-copy neutral">
                                          설문 답변을 반영한 추천 문구를 불러오는 중입니다.
                                      </div>
                                  </c:if>
                              </c:otherwise>
                          </c:choose>
                      </div>
                  </div>

                  <span class="popup-data" style="display: none;">
                      <c:out value="${item.product.description}" escapeXml="false" />
                  </span>

                </stripes:link>
                <%-- ★★★ 팝업을 위한 구조 변경 끝 ★★★ --%>
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
        </table>
      </div>

    </c:if>
  </c:if>

</div>

<%-- ★★★ 이미지 팝업 및 위치 조정 스크립트 ★★★ --%>
<script>
    // 이미지 경로 추출 함수
    function extractImagePath(desc) {
        if (!desc) return '/jpetstore/images/placeholder.gif';
        let match = desc.match(/<(?:image|img)[^>]+src\s*=\s*["']([^"']+)["']/i);
        if (match && match[1]) {
            let imgPath = match[1];
            if (imgPath.startsWith('../')) {
                return imgPath.replace('../', '/jpetstore/');
            }
            if (!imgPath.startsWith('/')) {
                return '/jpetstore/' + imgPath;
            }
            return imgPath;
        }
        return '/jpetstore/images/placeholder.gif';
    }

    // 팝업 위치 자동 조정 함수
    function adjustPopupPosition(link, popup) {
        const linkRect = link.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const popupRect = popup.getBoundingClientRect();
        const popupWidth = popupRect.width || 250;
        const popupHeight = popupRect.height || 300;

        const spaceOnLeft = linkRect.left;
        const spaceOnRight = viewportWidth - linkRect.right;
        const spaceOnTop = linkRect.top;
        const spaceOnBottom = viewportHeight - linkRect.bottom;

        let popupLeft = linkRect.left - popupWidth - 10;
        let popupTop = linkRect.top;

        // 좌우 위치 결정
        if (spaceOnLeft >= popupWidth + 10) {
            popupLeft = linkRect.left - popupWidth - 10;
        } else if (spaceOnRight >= popupWidth + 10) {
            popupLeft = linkRect.right + 10;
        } else {
            popupLeft = linkRect.right + 10;
            if (popupLeft + popupWidth > viewportWidth) popupLeft = viewportWidth - popupWidth - 10;
            if (popupLeft < 10) popupLeft = 10;
        }

        // 상하 위치 결정
        if (spaceOnBottom >= popupHeight) {
            popupTop = linkRect.top;
        } else if (spaceOnTop >= popupHeight) {
            popupTop = linkRect.bottom - popupHeight;
        } else {
            if (spaceOnBottom < spaceOnTop) {
                popupTop = linkRect.bottom - popupHeight;
                if (popupTop < 10) popupTop = 10;
            } else {
                popupTop = linkRect.top;
                if (popupTop + popupHeight > viewportHeight - 10) popupTop = viewportHeight - popupHeight - 10;
            }
        }

        popup.style.left = popupLeft + 'px';
        popup.style.top = popupTop + 'px';
        popup.style.right = 'auto';
        popup.style.bottom = 'auto';
        popup.style.transform = '';
    }

    document.addEventListener('DOMContentLoaded', function() {
        const links = document.querySelectorAll('#Catalog .item-link');

        links.forEach(link => {
            const popup = link.querySelector('.image-popup');
            const dataSpan = link.querySelector('.popup-data');
            const imgTag = popup ? popup.querySelector('img') : null;

            // 이미지 로딩
            if (popup && dataSpan && imgTag) {
                function setImage() {
                    const description = dataSpan.innerHTML || '';
                    if (description && description.trim() !== '') {
                        imgTag.src = extractImagePath(description);
                    } else {
                        setTimeout(setImage, 100);
                    }
                }
                setImage();
            }

            // 마우스 이벤트
            if (link && popup) {
                link.addEventListener('mouseenter', function() {
                    if (dataSpan && imgTag) {
                        const description = dataSpan.innerHTML || '';
                        if (description) imgTag.src = extractImagePath(description);
                    }
                    popup.style.display = 'block';

                    // 위치 조정
                    requestAnimationFrame(function() {
                        adjustPopupPosition(link, popup);
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

        // 스크롤/리사이즈 시 위치 재조정
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
</script>

<%@ include file="../common/IncludeBottom.jsp"%>
