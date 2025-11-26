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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.domain.RecommendationMessage;
import org.mybatis.jpetstore.mapper.ProductMapper;
import org.mybatis.jpetstore.mapper.RecommendationMessageMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationMessageService {

  private final RecommendationMessageMapper recommendationMessageMapper;
  private final ProductMapper productMapper;
  private final CatalogService catalogService;

  public RecommendationMessageService(RecommendationMessageMapper recommendationMessageMapper,
      ProductMapper productMapper, CatalogService catalogService) {
    this.recommendationMessageMapper = recommendationMessageMapper;
    this.productMapper = productMapper;
    this.catalogService = catalogService;
  }

  @Async("recommendationTaskExecutor")
  @Transactional
  public void refreshRecommendationsAsync(Account account) {
    refreshRecommendationsInternal(account);
  }

  @Transactional
  public void refreshRecommendations(Account account) {
    refreshRecommendationsInternal(account);
  }

  private void refreshRecommendationsInternal(Account account) {
    if (account == null || account.getUsername() == null) {
      System.out.println("=== refreshRecommendationsInternal: account or username is null ===");
      return;
    }

    System.out.println("=== Starting recommendation message generation for user: " + account.getUsername() + " ===");
    System.out.println("=== Survey data check ===");
    System.out
        .println("  residenceEnv: [" + (account.getResidenceEnv() != null ? account.getResidenceEnv() : "null") + "]");
    System.out.println("  carePeriod: [" + (account.getCarePeriod() != null ? account.getCarePeriod() : "null") + "]");
    System.out
        .println("  petColorPref: [" + (account.getPetColorPref() != null ? account.getPetColorPref() : "null") + "]");
    System.out
        .println("  petSizePref: [" + (account.getPetSizePref() != null ? account.getPetSizePref() : "null") + "]");
    System.out
        .println("  activityTime: [" + (account.getActivityTime() != null ? account.getActivityTime() : "null") + "]");
    System.out.println(
        "  dietManagement: [" + (account.getDietManagement() != null ? account.getDietManagement() : "null") + "]");

    // Delete old messages first
    recommendationMessageMapper.deleteMessagesForUser(account.getUsername());
    System.out.println("=== Deleted old recommendation messages ===");

    // Check if user has completed survey
    boolean surveyCompleted = catalogService.hasCompletedSurvey(account);
    System.out.println("=== Survey completed check: " + surveyCompleted + " ===");
    if (!surveyCompleted) {
      System.out.println("=== User has not completed survey - skipping recommendation generation ===");
      System.out.println("=== NOTE: All 6 survey fields must be filled (not empty) to generate recommendations ===");
      return;
    }

    // Get all products and generate recommendation messages for each
    List<Product> products = productMapper.getAllProducts();
    System.out.println("=== Found " + products.size() + " products to process ===");

    int processedCount = 0;
    for (Product product : products) {
      try {
        // Determine if product is recommended
        boolean recommended = catalogService.isProductRecommended(account, product.getProductId());

        // Generate AI recommendation message (this calls OpenAI API)
        String message = catalogService.getRecommendationMessage(account, product.getProductId(), recommended);

        // Create and save recommendation message
        RecommendationMessage recommendationMessage = new RecommendationMessage();
        recommendationMessage.setUsername(account.getUsername());
        recommendationMessage.setProductId(product.getProductId());
        recommendationMessage.setRecommended(recommended);
        recommendationMessage.setMessage(message);
        recommendationMessage.setLastUpdated(LocalDateTime.now());

        recommendationMessageMapper.insertRecommendationMessage(recommendationMessage);
        processedCount++;

        if (processedCount % 10 == 0) {
          System.out.println("=== Processed " + processedCount + " / " + products.size() + " products ===");
        }
      } catch (Exception e) {
        System.err.println("=== Error processing product " + product.getProductId() + ": " + e.getMessage() + " ===");
        e.printStackTrace();
        // Continue with next product even if one fails
      }
    }

    System.out.println("=== Completed recommendation message generation for user: " + account.getUsername()
        + " (processed " + processedCount + " / " + products.size() + " products) ===");
  }

  @Transactional(readOnly = true)
  public Map<String, RecommendationMessage> getRecommendationMessageMap(String username) {
    if (username == null) {
      return Collections.emptyMap();
    }

    List<RecommendationMessage> messages = recommendationMessageMapper.getMessagesForUser(username);
    if (messages.isEmpty()) {
      return Collections.emptyMap();
    }

    return messages.stream().collect(Collectors.toMap(RecommendationMessage::getProductId, Function.identity()));
  }

  @Transactional(readOnly = true)
  public RecommendationMessage getRecommendationMessage(String username, String productId) {
    if (username == null || productId == null) {
      return null;
    }
    return recommendationMessageMapper.getMessage(username, productId);
  }
}
