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

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.integration.spring.SpringBean;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.service.CatalogService;
import org.mybatis.jpetstore.service.OpenAIIntegrationService;

/**
 * Compare two items in a popup and show survey-based summary + final feedback.
 */
@UrlBinding("/comparePopup.action")
public class ComparePopupActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 1L;
  private static final String VIEW = "/WEB-INF/jsp/catalog/ComparePopup.jsp";

  @SpringBean
  private transient CatalogService catalogService;

  @SpringBean
  private transient OpenAIIntegrationService aiService; // 🔵 추가됨

  private String id1;
  private String id2;

  private Item item1;
  private Item item2;

  private String residenceEnv;
  private String petSizePref;
  private String activityTime;

  private String finalFeedback;

  // ---- getters/setters ----
  public String getId1() {
    return id1;
  }

  public void setId1(String id1) {
    this.id1 = id1;
  }

  public String getId2() {
    return id2;
  }

  public void setId2(String id2) {
    this.id2 = id2;
  }

  public Item getItem1() {
    return item1;
  }

  public Item getItem2() {
    return item2;
  }

  public String getResidenceEnv() {
    return residenceEnv;
  }

  public String getPetSizePref() {
    return petSizePref;
  }

  public String getActivityTime() {
    return activityTime;
  }

  public String getFinalFeedback() {
    return finalFeedback;
  }

  @DefaultHandler
  public Resolution showPopup() {

    HttpSession session = context.getRequest().getSession(false);
    AccountActionBean accountBean = session == null ? null : (AccountActionBean) session.getAttribute("accountBean");

    if (accountBean == null || !accountBean.isAuthenticated()) {
      setMessage("Please sign in before comparing products.");
      return new RedirectResolution(AccountActionBean.class);
    }

    Account account = accountBean.getAccount();

    if (id1 != null && !id1.isBlank()) {
      item1 = catalogService.getItem(id1);
    }
    if (id2 != null && !id2.isBlank()) {
      item2 = catalogService.getItem(id2);
    }

    if (item1 == null || item2 == null) {
      setMessage("Could not load selected items. Please try again.");
      return new ForwardResolution(VIEW);
    }

    residenceEnv = account.getResidenceEnv();
    petSizePref = account.getPetSizePref();
    activityTime = account.getActivityTime();

    // --------- LLM 사용해서 최종 피드백 생성 ---------
    String prompt = "You are an expert pet-care and pet-product advisor. "
        + "Compare the two pet items and give a personalized recommendation.\n\n" + "User Profile:\n"
        + "- Residence Environment: " + residenceEnv + "\n" + "- Preferred Pet Size: " + petSizePref + "\n"
        + "- Activity Time: " + activityTime + "\n\n" + "Item 1:\n" + "- ID: " + item1.getItemId() + "\n" + "- Name: "
        + item1.getProduct().getName() + "\n" + "- Price: " + item1.getListPrice() + "\n\n" + "Item 2:\n" + "- ID: "
        + item2.getItemId() + "\n" + "- Name: " + item2.getProduct().getName() + "\n" + "- Price: "
        + item2.getListPrice() + "\n\n" + "Provide a friendly, clear 4–5 sentence recommendation explaining "
        + "which product suits the user's environment and lifestyle better.";

    finalFeedback = aiService.generateFeedback(prompt);

    return new ForwardResolution(VIEW);
  }
}
