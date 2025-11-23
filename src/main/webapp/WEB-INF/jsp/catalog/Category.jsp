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

<div id="BackLink"><stripes:link
	beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean">
	Return to Main Menu</stripes:link></div>

<div id="Catalog">

<h2>${actionBean.category.name}</h2>

<%-- Check if user is logged in and has completed survey --%>
<c:set var="hasCompletedSurvey" value="false" />
<c:if test="${not empty sessionScope.accountBean and sessionScope.accountBean.authenticated and not empty actionBean.productRecommendationMap}">
	<c:set var="hasCompletedSurvey" value="true" />
</c:if>

<table>
	<tr>
		<th>Product ID</th>
		<th>Name</th>
	</tr>
	<c:forEach var="product" items="${actionBean.productList}">
		<c:set var="isRecommended" value="false" />
		<c:set var="recommendationMessage" value="" />
		<c:if test="${not empty actionBean.productRecommendationMap}">
			<c:set var="isRecommended" value="${actionBean.productRecommendationMap[product.productId]}" />
		</c:if>
		<c:if test="${not empty actionBean.productRecommendationMessageMap}">
			<c:set var="recommendationMessage" value="${actionBean.productRecommendationMessageMap[product.productId]}" />
		</c:if>
		<tr>
			<td><span class="product-link"
				data-product-id="${product.productId}"
				data-description="${fn:replace(product.description, '\"', '&quot;')}"
				data-category-id="${product.categoryId}"
				data-product-name="${product.name}"
				data-is-recommended="${isRecommended}"
				data-recommendation-message="${fn:replace(recommendationMessage, '\"', '&quot;')}"
				data-has-completed-survey="${hasCompletedSurvey}">
				<stripes:link
					beanclass="org.mybatis.jpetstore.web.actions.CatalogActionBean"
					event="viewProduct">
					<stripes:param name="productId" value="${product.productId}" />
					${product.productId}
				</stripes:link>
			</span></td>
			<td>${product.name}</td>
		</tr>
	</c:forEach>
</table>

<!-- Image Popup -->
<div id="productImagePopup" class="product-image-popup"></div>

</div>

<style>
.product-image-popup {
	position: absolute;
	display: none;
	z-index: 1000;
	background-color: white;
	border: 2px solid #333;
	border-radius: 5px;
	padding: 5px;
	box-shadow: 0 4px 8px rgba(0,0,0,0.3);
	pointer-events: none;
	max-width: 280px;
}

.product-image-popup img {
	max-width: 300px;
	max-height: 300px;
	display: block;
}

.product-image-popup .recommendation-text {
	text-align: center;
	padding: 5px 10px;
	font-size: 12px;
	font-weight: bold;
	margin-top: 5px;
	max-width: 250px;
	word-wrap: break-word;
	word-break: break-word;
	line-height: 1.4;
}

.product-image-popup .recommendation-text.recommendation-yes {
	color: #0066cc;
}

.product-image-popup .recommendation-text.recommendation-no {
	color: #cc0000;
}

.product-link {
	position: relative;
	cursor: pointer;
}
</style>

