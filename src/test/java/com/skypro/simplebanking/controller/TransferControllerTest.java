package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.AccountCurrency;
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

import static org.assertj.core.api.Assertions.assertThat;
import static com.skypro.simplebanking.TestData.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldTransferAmount_CorrectUsers_CorrectAuthenticationData_CorrectCurrencies_CorrectAmount_ThenTransferSuccessfully() throws Exception {
        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        long receiverAmount = getUserAmount(toUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long senderActualAmount = getUserAmount(userRepository.findById(fromUser.getId()).orElseThrow(), AccountCurrency.RUB);
        long receiverActualAmount = getUserAmount(userRepository.findById(toUser.getId()).orElseThrow(), AccountCurrency.RUB);

        assertThat(senderActualAmount).isEqualTo(0);
        assertThat(receiverActualAmount).isEqualTo(receiverAmount + senderAmount);


    }

    @Test
    void shouldTransferAmount_NonExistedSender_ThenReturn404Status() throws Exception {
        User fromUser = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldTransferAmount_NonExistedReceiver_ThenReturn404Status() throws Exception {
        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = getTestUser();
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldTransferAmount_IncorrectAmount_ThenReturn404Status_AndThenReturnCorrectExceptionMessage() throws Exception {
        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long amount = -1;
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, amount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("Amount should be more than 0")));


    }

    @Test
    void shouldTransferAmount_AmountToTransferBiggerThenAmountOnAccount_ThenReturn404Status_AndThenReturnCorrectExceptionMessage() throws Exception {
        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long amount = getUserAmount(fromUser, AccountCurrency.RUB) + 1;
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, amount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString(String.format("Cannot withdraw %d %s", amount, AccountCurrency.RUB))));
    }

    @Test
    void shouldTransferAmount_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        User fromUser = getTestUser();
        BankingUserDetails userDetails = getAdminBankingUserDetails(fromUser);
        String content = objectMapper.writeValueAsString(new TransferRequest());

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldTransferAmount_DifferentCurrencies_ThenReturn400Status_AndThenReturnCorrectExceptionMessage() throws Exception {
        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());

        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.USD, getUserAmount(fromUser, AccountCurrency.RUB));
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("Account currencies should be same")));

    }
}

