/*
 * Copyright 2010-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    if (account == null || account.getUsername() == null) {
      return;
    }

    recommendationMessageMapper.deleteMessagesForUser(account.getUsername());

    if (!catalogService.hasCompletedSurvey(account)) {
      return;
    }

    List<Product> products = productMapper.getAllProducts();
    for (Product product : products) {
      boolean recommended = catalogService.isProductRecommended(account, product.getProductId());
      String message = catalogService.getRecommendationMessage(account, product.getProductId(), recommended);

      RecommendationMessage recommendationMessage = new RecommendationMessage();
      recommendationMessage.setUsername(account.getUsername());
      recommendationMessage.setProductId(product.getProductId());
      recommendationMessage.setRecommended(recommended);
      recommendationMessage.setMessage(message);
      recommendationMessage.setLastUpdated(LocalDateTime.now());

      recommendationMessageMapper.insertRecommendationMessage(recommendationMessage);
    }
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
}
