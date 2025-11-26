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
package org.mybatis.jpetstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.jpetstore.domain.Category;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.domain.SurveyRecommendation;
import org.mybatis.jpetstore.mapper.CategoryMapper;
import org.mybatis.jpetstore.mapper.ItemMapper;
import org.mybatis.jpetstore.mapper.ProductMapper;
import org.mybatis.jpetstore.mapper.SurveyRecommendationMapper;
import org.springframework.stereotype.Service;

/**
 * The Class CatalogService.
 *
 * @author Eduardo Macarron
 */
@Service
public class CatalogService {

  // Weight values for each survey condition (total = 10.0)
  private static final double WEIGHT_RESIDENCE_ENV = 3.0; // Most important (30%)
  private static final double WEIGHT_PET_SIZE_PREF = 2.5; // Very important (25%)
  private static final double WEIGHT_CARE_PERIOD = 1.5; // Important (15%)
  private static final double WEIGHT_DIET_MANAGEMENT = 1.0; // Moderate (10%)
  private static final double WEIGHT_ACTIVITY_TIME = 1.0; // Moderate (10%)
  private static final double WEIGHT_PET_COLOR_PREF = 1.0; // Least important (10%)
  private static final double RECOMMENDATION_THRESHOLD = 7.5; // 75% of total weight (10.0) - stricter criteria

  private final CategoryMapper categoryMapper;
  private final ItemMapper itemMapper;
  private final ProductMapper productMapper;
  private final SurveyRecommendationMapper surveyRecommendationMapper;
  private final OpenAiRecommendationService openAiRecommendationService;

  public CatalogService(CategoryMapper categoryMapper, ItemMapper itemMapper, ProductMapper productMapper,
      SurveyRecommendationMapper surveyRecommendationMapper, OpenAiRecommendationService openAiRecommendationService) {
    this.categoryMapper = categoryMapper;
    this.itemMapper = itemMapper;
    this.productMapper = productMapper;
    this.surveyRecommendationMapper = surveyRecommendationMapper;
    this.openAiRecommendationService = openAiRecommendationService;
  }

  public List<Category> getCategoryList() {
    return categoryMapper.getCategoryList();
  }

  public Category getCategory(String categoryId) {
    return categoryMapper.getCategory(categoryId);
  }

  public Product getProduct(String productId) {
    return productMapper.getProduct(productId);
  }

  public List<Product> getProductListByCategory(String categoryId) {
    return productMapper.getProductListByCategory(categoryId);
  }

  public List<Product> getAllProducts() {
    return productMapper.getAllProducts();
  }

  /**
   * Search product list.
   *
   * @param keywords
   *          the keywords
   *
   * @return the list
   */
  public List<Product> searchProductList(String keywords) {
    List<Product> products = new ArrayList<>();
    for (String keyword : keywords.split("\\s+")) {
      products.addAll(productMapper.searchProductList("%" + keyword.toLowerCase() + "%"));
    }
    return products;
  }

  public List<Item> getItemListByProduct(String productId) {
    return itemMapper.getItemListByProduct(productId);
  }

  public Item getItem(String itemId) {
    return itemMapper.getItem(itemId);
  }

  public boolean isItemInStock(String itemId) {
    return itemMapper.getInventoryQuantity(itemId) > 0;
  }

  // 새로 추가한 모든 동물 가져오는 메소드
  public List<Item> getAllItemList() {
    return itemMapper.getAllItemList();
  }

  // 이것도 product 기준이 아니라 item 기준으로 수정!
  // public List<Product> getAllProductList() {
  // return productMapper.getAllProductList(); // ProductMapper에게 모든 상품을 달라고 요청
  // }

  /**
   * Check if the account has completed the survey (all 6 survey preferences are filled).
   *
   * @param account
   *          the account to check
   *
   * @return true if the account has completed the survey, false otherwise
   */
  public boolean hasCompletedSurvey(org.mybatis.jpetstore.domain.Account account) {
    if (account == null) {
      return false;
    }

    // Check if account has all required survey preferences
    String residenceEnv = account.getResidenceEnv();
    String carePeriod = account.getCarePeriod();
    String petColorPref = account.getPetColorPref();
    String petSizePref = account.getPetSizePref();
    String activityTime = account.getActivityTime();
    String dietManagement = account.getDietManagement();

    // All 6 survey fields must be non-null and non-empty
    return residenceEnv != null && !residenceEnv.isEmpty() && carePeriod != null && !carePeriod.isEmpty()
        && petColorPref != null && !petColorPref.isEmpty() && petSizePref != null && !petSizePref.isEmpty()
        && activityTime != null && !activityTime.isEmpty() && dietManagement != null && !dietManagement.isEmpty();
  }

