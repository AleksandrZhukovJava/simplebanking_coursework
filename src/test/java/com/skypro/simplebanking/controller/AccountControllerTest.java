package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static com.skypro.simplebanking.TestData.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void correctUser_CorrectAuthenticationData_ThenReturnCorrectAccounts() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (long id : user.getAccounts().stream().map(Account::getId).toList()) {
            mockMvc.perform(get("/account/{id}", id)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(id));
        }
    }

    @Test
    void shouldGetUserAccount_IncorrectUser_ThenReturn404Status() throws Exception {
        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (long id : user.getAccounts().stream().map(Account::getId).toList()) {
            mockMvc.perform(get("/account/{id}", id)
                            .with(user(userDetails)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test
    void shouldGetUserAccount_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        User user = getTestUser();
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(get("/account/{id}", user.getId())
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());

    }

    @Test
    void shouldDepositToAccount_CorrectUser_CorrectAuthenticationData_CorrectAmount_ThenReturnCorrectAccountDTO() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(getTestRandomLong());
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(account.getId()))
                    .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().name()))
                    .andExpect(jsonPath("$.amount").value(user.getAccounts()
                            .stream()
                            .filter(x -> x.getId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getAmount()));
        }
    }
    @Test
    void shouldDepositToAccount_IncorrectAmount_ThenReturn400Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(INVALID_AMOUNT);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void shouldDepositToAccount_IncorrectUser_ThenReturn400Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(getTestRandomLong());
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void shouldDepositToAccount_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(getTestRandomLong());
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isForbidden());
        }
    }
    @Test
    void shouldWithdrawFromAccount_CorrectUser_CorrectAuthenticationData_CorrectAmount_ThenReturnCorrectAccountDTO() throws Exception {
        User user = userRepository.save(getTestUser());
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(1);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(account.getId()))
                    .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().name()))
                    .andExpect(jsonPath("$.amount").value(user.getAccounts()
                            .stream()
                            .filter(x -> x.getId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getAmount()));
        }
    }
    @Test
    void shouldWithdrawFromAccount_IncorrectAmount_ThenReturn400Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(-1L);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void shouldWithdrawFromAccount_IncorrectUser_ThenReturn400Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(-1L);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void shouldWithdrawFromAccount_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(-1L);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isForbidden());
        }
    }
    @Test
    void shouldWithdrawFromAccount_AmountToWithdrawBiggerThenAmountOnAccount_ThenReturn400Status_ThenReturnCorrectExceptionMessage() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
            balanceChangeRequest.setAmount(account.getAmount() + 1);
            String content = objectMapper.writeValueAsString(balanceChangeRequest);
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string(containsString(String.format("Cannot withdraw %d %s",balanceChangeRequest.getAmount(),account.getAccountCurrency().name()))));
        }
    }
}