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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.integration.spring.SpringBean;
import net.sourceforge.stripes.validation.Validate;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.service.AccountService;
import org.mybatis.jpetstore.service.CatalogService;
import org.springframework.dao.DuplicateKeyException;

/**
 * The Class AccountActionBean.
 *
 * @author Eduardo Macarron
 */
@SessionScope
public class AccountActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 5499663666155758178L;

  private static final String NEW_ACCOUNT = "/WEB-INF/jsp/account/NewAccountForm.jsp";
  private static final String EDIT_ACCOUNT = "/WEB-INF/jsp/account/EditAccountForm.jsp";
  private static final String SIGNON = "/WEB-INF/jsp/account/SignonForm.jsp";

  private static final List<String> LANGUAGE_LIST;
  private static final List<String> CATEGORY_LIST;

  @SpringBean
  private transient AccountService accountService;
  @SpringBean
  private transient CatalogService catalogService;

  private Account account = new Account();
  private List<Product> myList;
  private boolean authenticated;
  private String repeatedPassword;

  static {
    LANGUAGE_LIST = Collections.unmodifiableList(Arrays.asList("english", "japanese"));
    CATEGORY_LIST = Collections.unmodifiableList(Arrays.asList("FISH", "DOGS", "REPTILES", "CATS", "BIRDS"));
  }

  public Account getAccount() {
    return this.account;
  }

  public String getUsername() {
    return account.getUsername();
  }

  @Validate(required = true, on = { "signon", "newAccount" })
  public void setUsername(String username) {
    account.setUsername(username);
  }

  public String getPassword() {
    return account.getPassword();
  }

  @Validate(required = true, on = { "signon", "newAccount" })
  public void setPassword(String password) {
    account.setPassword(password);
  }

  public List<Product> getMyList() {
    return myList;
  }

  public void setMyList(List<Product> myList) {
    this.myList = myList;
  }

  public List<String> getLanguages() {
    return LANGUAGE_LIST;
  }

  public List<String> getCategories() {
    return CATEGORY_LIST;
  }

  public Resolution newAccountForm() {
    return new ForwardResolution(NEW_ACCOUNT);
  }

  /**
   * New account.
   *
   * @return the resolution
   */
  public Resolution newAccount() {
    if (!passwordsMatch()) {
      setMessage("Password and repeated password must match.");
      return new ForwardResolution(NEW_ACCOUNT);
    }
    try {
      System.out.println("=== newAccount() called ===");
      System.out.println("Account before insert - residenceEnv: [" + account.getResidenceEnv() + "]");
      System.out.println("Account before insert - carePeriod: [" + account.getCarePeriod() + "]");
      System.out.println("Account before insert - petColorPref: [" + account.getPetColorPref() + "]");
      System.out.println("Account before insert - petSizePref: [" + account.getPetSizePref() + "]");
      System.out.println("Account before insert - activityTime: [" + account.getActivityTime() + "]");
      System.out.println("Account before insert - dietManagement: [" + account.getDietManagement() + "]");

      accountService.insertAccount(account);
      System.out.println("=== Account inserted into DB ===");

      // Get the latest account data from DB after insert
      account = accountService.getAccount(account.getUsername());
      System.out.println("=== Account reloaded from DB ===");
      System.out.println("Account after reload - residenceEnv: [" + account.getResidenceEnv() + "]");
      System.out.println("Account after reload - carePeriod: [" + account.getCarePeriod() + "]");
      System.out.println("Account after reload - petColorPref: [" + account.getPetColorPref() + "]");
      System.out.println("Account after reload - petSizePref: [" + account.getPetSizePref() + "]");
      System.out.println("Account after reload - activityTime: [" + account.getActivityTime() + "]");
      System.out.println("Account after reload - dietManagement: [" + account.getDietManagement() + "]");

      // Check if survey is completed
      boolean surveyCompleted = catalogService.hasCompletedSurvey(account);
      System.out.println("=== Survey completed: " + surveyCompleted + " ===");

      // Refresh recommendation messages with the new account data (synchronously)
      // This ensures recommendations are ready immediately after signup
      System.out.println("=== Calling refreshRecommendationsForUser() ===");
      accountService.refreshRecommendationsForUser(account.getUsername());
      System.out.println("=== refreshRecommendationsForUser() completed ===");

      myList = catalogService.getProductListByCategory(account.getFavouriteCategoryId());
      authenticated = true;
      // Update session with the new accountBean
      HttpSession session = context.getRequest().getSession();
      session.setAttribute("accountBean", this);
      return new RedirectResolution(CatalogActionBean.class);
    } catch (DuplicateKeyException e) {
      setMessage("This username already exists. Please choose a different username.");
      return new ForwardResolution(NEW_ACCOUNT);
    }
  }

  /**
   * Edits the account form.
   *
   * @return the resolution
   */
  public Resolution editAccountForm() {
    // Get username from session accountBean if current account doesn't have it
    String username = account != null ? account.getUsername() : null;
    if (username == null) {
      HttpSession session = context.getRequest().getSession(false);
      if (session != null) {
        AccountActionBean sessionBean = (AccountActionBean) session.getAttribute("accountBean");
        if (sessionBean != null && sessionBean.getAccount() != null) {
          username = sessionBean.getAccount().getUsername();
        }
      }
    }

    // Load current account information from DB to ensure we have the latest data
    if (username != null) {
      Account currentAccount = accountService.getAccount(username);
      if (currentAccount != null) {
        account = currentAccount;
        // Clear password fields for security
        account.setPassword(null);
        repeatedPassword = null;
      }
    }
    return new ForwardResolution(EDIT_ACCOUNT);
  }

  /**
   * Edits the account.
   *
   * @return the resolution
   */
  public Resolution editAccount() {
    // Force log output immediately - this MUST appear if method is called
    System.out.println("=== editAccount() METHOD CALLED - START ===");
    System.out.flush();

    try {
      // Get username from session (form doesn't submit username as it's read-only)
      HttpSession session = context.getRequest().getSession(false);
      String username = null;
      if (session != null) {
        AccountActionBean sessionBean = (AccountActionBean) session.getAttribute("accountBean");
        if (sessionBean != null && sessionBean.getAccount() != null) {
          username = sessionBean.getAccount().getUsername();
          System.out.println("=== Got username from session: " + username + " ===");
        }
      }

      if (username == null) {
        System.out.println("=== ERROR: Username is null - user not signed in ===");
        setMessage("You must be signed in to edit your account.");
        return new RedirectResolution(AccountActionBean.class, "signonForm");
      }

      // IMPORTANT: Ensure account object exists and get form data directly from request
      // This ensures we capture all form data including survey answers
      if (account == null) {
        System.out.println("=== WARNING: account is null, creating new Account ===");
        account = new Account();
      }

      // Set username from session (form doesn't submit it)
      account.setUsername(username);

      // Get form data directly from request parameters to ensure we have the latest values
      // This is a workaround for potential Stripes binding issues with @SessionScope
      HttpServletRequest request = context.getRequest();
      System.out.println("=== Reading request parameters ===");
      String residenceEnv = request.getParameter("account.residenceEnv");
      String carePeriod = request.getParameter("account.carePeriod");
      String petColorPref = request.getParameter("account.petColorPref");
      String petSizePref = request.getParameter("account.petSizePref");
      String activityTime = request.getParameter("account.activityTime");
      String dietManagement = request.getParameter("account.dietManagement");

      System.out.println("=== Request parameters read ===");
      System.out.println("  residenceEnv: [" + residenceEnv + "]");
      System.out.println("  carePeriod: [" + carePeriod + "]");
      System.out.println("  petColorPref: [" + petColorPref + "]");
      System.out.println("  petSizePref: [" + petSizePref + "]");
      System.out.println("  activityTime: [" + activityTime + "]");
      System.out.println("  dietManagement: [" + dietManagement + "]");

      // Update account object with form data if provided
      if (residenceEnv != null) {
        account.setResidenceEnv(residenceEnv);
      }
      if (carePeriod != null) {
        account.setCarePeriod(carePeriod);
      }
      if (petColorPref != null) {
        account.setPetColorPref(petColorPref);
      }
      if (petSizePref != null) {
        account.setPetSizePref(petSizePref);
      }
      if (activityTime != null) {
        account.setActivityTime(activityTime);
      }
      if (dietManagement != null) {
        account.setDietManagement(dietManagement);
      }

      System.out.println("=== Using account object with form data (including survey answers) ===");
      System.out.println("Account from form - residenceEnv: [" + account.getResidenceEnv() + "]");
      System.out.println("Account from form - carePeriod: [" + account.getCarePeriod() + "]");
      System.out.println("Account from form - petColorPref: [" + account.getPetColorPref() + "]");
      System.out.println("Account from form - petSizePref: [" + account.getPetSizePref() + "]");
      System.out.println("Account from form - activityTime: [" + account.getActivityTime() + "]");
      System.out.println("Account from form - dietManagement: [" + account.getDietManagement() + "]");

      // Only validate password if it's being changed (not empty)
      String password = account.getPassword();
      if (password != null && !password.isEmpty()) {
        if (!passwordsMatch()) {
          System.out.println("=== Password mismatch ===");
          setMessage("Password and repeated password must match.");
          return new ForwardResolution(EDIT_ACCOUNT);
        }
      } else {
        // If password is empty, set it to null so it won't be updated
        account.setPassword(null);
      }

      System.out.println("=== Calling updateAccount() ===");
      accountService.updateAccount(account);
      System.out.println("=== Account updated in DB ===");

      // Get the latest account data from DB after update
      account = accountService.getAccount(username);
      System.out.println("=== Account reloaded from DB ===");
      System.out.println("Account after reload - residenceEnv: [" + account.getResidenceEnv() + "]");
      System.out.println("Account after reload - carePeriod: [" + account.getCarePeriod() + "]");
      System.out.println("Account after reload - petColorPref: [" + account.getPetColorPref() + "]");
      System.out.println("Account after reload - petSizePref: [" + account.getPetSizePref() + "]");
      System.out.println("Account after reload - activityTime: [" + account.getActivityTime() + "]");
      System.out.println("Account after reload - dietManagement: [" + account.getDietManagement() + "]");

      // Check if survey is completed
      boolean surveyCompleted = catalogService.hasCompletedSurvey(account);
      System.out.println("=== Survey completed: " + surveyCompleted + " ===");

      // Clear password fields for security
      account.setPassword(null);
      repeatedPassword = null;

      // Refresh recommendation messages with the updated account data
      // This will generate AI messages for all products if survey is completed
      System.out.println("=== Calling refreshRecommendationsForUser() ===");
      accountService.refreshRecommendationsForUser(username);
      System.out.println("=== refreshRecommendationsForUser() completed ===");

      myList = catalogService.getProductListByCategory(account.getFavouriteCategoryId());
      // Update session with the updated accountBean
      HttpSession updatedSession = context.getRequest().getSession();
      updatedSession.setAttribute("accountBean", this);
      System.out.println("=== Session accountBean updated with new account data ===");
      System.out.println("=== editAccount() METHOD COMPLETED SUCCESSFULLY ===");
      return new RedirectResolution(CatalogActionBean.class);
    } catch (Exception e) {
      System.err.println("=== ERROR in editAccount(): " + e.getMessage() + " ===");
      System.err.println("=== Exception class: " + e.getClass().getName() + " ===");
      e.printStackTrace();
      setMessage("An error occurred while updating your account: " + e.getMessage());
      return new ForwardResolution(EDIT_ACCOUNT);
    }
  }

  /**
   * Signon form.
   *
   * @return the resolution
   */
  @DefaultHandler
  public Resolution signonForm() {
    return new ForwardResolution(SIGNON);
  }

  /**
   * Signon.
   *
   * @return the resolution
   */
  public Resolution signon() {

    account = accountService.getAccount(getUsername(), getPassword());

    if (account == null) {
      String value = "Invalid username or password.  Signon failed.";
      setMessage(value);
      clear();
      return new ForwardResolution(SIGNON);
    } else {
      account.setPassword(null);
      myList = catalogService.getProductListByCategory(account.getFavouriteCategoryId());
      authenticated = true;
      HttpSession s = context.getRequest().getSession();
      // this bean is already registered as /actions/Account.action
      s.setAttribute("accountBean", this);
      return new RedirectResolution(CatalogActionBean.class);
    }
  }

  /**
   * Signoff.
   *
   * @return the resolution
   */
  public Resolution signoff() {
    context.getRequest().getSession().invalidate();
    clear();
    return new RedirectResolution(CatalogActionBean.class);
  }

  /**
   * Checks if is authenticated.
   *
   * @return true, if is authenticated
   */
  public boolean isAuthenticated() {
    return authenticated && account != null && account.getUsername() != null;
  }

  /**
   * Clear.
   */
  public void clear() {
    account = new Account();
    myList = null;
    authenticated = false;
    repeatedPassword = null;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  private boolean passwordsMatch() {
    String password = account.getPassword();
    if (password == null || password.isEmpty()) {
      return true;
    }
    return password.equals(repeatedPassword == null ? "" : repeatedPassword);
  }

}
