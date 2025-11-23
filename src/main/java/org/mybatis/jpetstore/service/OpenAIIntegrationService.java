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
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;

import java.io.InputStream;

import org.springframework.stereotype.Service;

@Service
public class OpenAIIntegrationService {

  private String apiKey;

  public OpenAIIntegrationService() {
    try {

      // Classpath 기반 credentials 폴더에서 JSON 읽기
      InputStream is = getClass().getClassLoader().getResourceAsStream("credentials/openai_credentials.json");

      if (is == null) {
        System.out.println("❌ openai_credentials.json NOT FOUND");
        this.apiKey = null;
        return;
      }

      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(is);

      // 너의 JSON key = "openai_api_key"
      if (node.has("openai_api_key")) {
        this.apiKey = node.get("openai_api_key").asText();
      } else {
        System.out.println("❌ JSON has no 'openai_api_key'");
        this.apiKey = null;
      }

      System.out.println("🔑 Loaded OPENAI API KEY: " + (apiKey != null));

    } catch (Exception e) {
      e.printStackTrace();
      this.apiKey = null;
    }
  }

  public String generateFeedback(String prompt) {
    try {
      if (apiKey == null || apiKey.isBlank()) {
        return "OpenAI API key is missing.";
      }

      OpenAiService ai = new OpenAiService(apiKey);

      CompletionRequest req = CompletionRequest.builder().model("gpt-3.5-turbo-instruct").prompt(prompt).maxTokens(200)
          .temperature(0.7).build();

      return ai.createCompletion(req).getChoices().get(0).getText().trim();

    } catch (Exception e) {
      e.printStackTrace();
      return "AI could not generate a recommendation.";
    }
  }
}
