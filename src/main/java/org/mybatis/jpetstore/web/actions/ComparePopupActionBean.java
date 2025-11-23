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

/**
 * Compare two items in a popup and show survey-based summary + final feedback.
 */
@UrlBinding("/comparePopup.action")
public class ComparePopupActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 1L;

  private static final String VIEW = "/WEB-INF/jsp/catalog/ComparePopup.jsp";

  @SpringBean
  private transient CatalogService catalogService;

  // --- Request params ---
  private String id1;
  private String id2;

  // --- Model for JSP ---
  private Item item1;
  private Item item2;

  // 설문/프로필 기반 요약 값
  private String residenceEnv;
  private String petSizePref;
  private String activityTime;

  // 최종 피드백 문장
  private String finalFeedback;

  // --- getters / setters ---

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

    // 1) 로그인 세션에서 account 가져오기
    HttpSession session = context.getRequest().getSession(false);
    AccountActionBean accountBean = session == null ? null : (AccountActionBean) session.getAttribute("accountBean");

    if (accountBean == null || !accountBean.isAuthenticated()) {
      setMessage("Please sign in before comparing products.");
      return new RedirectResolution(AccountActionBean.class);
    }

    Account account = accountBean.getAccount();

    // 2) 선택된 두 개의 Item 조회
    if (id1 != null && !id1.isBlank()) {
      item1 = catalogService.getItem(id1);
    }
    if (id2 != null && !id2.isBlank()) {
      item2 = catalogService.getItem(id2);
    }

    // null 방어
    if (item1 == null || item2 == null) {
      setMessage("Could not load selected items. Please try again.");
      return new ForwardResolution(VIEW);
    }

    // 3) 설문/프로필 데이터에서 필요한 값 꺼내기
    residenceEnv = account.getResidenceEnv();
    petSizePref = account.getPetSizePref();
    activityTime = account.getActivityTime();

    // 4) 간단한 rule 기반 최종 피드백 생성
    StringBuilder sb = new StringBuilder();

    sb.append("Based on your living environment");
    if (residenceEnv != null && !residenceEnv.isBlank()) {
      sb.append(" (").append(residenceEnv).append(")");
    }
    sb.append(", preferred pet size");
    if (petSizePref != null && !petSizePref.isBlank()) {
      sb.append(" (").append(petSizePref).append(")");
    }
    sb.append(", and activity time");
    if (activityTime != null && !activityTime.isBlank()) {
      sb.append(" (").append(activityTime).append(")");
    }
    sb.append(", we compared the two items you selected. ");

    // 가격 비교
    if (item1.getListPrice() != null && item2.getListPrice() != null) {
      int comp = item1.getListPrice().compareTo(item2.getListPrice());
      if (comp < 0) {
        sb.append("Item ").append(item1.getItemId()).append(" is more budget-friendly, ");
        sb.append("while item ").append(item2.getItemId()).append(" is relatively more premium. ");
      } else if (comp > 0) {
        sb.append("Item ").append(item2.getItemId()).append(" is more budget-friendly, ");
        sb.append("while item ").append(item1.getItemId()).append(" is relatively more premium. ");
      } else {
        sb.append("Both items are in a similar price range. ");
      }
    }

    // 활동 시간에 따른 추천 방향
    if (activityTime != null) {
      String lower = activityTime.toLowerCase();
      if (lower.contains("high") || lower.contains("long") || lower.contains("많")) {
        sb.append(
            "Since your pet tends to be active, products with better durability and strong support are recommended. ");
      } else if (lower.contains("low") || lower.contains("short") || lower.contains("적")) {
        sb.append(
            "Since your pet is relatively less active, comfort and stability may be more important than durability. ");
      }
    }

    // 마지막 한 줄 결론 느낌
    sb.append("Overall, please choose the item that best matches your budget and daily lifestyle with your pet.");

    finalFeedback = sb.toString();

    return new ForwardResolution(VIEW);
  }
}
