package com.skypro.simplebanking;

import com.github.javafaker.Faker;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.CreateUserRequest;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;

import java.util.ArrayList;
import java.util.Random;


public class TestData {
    public static final Long INVALID_AMOUNT = -1L;
    public static final Faker faker = new Faker();
    public static final TransferRequest TEST_EMPTY_TRANSFER_REQUEST = new TransferRequest();
    public static Long getTestRandomLong() {
        return new Random().nextLong(1000);
    }
    public static String getTestPassword() {
        return faker.number().digits(10);
    }
    public static String getTestUserName() {
        return faker.name().name();
    }
    public static User getTestUser() {
        User user = new User();
        user.setAccounts(new ArrayList<>());
        for (AccountCurrency currency : AccountCurrency.values()) {
            Account account = new Account();
            account.setId(getTestRandomLong());
            account.setUser(user);
            account.setAccountCurrency(currency);
            account.setAmount(getTestRandomLong());
            user.getAccounts().add(account);
        }
        user.setId(getTestRandomLong());
        user.setUsername(faker.name().username());
        user.setPassword(getTestPassword());
        return user;
    }
    public static BankingUserDetails getAdminBankingUserDetails(User user) {
        return new BankingUserDetails(getTestRandomLong(), user.getUsername(), user.getPassword(), true);
    }
    public static Account getTestAccount(User user, AccountCurrency currency) {
        Account account = new Account();
        account.setId(getTestRandomLong());
        account.setUser(user);
        account.setAmount(getTestRandomLong());
        account.setAccountCurrency(currency);
        return account;
    }
    public static TransferRequest getTestTransferRequest(User fromUser, AccountCurrency fromAccountCurrency, User toUser, AccountCurrency toAccountCurrency, long amount) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(fromAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setToUserId(toUser.getId());
        transferRequest.setToAccountId(toUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(toAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setAmount(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(AccountCurrency.RUB))
                .findFirst()
                .map(x -> x.getAmount() - 1)
                .orElseThrow());
        transferRequest.setAmount(amount);
        return transferRequest;
    }
    public static long getUserAmount(User user, AccountCurrency accountCurrency) {
        return user.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(accountCurrency))
                .mapToLong(Account::getAmount)
                .findAny()
                .orElseThrow();
    }

    public static CreateUserRequest getUserRequest(User user) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(user.getUsername());
        createUserRequest.setPassword(user.getPassword());
        return createUserRequest;
    }
}
