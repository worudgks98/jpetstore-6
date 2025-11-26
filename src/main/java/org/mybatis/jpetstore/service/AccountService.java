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

import java.util.Optional;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The Class AccountService.
 *
 * @author Eduardo Macarron
 */
@Service
public class AccountService {

  private final AccountMapper accountMapper;
  private final RecommendationMessageService recommendationMessageService;

  public AccountService(AccountMapper accountMapper, RecommendationMessageService recommendationMessageService) {
    this.accountMapper = accountMapper;
    this.recommendationMessageService = recommendationMessageService;
  }

  public Account getAccount(String username) {
    return accountMapper.getAccountByUsername(username);
  }

  public Account getAccount(String username, String password) {
    return accountMapper.getAccountByUsernameAndPassword(username, password);
  }

  /**
   * Insert account.
   *
   * @param account
   *          the account
   */
  @Transactional
  public void insertAccount(Account account) {
    accountMapper.insertAccount(account);
    accountMapper.insertProfile(account);
    accountMapper.insertSignon(account);
    triggerRecommendationRefresh(account);
  }

  /**
   * Update account.
   *
   * @param account
   *          the account
   */
  @Transactional
  public void updateAccount(Account account) {
    accountMapper.updateAccount(account);
    accountMapper.updateProfile(account);

    Optional.ofNullable(account.getPassword()).filter(password -> password.length() > 0)
        .ifPresent(password -> accountMapper.updateSignon(account));

    // Note: Recommendation refresh is handled in AccountActionBean.editAccount()
    // after the transaction commits to ensure we have the latest data
  }

  /**
   * Refresh recommendation messages for an account. This should be called after updateAccount() to ensure
   * recommendations are regenerated.
   *
   * @param username
   *          the username to refresh recommendations for
   */
  public void refreshRecommendationsForUser(String username) {
    if (username == null) {
      return;
    }

    // Fetch the latest account data from DB
    Account latestAccount = accountMapper.getAccountByUsername(username);
    if (latestAccount != null) {
      recommendationMessageService.refreshRecommendations(latestAccount);
    }
  }

  private void triggerRecommendationRefresh(Account account) {
    if (account == null || account.getUsername() == null) {
      return;
    }

    // Create a copy of the account to avoid issues with transaction state
    Account accountSnapshot = new Account();
    accountSnapshot.setUsername(account.getUsername());
    accountSnapshot.setResidenceEnv(account.getResidenceEnv());
    accountSnapshot.setCarePeriod(account.getCarePeriod());
    accountSnapshot.setPetColorPref(account.getPetColorPref());
    accountSnapshot.setPetSizePref(account.getPetSizePref());
    accountSnapshot.setActivityTime(account.getActivityTime());
    accountSnapshot.setDietManagement(account.getDietManagement());

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          // After commit, fetch the latest account data from DB to ensure we have the most up-to-date information
          Account latestAccount = accountMapper.getAccountByUsername(accountSnapshot.getUsername());
          if (latestAccount != null) {
            recommendationMessageService.refreshRecommendations(latestAccount);
          } else {
            // Fallback to snapshot if DB fetch fails
            recommendationMessageService.refreshRecommendations(accountSnapshot);
          }
        }
      });
    } else {
      // If no transaction, use the account directly
      recommendationMessageService.refreshRecommendations(accountSnapshot);
    }
  }

}
