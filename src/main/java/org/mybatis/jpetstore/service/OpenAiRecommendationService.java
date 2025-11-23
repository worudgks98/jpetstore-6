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
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenAiRecommendationService {

  private static final Logger logger = LoggerFactory.getLogger(OpenAiRecommendationService.class);
  private static final String OPENAI_API_KEY;
  private static final String OPENAI_MODEL = "gpt-3.5-turbo"; // Or "gpt-4", "gpt-4o" etc.

  private final OpenAiService openAiService;

  static {
    String apiKey = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      InputStream is = OpenAiRecommendationService.class.getClassLoader()
          .getResourceAsStream("credentials/openai_credentials.json");
      if (is == null) {
        throw new IOException("openai_credentials.json not found in classpath.");
      }
      JsonNode rootNode = mapper.readTree(is);
      apiKey = rootNode.path("openai_api_key").asText();
      if (apiKey.isEmpty() || "YOUR_OPENAI_API_KEY_HERE".equals(apiKey)) {
        throw new IllegalArgumentException(
            "OPENAI_API_KEY not found or is a placeholder in openai_credentials.json. Please update the file.");
      }
    } catch (IOException e) {
      logger.error("Error reading openai_credentials.json: {}", e.getMessage());
      throw new RuntimeException("Failed to load OpenAI API key from credentials file.", e);
    }
    OPENAI_API_KEY = apiKey;
  }

  public OpenAiRecommendationService() {
    // Set a timeout for the OpenAI API calls
    this.openAiService = new OpenAiService(OPENAI_API_KEY, Duration.ofSeconds(30));
  }

  public String getRecommendation(Account account, List<Product> productList) {
    try {
      StringBuilder promptBuilder = new StringBuilder();
      promptBuilder.append(
          "Based on the following user preferences and available products, recommend 2 to 5 products from the list.\n");
      promptBuilder.append("User Preferences:\n");
      // promptBuilder.append(" - Favourite Category: ").append(account.getFavouriteCategoryId()).append("\n");
      // promptBuilder.append(" - Language Preference: ").append(account.getLanguagePreference()).append("\n");
      promptBuilder.append("  - Living Environment: ").append(account.getResidenceEnv()).append("\n");
      promptBuilder.append("  - Pet Care Period: ").append(account.getCarePeriod()).append("\n");
      promptBuilder.append("  - Pet Color Preference: ").append(account.getPetColorPref()).append("\n");
      promptBuilder.append("  - Pet Size Preference: ").append(account.getPetSizePref()).append("\n");
      promptBuilder.append("  - Activity Time: ").append(account.getActivityTime()).append("\n");
      promptBuilder.append("  - Diet Management: ").append(account.getDietManagement()).append("\n");
      promptBuilder.append("\nAvailable Products (Product ID - Name - Category):\n");
      for (Product product : productList) {
        promptBuilder.append("  - ").append(product.getProductId()).append(" - ").append(product.getName())
            .append(" - ").append(product.getCategoryId()).append("\n");
      }
      promptBuilder.append(
          "\nBased on the 'User Preferences', you MUST recommend between 2 and 5 products from the 'Available Products' list above. DO NOT recommend fewer than 2 or more than 5 products. Output the recommendations as a JSON array of objects, where each object has 'productId' and 'productName' fields. Do not include any other text or explanation outside the JSON. Example output:\n"
              + "[\n" + "    {\"productId\": \"FI-FW-01\", \"productName\": \"Koi\"},\n"
              + "    {\"productId\": \"FI-FW-02\", \"productName\": \"Goldfish\"}\n" + "]");

      String fullPrompt = promptBuilder.toString();
      ChatMessage userMessage = new ChatMessage("user", fullPrompt);

      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model(OPENAI_MODEL)
          .messages(Collections.singletonList(userMessage)).maxTokens(500) // Limit the response length
          .temperature(0.7) // Creativity level
          .build();

      logger.info("Sending prompt to OpenAI API. Prompt length: {} characters", fullPrompt.length());
      logger.info("Full Prompt sent to OpenAI API:\n{}", fullPrompt);
      String response = openAiService.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage()
          .getContent();

      logger.info("OpenAI API Response: {}", response);
      return response;

    } catch (Exception e) {
      logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
      return "Error getting recommendation from OpenAI: " + e.getMessage();
    }
  }

  /**
   * Generate a personalized recommendation message explaining why a product is recommended or not. Only matching
   * conditions are included in the message.
   *
   * @param account
   *          the user account with survey preferences
   * @param product
   *          the product to generate recommendation message for
   * @param isRecommended
   *          whether the product is recommended or not
   * @param matchingConditions
   *          set of matching condition names (e.g., "residenceEnv", "petSizePref") - only these will be mentioned
   *
   * @return a personalized recommendation message
   */
  public String generateRecommendationMessage(Account account, Product product, boolean isRecommended,
      java.util.Set<String> matchingConditions) {
    try {
      StringBuilder promptBuilder = new StringBuilder();
      promptBuilder.append("You are a pet recommendation assistant. Generate a brief, friendly message explaining ");

      if (isRecommended) {
        promptBuilder.append("why this pet is recommended for the user based on their MATCHING preferences.\n\n");
        // For recommended products, show matching conditions
        promptBuilder.append("Matching User Preferences (ONLY mention these in your message):\n");
        if (matchingConditions.contains("residenceEnv") && account.getResidenceEnv() != null) {
          promptBuilder.append("  - Living Environment: ").append(account.getResidenceEnv()).append("\n");
        }
        if (matchingConditions.contains("carePeriod") && account.getCarePeriod() != null) {
          promptBuilder.append("  - Pet Care Period: ").append(account.getCarePeriod()).append("\n");
        }
        if (matchingConditions.contains("petColorPref") && account.getPetColorPref() != null) {
          promptBuilder.append("  - Pet Color Preference: ").append(account.getPetColorPref()).append("\n");
        }
        if (matchingConditions.contains("petSizePref") && account.getPetSizePref() != null) {
          promptBuilder.append("  - Pet Size Preference: ").append(account.getPetSizePref()).append("\n");
        }
        if (matchingConditions.contains("activityTime") && account.getActivityTime() != null) {
          promptBuilder.append("  - Activity Time: ").append(account.getActivityTime()).append("\n");
        }
        if (matchingConditions.contains("dietManagement") && account.getDietManagement() != null) {
          promptBuilder.append("  - Diet Management: ").append(account.getDietManagement()).append("\n");
        }
        if (matchingConditions.isEmpty()) {
          promptBuilder.append("  (No specific preferences matched)\n");
        }
      } else {
        promptBuilder.append("why this pet may not be the best match for the user. ");
        promptBuilder.append("Explain the specific reasons based on mismatching preferences.\n\n");
        // For not recommended products, show mismatching conditions to explain why
        promptBuilder.append("Mismatching User Preferences (explain why these don't match):\n");
        if (matchingConditions.contains("residenceEnv") && account.getResidenceEnv() != null) {
          promptBuilder.append("  - Living Environment: User has '").append(account.getResidenceEnv())
              .append("' but this pet requires different environment\n");
        }
        if (matchingConditions.contains("carePeriod") && account.getCarePeriod() != null) {
          promptBuilder.append("  - Pet Care Period: User prefers '").append(account.getCarePeriod())
              .append("' but this pet needs different care period\n");
        }
        if (matchingConditions.contains("petColorPref") && account.getPetColorPref() != null) {
          promptBuilder.append("  - Pet Color Preference: User prefers '").append(account.getPetColorPref())
              .append("' but this pet has different colors\n");
        }
        if (matchingConditions.contains("petSizePref") && account.getPetSizePref() != null) {
          promptBuilder.append("  - Pet Size Preference: User prefers '").append(account.getPetSizePref())
              .append("' but this pet is different size\n");
        }
        if (matchingConditions.contains("activityTime") && account.getActivityTime() != null) {
          promptBuilder.append("  - Activity Time: User has '").append(account.getActivityTime())
              .append("' but this pet needs different activity schedule\n");
        }
        if (matchingConditions.contains("dietManagement") && account.getDietManagement() != null) {
          promptBuilder.append("  - Diet Management: User prefers '").append(account.getDietManagement())
              .append("' but this pet needs different diet\n");
        }
        if (matchingConditions.isEmpty()) {
          promptBuilder.append("  (General mismatch with user preferences)\n");
        }
      }

      promptBuilder.append("\nPet Information:\n");
      promptBuilder.append("  - Name: ").append(product.getName()).append("\n");
      promptBuilder.append("  - Category: ").append(product.getCategoryId()).append("\n");
      if (product.getDescription() != null && !product.getDescription().isEmpty()) {
        // Remove HTML tags from description for cleaner prompt
        String cleanDescription = product.getDescription().replaceAll("<[^>]+>", "").trim();
        if (!cleanDescription.isEmpty()) {
          promptBuilder.append("  - Description: ").append(cleanDescription).append("\n");
        }
      }

      if (isRecommended) {
        promptBuilder.append("\nIMPORTANT: Only mention the matching preferences listed above. ");
        promptBuilder
            .append("Do NOT mention any preferences that are not in the 'Matching User Preferences' list above.\n");
      } else {
        promptBuilder.append("\nIMPORTANT: Explain why this pet is not recommended based on the mismatching ");
        promptBuilder.append("preferences listed above. Be specific about what doesn't match.\n");
      }

      promptBuilder.append("\nGenerate a concise, friendly message (maximum 150 characters) in English that explains ");
      if (isRecommended) {
        promptBuilder.append("why this pet is a good match. ONLY mention the matching preferences listed above.\n");
      } else {
        promptBuilder.append("why this pet may not be ideal. Include specific reasons from the mismatching ");
        promptBuilder.append("preferences above. Be polite and constructive.\n");
      }
      promptBuilder.append("Do not include any prefix or explanation, just the message itself.");

      String fullPrompt = promptBuilder.toString();
      ChatMessage userMessage = new ChatMessage("user", fullPrompt);

      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model(OPENAI_MODEL)
          .messages(Collections.singletonList(userMessage)).maxTokens(200) // Increased for not-recommended messages
          .temperature(0.7) // Creativity level
          .build();

      logger.debug("Generating recommendation message for product: {}", product.getProductId());
      String response = openAiService.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage()
          .getContent().trim();

      logger.debug("Generated recommendation message: {}", response);
      return response;

    } catch (Exception e) {
      logger.error("Error generating recommendation message: {}", e.getMessage(), e);
      // Fallback to simple message if LLM fails
      if (isRecommended) {
        return "We recommend this pet based on your preferences.";
      } else {
        return "This pet may not be the best match for your preferences.";
      }
    }
  }

  public List<String> listAvailableModels() {
    // This library does not directly expose a simple list of available chat models.
    // Hardcoding common ones for demonstration.
    return List.of("gpt-3.5-turbo", "gpt-4", "gpt-4o");
  }
}
