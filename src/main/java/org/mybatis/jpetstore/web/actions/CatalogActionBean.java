/*
 *    Copyright 2010-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore.web.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.integration.spring.SpringBean;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Category;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.domain.RecommendationMessage;
import org.mybatis.jpetstore.service.CatalogService;
import org.mybatis.jpetstore.service.RecommendationMessageService;

/**
 * The Class CatalogActionBean.
 *
 * @author Eduardo Macarron
 */
@SessionScope
public class CatalogActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 5849523372175050635L;

  private static final String MAIN = "/WEB-INF/jsp/catalog/Main.jsp";
  private static final String VIEW_CATEGORY = "/WEB-INF/jsp/catalog/Category.jsp";
  private static final String VIEW_PRODUCT = "/WEB-INF/jsp/catalog/Product.jsp";
  private static final String VIEW_ITEM = "/WEB-INF/jsp/catalog/Item.jsp";
  private static final String SEARCH_PRODUCTS = "/WEB-INF/jsp/catalog/SearchProducts.jsp";

  @SpringBean
  private transient CatalogService catalogService;
  @SpringBean
  private transient RecommendationMessageService recommendationMessageService;

  private String keyword;

  private String categoryId;
  private Category category;
  private List<Category> categoryList;

  private String productId;
  private Product product;
  private List<Product> productList;

  private String itemId;
  private Item item;
  private List<Item> itemList;

  private Map<String, Boolean> productRecommendationMap;
  private Map<String, String> productRecommendationMessageMap;
  private boolean userCompletedSurvey;
  private RecommendationMessage productRecommendationMessage;
  private Map<String, RecommendationMessage> itemRecommendationMessageMap;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public List<Category> getCategoryList() {
    return categoryList;
  }

  public void setCategoryList(List<Category> categoryList) {
    this.categoryList = categoryList;
  }

  public List<Product> getProductList() {
    return productList;
  }

  public void setProductList(List<Product> productList) {
    this.productList = productList;
  }

  public List<Item> getItemList() {
    return itemList;
  }

  public void setItemList(List<Item> itemList) {
    this.itemList = itemList;
  }

  public Map<String, Boolean> getProductRecommendationMap() {
    return productRecommendationMap;
  }

  public void setProductRecommendationMap(Map<String, Boolean> productRecommendationMap) {
    this.productRecommendationMap = productRecommendationMap;
  }

  public Map<String, String> getProductRecommendationMessageMap() {
    return productRecommendationMessageMap;
  }

  public void setProductRecommendationMessageMap(Map<String, String> productRecommendationMessageMap) {
    this.productRecommendationMessageMap = productRecommendationMessageMap;
  }

  public boolean isUserCompletedSurvey() {
    return userCompletedSurvey;
  }

  public RecommendationMessage getProductRecommendationMessage() {
    return productRecommendationMessage;
  }

  @DefaultHandler
  public ForwardResolution viewMain() {
    return new ForwardResolution(MAIN);
  }

  /**
   * View category.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewCategory() {
    userCompletedSurvey = false;
    // "ALL" 이라는 카테고리 아이디가 들어오면 새로운 메소드를 호출함
    if ("ALL".equals(categoryId)) {
      itemList = catalogService.getAllItemList();
      product = new Product();
      product.setCategoryId("ALL");

      // ALL 카테고리: 각 아이템의 productId별 추천 메시지 미리 로드
      itemRecommendationMessageMap = new HashMap<>();
      HttpSession session = context.getRequest().getSession(false);
      if (session != null) {
        AccountActionBean accountBean = (AccountActionBean) session.getAttribute("accountBean");
        if (accountBean != null && accountBean.isAuthenticated() && accountBean.getAccount() != null) {
          Account account = accountBean.getAccount();
          if (catalogService.hasCompletedSurvey(account)) {
            Map<String, RecommendationMessage> cachedMessages = recommendationMessageService
                .getRecommendationMessageMap(account.getUsername());

            // 각 아이템의 productId별로 추천 메시지 가져오기
            for (Item item : itemList) {
              String prodId = item.getProduct().getProductId();
              if (prodId != null && !itemRecommendationMessageMap.containsKey(prodId)) {
                RecommendationMessage message = cachedMessages.get(prodId);
                if (message == null) {
                  // 캐시에 없으면 새로 가져오기
                  message = recommendationMessageService.getRecommendationMessage(account.getUsername(), prodId);
                }
                if (message != null) {
                  itemRecommendationMessageMap.put(prodId, message);
                }
              }
            }
          }
        }
      }

      return new ForwardResolution(VIEW_PRODUCT);
      // 이건 세부 품목이 아니라 상품 분류를 전부 가져오는 기능이었음;; ex) 작은물고기, 중간물고기, 큰물고기(X), 물고기 (O)
      // productList = catalogService.getAllProductList();
      // category = new Category();
      // category.setName("ALL Products");
    }
    // 아니라면 평소 하던대로 진행
    else if (categoryId != null) {
      productList = catalogService.getProductListByCategory(categoryId);
      category = catalogService.getCategory(categoryId);

      // Check recommendations ONLY for logged-in users who have completed the survey
      productRecommendationMap = new HashMap<>();
      productRecommendationMessageMap = new HashMap<>();
      userCompletedSurvey = false;
      HttpSession session = context.getRequest().getSession();
      AccountActionBean accountBean = (AccountActionBean) session.getAttribute("accountBean");

      // Only create recommendation map if user is logged in
      if (accountBean != null && accountBean.isAuthenticated() && accountBean.getAccount() != null) {
        Account account = accountBean.getAccount();
        userCompletedSurvey = catalogService.hasCompletedSurvey(account);
        Map<String, RecommendationMessage> cachedMessages = recommendationMessageService
            .getRecommendationMessageMap(account.getUsername());

        for (Product product : productList) {
          RecommendationMessage cachedMessage = cachedMessages.get(product.getProductId());
          if (cachedMessage != null) {
            productRecommendationMap.put(product.getProductId(), cachedMessage.isRecommended());
            productRecommendationMessageMap.put(product.getProductId(), cachedMessage.getMessage());
            continue;
          }

          if (userCompletedSurvey) {
            boolean isRecommended = catalogService.isProductRecommended(account, product.getProductId());
            productRecommendationMap.put(product.getProductId(), isRecommended);

            String recommendationMessage = catalogService.getRecommendationMessage(account, product.getProductId(),
                isRecommended);
            productRecommendationMessageMap.put(product.getProductId(), recommendationMessage);
          }
        }
      }
    }
    return new ForwardResolution(VIEW_CATEGORY);
  }

  /**
   * View product.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewProduct() {
    if (productId != null) {
      itemList = catalogService.getItemListByProduct(productId);
      product = catalogService.getProduct(productId);
      productRecommendationMessage = resolveRecommendationMessage(productId);
    }
    return new ForwardResolution(VIEW_PRODUCT);
  }

  /**
   * View item.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewItem() {
    item = catalogService.getItem(itemId);
    product = item.getProduct();
    return new ForwardResolution(VIEW_ITEM);
  }

  /**
   * Search products.
   *
   * @return the forward resolution
   */
  public ForwardResolution searchProducts() {
    if (keyword == null || keyword.length() < 1) {
      setMessage("Please enter a keyword to search for, then press the search button.");
      return new ForwardResolution(ERROR);
    } else {
      productList = catalogService.searchProductList(keyword.toLowerCase());
      return new ForwardResolution(SEARCH_PRODUCTS);
    }
  }

  /**
   * Clear.
   */
  public void clear() {
    keyword = null;

    categoryId = null;
    category = null;
    categoryList = null;

    productId = null;
    product = null;
    productList = null;

    itemId = null;
    item = null;
    itemList = null;
    productRecommendationMessage = null;
    itemRecommendationMessageMap = null;
  }

  public Map<String, RecommendationMessage> getItemRecommendationMessageMap() {
    return itemRecommendationMessageMap;
  }

  private RecommendationMessage resolveRecommendationMessage(String currentProductId) {
    if (currentProductId == null) {
      return null;
    }
    HttpSession session = context.getRequest().getSession(false);
    if (session == null) {
      return null;
    }
    AccountActionBean accountBean = (AccountActionBean) session.getAttribute("accountBean");
    if (accountBean == null || !accountBean.isAuthenticated() || accountBean.getAccount() == null) {
      return null;
    }
    Account account = accountBean.getAccount();
    if (!catalogService.hasCompletedSurvey(account)) {
      return null;
    }
    return recommendationMessageService.getRecommendationMessage(account.getUsername(), currentProductId);
  }

}
