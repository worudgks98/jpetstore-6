/*
 *    Copyright 2010-2022 the original author or authors.
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
    triggerRecommendationRefresh(account.getUsername());
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
    triggerRecommendationRefresh(account.getUsername());
  }

  private void triggerRecommendationRefresh(String username) {
    Optional<Account> latestAccount = Optional.ofNullable(username).map(accountMapper::getAccountByUsername);
    if (!latestAccount.isPresent()) {
      return;
    }

    Account accountSnapshot = latestAccount.get();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          recommendationMessageService.refreshRecommendationsAsync(accountSnapshot);
        }
      });
    } else {
      recommendationMessageService.refreshRecommendationsAsync(accountSnapshot);
    }
  }

}