<script>
(function() {
	// Get image path by product ID (direct mapping)
	// dog1 = Bulldog, dog2 = Chihuahua, dog3 = Dalmation, dog4 = Labrador Retriever, dog5 = Golden Retriever, dog6 = Poodle
	function getImagePathByProductId(productId) {
		if (!productId) {
			return null;
		}

		// Normalize product ID (trim and uppercase)
		var normalizedId = productId.trim().toUpperCase();

		var productImageMap = {
			'K9-BD-01': '/jpetstore/images/dog1.gif',  // Bulldog -> dog1
			'K9-CW-01': '/jpetstore/images/dog2.gif',  // Chihuahua -> dog2
			'K9-DL-01': '/jpetstore/images/dog3.gif',  // Dalmation -> dog3
			'K9-RT-02': '/jpetstore/images/dog4.gif',  // Labrador Retriever -> dog4
			'K9-RT-01': '/jpetstore/images/dog5.gif',  // Golden Retriever -> dog5
			'K9-PO-02': '/jpetstore/images/dog6.gif'   // Poodle -> dog6
		};

		var imagePath = productImageMap[normalizedId];
		if (imagePath) {
			console.log('Found direct mapping for', normalizedId, '->', imagePath);
			return imagePath;
		}

		console.log('No direct mapping found for', normalizedId);
		return null;
	}

	// Extract image path from description (fallback)
	function extractImagePath(description) {
		if (!description) {
			return null;
		}

		// Try multiple patterns to find image path
		// Pattern 1: <image src="../images/xxx.gif"> or <image src='../images/xxx.gif'>
		var match = description.match(/<image\s+src\s*=\s*["']([^"']*\.\.\/images\/[^"']+)["']/i);
		if (match && match[1]) {
			var path = match[1];
			if (path.startsWith('../images/')) {
				return '/jpetstore/images/' + path.substring('../images/'.length);
			}
			return '/jpetstore/images/' + path;
		}

		// Pattern 2: <image src="../images/xxx.gif"> (without quotes)
		match = description.match(/<image\s+src\s*=\s*\.\.\/images\/([^\s>]+)/i);
		if (match && match[1]) {
			return '/jpetstore/images/' + match[1];
		}

		// Pattern 3: ../images/xxx.gif (standalone)
		match = description.match(/\.\.\/images\/([^\s"'>]+)/i);
		if (match && match[1]) {
			return '/jpetstore/images/' + match[1];
		}

		// Pattern 4: HTML escaped version &lt;image src=...
		match = description.match(/&lt;image\s+src\s*=\s*["']?\.\.\/images\/([^"'>\s]+)["']?/i);
		if (match && match[1]) {
			return '/jpetstore/images/' + match[1];
		}

		return null;
	}

	// Get category-based default image
	function getDefaultImage(categoryId) {
		var imageMap = {
			'FISH': '/jpetstore/images/fish1.gif',
			'DOGS': '/jpetstore/images/dog1.gif',
			'CATS': '/jpetstore/images/cat1.gif',
			'BIRDS': '/jpetstore/images/bird1.gif',
			'REPTILES': '/jpetstore/images/snake1.gif'
		};
		return imageMap[categoryId] || '/jpetstore/images/splash.gif';
	}

	var popup = document.getElementById('productImagePopup');
	var productLinks = document.querySelectorAll('.product-link');

	// Get pet type name from category
	function getPetTypeName(categoryId) {
		var typeMap = {
			'FISH': 'fish',
			'DOGS': 'dog',
			'CATS': 'cat',
			'BIRDS': 'bird',
			'REPTILES': 'reptile'
		};
		return typeMap[categoryId] || 'pet';
	}

	productLinks.forEach(function(link) {
		var description = link.getAttribute('data-description');
		var categoryId = link.getAttribute('data-category-id');
		var productId = link.getAttribute('data-product-id');
		var productName = link.getAttribute('data-product-name');
		var isRecommended = link.getAttribute('data-is-recommended') === 'true';
		var recommendationMessage = link.getAttribute('data-recommendation-message');

		link.addEventListener('mouseenter', function(e) {
			console.log('Product ID:', productId);
			console.log('Description:', description);
			console.log('Category ID:', categoryId);
			console.log('Is Recommended:', isRecommended);

			// Get survey completion status
			var hasCompletedSurvey = link.getAttribute('data-has-completed-survey') === 'true';
			console.log('Has Completed Survey:', hasCompletedSurvey);

			// FORCE: Get image by product ID (direct mapping) - ONLY use this for DOGS category
			var imagePath = null;
			if (categoryId === 'DOGS') {
				// For DOGS category, ONLY use product ID mapping
				imagePath = getImagePathByProductId(productId);
				console.log('Image path by product ID (DOGS):', imagePath);
			} else {
				// For other categories, try product ID first, then description
				imagePath = getImagePathByProductId(productId);
				if (!imagePath && description) {
					imagePath = extractImagePath(description);
					console.log('Extracted image path from description:', imagePath);
				}
			}

			// If still not found, use default image
			if (!imagePath) {
				imagePath = getDefaultImage(categoryId);
				console.log('Using default image:', imagePath);
			}

			if (imagePath) {
				console.log('Final image path:', imagePath);

				// Build popup content
				var popupContent = '<img src="' + imagePath + '" alt="Product Image" onerror="console.error(\'Image load failed:\', this.src); this.src=\'/jpetstore/images/splash.gif\'" />';

				// Add recommendation text ONLY if user has completed survey
				if (productName && hasCompletedSurvey) {
					var messageToShow = '';
					var actualIsRecommended = isRecommended;

					if (recommendationMessage && recommendationMessage.trim() !== '') {
						// Use LLM-generated recommendation message
						messageToShow = recommendationMessage;

						// Check if message contains "not recommend" or similar negative keywords
						// If so, override isRecommended to false for correct color
						var lowerMessage = messageToShow.toLowerCase();
						if (lowerMessage.includes('not recommend') ||
						    lowerMessage.includes('not the best') ||
						    lowerMessage.includes('may not be') ||
						    lowerMessage.includes('not ideal') ||
						    lowerMessage.includes('not suitable')) {
							actualIsRecommended = false;
						} else if (lowerMessage.includes('recommend') &&
						           !lowerMessage.includes('not recommend') &&
						           !lowerMessage.includes('don\'t recommend')) {
							// Message contains "recommend" but not "not recommend"
							actualIsRecommended = true;
						}
					} else {
						// Fallback to default message
						var petType = getPetTypeName(categoryId);
						if (isRecommended) {
							messageToShow = 'We recommend this ' + petType;
						} else {
							messageToShow = 'We don\'t recommend this ' + petType;
						}
					}

					var messageClass = actualIsRecommended ? 'recommendation-yes' : 'recommendation-no';
					popupContent += '<div class="recommendation-text ' + messageClass + '">' + messageToShow + '</div>';
				}
				// If hasCompletedSurvey is false, no recommendation text is shown

				popup.innerHTML = popupContent;
				popup.style.display = 'block';

				// Position popup near mouse cursor
				var rect = link.getBoundingClientRect();
				popup.style.left = (rect.right + 10) + 'px';
				popup.style.top = (rect.top + window.scrollY) + 'px';
			}
		});

		link.addEventListener('mouseleave', function(e) {
			popup.style.display = 'none';
		});

		link.addEventListener('mousemove', function(e) {
			if (popup.style.display === 'block') {
				// Update popup position to follow mouse
				popup.style.left = (e.clientX + 10) + 'px';
				popup.style.top = (e.clientY + 10 + window.scrollY) + 'px';
			}
		});
	});
})();
</script>

<%@ include file="../common/IncludeBottom.jsp"%>