  /**
   * Check if a product is recommended for the given account based on survey preferences. A product is recommended if
   * the weighted score of matching conditions is at least 6.5 out of 10.0 (65%).
   *
   * @param account
   *          the account with survey preferences
   * @param productId
   *          the product ID to check
   *
   * @return true if the product is recommended, false otherwise
   */
  public boolean isProductRecommended(org.mybatis.jpetstore.domain.Account account, String productId) {
    if (account == null || productId == null) {
      return false;
    }

    // First check if account has completed the survey
    if (!hasCompletedSurvey(account)) {
      return false;
    }

    // Get survey preferences (already validated in hasCompletedSurvey)
    String residenceEnv = account.getResidenceEnv();
    String carePeriod = account.getCarePeriod();
    String petColorPref = account.getPetColorPref();
    String petSizePref = account.getPetSizePref();
    String activityTime = account.getActivityTime();
    String dietManagement = account.getDietManagement();

    try {
      // Get product to check its category
      Product product = productMapper.getProduct(productId);
      if (product == null) {
        System.out.println("=== Product not found for productId: " + productId + " ===");
        return false;
      }
      String categoryId = product.getCategoryId();

      // Logic check: Fish products (FI-FW-*, FI-SW-*) should not be recommended for Dry environment
      if (residenceEnv != null && residenceEnv.trim().equals("Dry environment")) {
        if (productId.startsWith("FI-FW-") || productId.startsWith("FI-SW-")) {
          System.out.println("=== Fish product " + productId + " excluded for Dry environment ===");
          return false;
        }
      }

      // Get all survey recommendations
      List<SurveyRecommendation> allRecommendations = surveyRecommendationMapper.getSurveyRecommendations();

      // Debug: Log user's survey preferences
      System.out.println("=== Checking recommendation for productId: " + productId + " ===");
      System.out.println("User preferences:");
      System.out.println("  residenceEnv: [" + residenceEnv + "]");
      System.out.println("  carePeriod: [" + carePeriod + "]");
      System.out.println("  petColorPref: [" + petColorPref + "]");
      System.out.println("  petSizePref: [" + petSizePref + "]");
      System.out.println("  activityTime: [" + activityTime + "]");
      System.out.println("  dietManagement: [" + dietManagement + "]");
      System.out.println("Total recommendations in DB: " + allRecommendations.size());

      ObjectMapper mapper = new ObjectMapper();

      // Check each recommendation using weighted scoring (threshold: 6.5/10.0)
      for (SurveyRecommendation recommendation : allRecommendations) {
        if (recommendation.getRecommendedJsonData() == null) {
          continue;
        }

        // Calculate weighted score for matching conditions
        double weightedScore = 0.0;
        if (residenceEnv != null && recommendation.getResidenceEnv() != null
            && residenceEnv.trim().equals(recommendation.getResidenceEnv().trim())) {
          weightedScore += WEIGHT_RESIDENCE_ENV;
          System.out.println("    ✓ residenceEnv match: [" + residenceEnv.trim() + "] = ["
              + recommendation.getResidenceEnv().trim() + "] (+" + WEIGHT_RESIDENCE_ENV + ")");
        } else {
          System.out.println("    ✗ residenceEnv mismatch: user=[" + residenceEnv + "] vs DB=["
              + recommendation.getResidenceEnv() + "]");
        }
        if (petSizePref != null && recommendation.getPetSizePref() != null
            && petSizePref.trim().equals(recommendation.getPetSizePref().trim())) {
          weightedScore += WEIGHT_PET_SIZE_PREF;
          System.out.println("    ✓ petSizePref match: [" + petSizePref.trim() + "] = ["
              + recommendation.getPetSizePref().trim() + "] (+" + WEIGHT_PET_SIZE_PREF + ")");
        } else {
          System.out.println(
              "    ✗ petSizePref mismatch: user=[" + petSizePref + "] vs DB=[" + recommendation.getPetSizePref() + "]");
        }
        if (carePeriod != null && recommendation.getCarePeriod() != null
            && carePeriod.trim().equals(recommendation.getCarePeriod().trim())) {
          weightedScore += WEIGHT_CARE_PERIOD;
          System.out.println("    ✓ carePeriod match: [" + carePeriod.trim() + "] = ["
              + recommendation.getCarePeriod().trim() + "] (+" + WEIGHT_CARE_PERIOD + ")");
        } else {
          System.out.println(
              "    ✗ carePeriod mismatch: user=[" + carePeriod + "] vs DB=[" + recommendation.getCarePeriod() + "]");
        }
        if (dietManagement != null && recommendation.getDietManagement() != null
            && dietManagement.trim().equals(recommendation.getDietManagement().trim())) {
          weightedScore += WEIGHT_DIET_MANAGEMENT;
          System.out.println("    ✓ dietManagement match: [" + dietManagement.trim() + "] = ["
              + recommendation.getDietManagement().trim() + "] (+" + WEIGHT_DIET_MANAGEMENT + ")");
        } else {
          System.out.println("    ✗ dietManagement mismatch: user=[" + dietManagement + "] vs DB=["
              + recommendation.getDietManagement() + "]");
        }
        if (activityTime != null && recommendation.getActivityTime() != null
            && activityTime.trim().equals(recommendation.getActivityTime().trim())) {
          weightedScore += WEIGHT_ACTIVITY_TIME;
          System.out.println("    ✓ activityTime match: [" + activityTime.trim() + "] = ["
              + recommendation.getActivityTime().trim() + "] (+" + WEIGHT_ACTIVITY_TIME + ")");
        } else {
          System.out.println("    ✗ activityTime mismatch: user=[" + activityTime + "] vs DB=["
              + recommendation.getActivityTime() + "]");
        }
        if (petColorPref != null && recommendation.getPetColorPref() != null
            && petColorPref.trim().equals(recommendation.getPetColorPref().trim())) {
          weightedScore += WEIGHT_PET_COLOR_PREF;
          System.out.println("    ✓ petColorPref match: [" + petColorPref.trim() + "] = ["
              + recommendation.getPetColorPref().trim() + "] (+" + WEIGHT_PET_COLOR_PREF + ")");
        } else {
          System.out.println("    ✗ petColorPref mismatch: user=[" + petColorPref + "] vs DB=["
              + recommendation.getPetColorPref() + "]");
        }

        // Debug: Log weighted score for ALL recommendations (to debug threshold issues)
        System.out.println("Recommendation ID " + recommendation.getSurveyRecommendationId() + " weighted score: "
            + String.format("%.2f", weightedScore) + "/10.0 (threshold: " + RECOMMENDATION_THRESHOLD + ")");
        System.out.println("  DB values: [" + recommendation.getResidenceEnv() + "], [" + recommendation.getCarePeriod()
            + "], [" + recommendation.getPetColorPref() + "], [" + recommendation.getPetSizePref() + "], ["
            + recommendation.getActivityTime() + "], [" + recommendation.getDietManagement() + "]");
        if (weightedScore >= RECOMMENDATION_THRESHOLD) {
          System.out.println("  ✓ Score meets threshold - checking if product is in recommendation list");
        } else {
          System.out.println("  ✗ Score below threshold - skipping this recommendation");
        }

        // If weighted score meets threshold (6.5/10.0), check if productId is in recommendations
        // Use strict comparison: must be >= 6.5 (not just >= 6.0)
        if (weightedScore >= RECOMMENDATION_THRESHOLD) {
          System.out.println("  ✓ Score " + String.format("%.2f", weightedScore) + " >= threshold "
              + RECOMMENDATION_THRESHOLD + " - checking product list");
          JsonNode jsonArray = mapper.readTree(recommendation.getRecommendedJsonData());

          if (jsonArray.isArray()) {
            System.out.println("  Checking JSON array for productId: " + productId);
            for (JsonNode node : jsonArray) {
              if (node.has("productId")) {
                String recProductId = node.get("productId").asText();
                System.out.println("    Found productId in recommendation: " + recProductId);
                if (productId.equals(recProductId)) {
                  System.out.println("    *** MATCH FOUND! Product is RECOMMENDED (score: "
                      + String.format("%.2f", weightedScore) + ") ***");
                  return true;
                }
              }
            }
            System.out.println("  ProductId " + productId + " not found in this recommendation's JSON");
          }
        } else {
          System.out.println("  ✗ Score " + String.format("%.2f", weightedScore) + " < threshold "
              + RECOMMENDATION_THRESHOLD + " - skipping");
        }
      }
      System.out.println("=== No recommendation found for productId: " + productId + " ===");
    } catch (Exception e) {
      // Log error but don't break the page
      System.err.println("Error checking product recommendation for productId: " + productId);
      System.err.println("Error message: " + e.getMessage());
      System.err.println("Error class: " + e.getClass().getName());
      e.printStackTrace();
      return false;
    }

    return false;
  }

  /**
   * Get recommendation message for a product using LLM. Only matching conditions are included in the message.
   *
   * @param account
   *          the account with survey preferences
   * @param productId
   *          the product ID to get recommendation message for
   * @param isRecommended
   *          whether the product is recommended
   *
   * @return a personalized recommendation message
   */
  public String getRecommendationMessage(org.mybatis.jpetstore.domain.Account account, String productId,
      boolean isRecommended) {
    if (account == null || productId == null) {
      return isRecommended ? "We recommend this pet." : "This pet may not be the best match.";
    }

    try {
      Product product = productMapper.getProduct(productId);
      if (product == null) {
        return isRecommended ? "We recommend this pet." : "This pet may not be the best match.";
      }

      // For recommended products, find matching conditions
      // For not recommended products, find mismatching conditions to explain why
      java.util.Set<String> conditionsToMention;
      if (isRecommended) {
        conditionsToMention = findMatchingConditions(account, productId);
      } else {
        conditionsToMention = findMismatchingConditions(account, productId);
      }

      return openAiRecommendationService.generateRecommendationMessage(account, product, isRecommended,
          conditionsToMention);
    } catch (Exception e) {
      System.err.println("Error generating recommendation message: " + e.getMessage());
      return isRecommended ? "We recommend this pet based on your preferences."
          : "This pet may not be the best match for your preferences.";
    }
  }

  /**
   * Find which survey conditions matched for the given product recommendation.
   *
   * @param account
   *          the account with survey preferences
   * @param productId
   *          the product ID to check
   *
   * @return a set of matching condition names (e.g., "residenceEnv", "petSizePref")
   */
  private java.util.Set<String> findMatchingConditions(org.mybatis.jpetstore.domain.Account account, String productId) {
    java.util.Set<String> matchingConditions = new java.util.HashSet<>();

    if (account == null || productId == null || !hasCompletedSurvey(account)) {
      return matchingConditions;
    }

    String residenceEnv = account.getResidenceEnv();
    String carePeriod = account.getCarePeriod();
    String petColorPref = account.getPetColorPref();
    String petSizePref = account.getPetSizePref();
    String activityTime = account.getActivityTime();
    String dietManagement = account.getDietManagement();

    try {
      List<SurveyRecommendation> allRecommendations = surveyRecommendationMapper.getSurveyRecommendations();
      ObjectMapper mapper = new ObjectMapper();

      // Find the recommendation that matched this product
      for (SurveyRecommendation recommendation : allRecommendations) {
        if (recommendation.getRecommendedJsonData() == null) {
          continue;
        }

        // Calculate weighted score and check which conditions matched
        double weightedScore = 0.0;
        if (residenceEnv != null && recommendation.getResidenceEnv() != null
            && residenceEnv.trim().equals(recommendation.getResidenceEnv().trim())) {
          weightedScore += WEIGHT_RESIDENCE_ENV;
        }
        if (petSizePref != null && recommendation.getPetSizePref() != null
            && petSizePref.trim().equals(recommendation.getPetSizePref().trim())) {
          weightedScore += WEIGHT_PET_SIZE_PREF;
        }
        if (carePeriod != null && recommendation.getCarePeriod() != null
            && carePeriod.trim().equals(recommendation.getCarePeriod().trim())) {
          weightedScore += WEIGHT_CARE_PERIOD;
        }
        if (dietManagement != null && recommendation.getDietManagement() != null
            && dietManagement.trim().equals(recommendation.getDietManagement().trim())) {
          weightedScore += WEIGHT_DIET_MANAGEMENT;
        }
        if (activityTime != null && recommendation.getActivityTime() != null
            && activityTime.trim().equals(recommendation.getActivityTime().trim())) {
          weightedScore += WEIGHT_ACTIVITY_TIME;
        }
        if (petColorPref != null && recommendation.getPetColorPref() != null
            && petColorPref.trim().equals(recommendation.getPetColorPref().trim())) {
          weightedScore += WEIGHT_PET_COLOR_PREF;
        }

        // If score meets threshold, check if productId is in recommendations
        if (weightedScore >= RECOMMENDATION_THRESHOLD) {
          JsonNode jsonArray = mapper.readTree(recommendation.getRecommendedJsonData());
          if (jsonArray.isArray()) {
            for (JsonNode node : jsonArray) {
              if (node.has("productId")) {
                String recProductId = node.get("productId").asText();
                if (productId.equals(recProductId)) {
                  // Found the matching recommendation - collect matching conditions
                  if (residenceEnv != null && recommendation.getResidenceEnv() != null
                      && residenceEnv.trim().equals(recommendation.getResidenceEnv().trim())) {
                    matchingConditions.add("residenceEnv");
                  }
                  if (petSizePref != null && recommendation.getPetSizePref() != null
                      && petSizePref.trim().equals(recommendation.getPetSizePref().trim())) {
                    matchingConditions.add("petSizePref");
                  }
                  if (carePeriod != null && recommendation.getCarePeriod() != null
                      && carePeriod.trim().equals(recommendation.getCarePeriod().trim())) {
                    matchingConditions.add("carePeriod");
                  }
                  if (dietManagement != null && recommendation.getDietManagement() != null
                      && dietManagement.trim().equals(recommendation.getDietManagement().trim())) {
                    matchingConditions.add("dietManagement");
                  }
                  if (activityTime != null && recommendation.getActivityTime() != null
                      && activityTime.trim().equals(recommendation.getActivityTime().trim())) {
                    matchingConditions.add("activityTime");
                  }
                  if (petColorPref != null && recommendation.getPetColorPref() != null
                      && petColorPref.trim().equals(recommendation.getPetColorPref().trim())) {
                    matchingConditions.add("petColorPref");
                  }
                  return matchingConditions; // Return immediately after finding match
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error finding matching conditions: " + e.getMessage());
    }

    return matchingConditions;
  }

  /**
   * Find which survey conditions did NOT match for the given product (for not recommended products). This helps explain
   * why the product is not recommended.
   *
   * @param account
   *          the account with survey preferences
   * @param productId
   *          the product ID to check
   *
   * @return a set of mismatching condition names (e.g., "residenceEnv", "petSizePref")
   */
  private java.util.Set<String> findMismatchingConditions(org.mybatis.jpetstore.domain.Account account,
      String productId) {
    java.util.Set<String> mismatchingConditions = new java.util.HashSet<>();

    if (account == null || productId == null || !hasCompletedSurvey(account)) {
      return mismatchingConditions;
    }

    String residenceEnv = account.getResidenceEnv();
    String carePeriod = account.getCarePeriod();
    String petColorPref = account.getPetColorPref();
    String petSizePref = account.getPetSizePref();
    String activityTime = account.getActivityTime();
    String dietManagement = account.getDietManagement();

    try {
      List<SurveyRecommendation> allRecommendations = surveyRecommendationMapper.getSurveyRecommendations();
      ObjectMapper mapper = new ObjectMapper();

      // Find recommendations that include this product but didn't match user's conditions
      for (SurveyRecommendation recommendation : allRecommendations) {
        if (recommendation.getRecommendedJsonData() == null) {
          continue;
        }

        // Check if this product is in the recommendation
        JsonNode jsonArray = mapper.readTree(recommendation.getRecommendedJsonData());
        if (jsonArray.isArray()) {
          boolean productFound = false;
          for (JsonNode node : jsonArray) {
            if (node.has("productId")) {
              String recProductId = node.get("productId").asText();
              if (productId.equals(recProductId)) {
                productFound = true;
                break;
              }
            }
          }

          if (productFound) {
            // This product is in a recommendation, but user's conditions didn't match
            // Find which conditions didn't match
            if (residenceEnv != null && recommendation.getResidenceEnv() != null
                && !residenceEnv.trim().equals(recommendation.getResidenceEnv().trim())) {
              mismatchingConditions.add("residenceEnv");
            }
            if (petSizePref != null && recommendation.getPetSizePref() != null
                && !petSizePref.trim().equals(recommendation.getPetSizePref().trim())) {
              mismatchingConditions.add("petSizePref");
            }
            if (carePeriod != null && recommendation.getCarePeriod() != null
                && !carePeriod.trim().equals(recommendation.getCarePeriod().trim())) {
              mismatchingConditions.add("carePeriod");
            }
            if (dietManagement != null && recommendation.getDietManagement() != null
                && !dietManagement.trim().equals(recommendation.getDietManagement().trim())) {
              mismatchingConditions.add("dietManagement");
            }
            if (activityTime != null && recommendation.getActivityTime() != null
                && !activityTime.trim().equals(recommendation.getActivityTime().trim())) {
              mismatchingConditions.add("activityTime");
            }
            if (petColorPref != null && recommendation.getPetColorPref() != null
                && !petColorPref.trim().equals(recommendation.getPetColorPref().trim())) {
              mismatchingConditions.add("petColorPref");
            }
            // Found at least one recommendation with this product, break after collecting mismatches
            break;
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error finding mismatching conditions: " + e.getMessage());
    }

    return mismatchingConditions;
  }
}

// test11
