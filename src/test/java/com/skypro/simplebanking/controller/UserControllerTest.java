package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.CreateUserRequest;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static com.skypro.simplebanking.TestData.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUser_AdminAuthenticationData_ThenAddUserToDataBase_ThenReturnCorrectDTO() throws Exception {
        User user = getTestUser();
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(post("/user/")
                .with(user(bankingUserDetails)).
                contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }
    @Test
    void shouldCreateUser_UserAuthenticationData_ThenReturn403Status() throws Exception {
        User user = getTestUser();
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(post("/user/")
                        .with(user(bankingUserDetails)).
                        contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldCreateUser_UserAlreadyExistsInDatabase_ThenReturn400Status() throws Exception { //поменять название
        User user = userRepository.save(getTestUser());
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(post("/user")
                        .with(user(bankingUserDetails)).
                        contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().is4xxClientError());
    }
    @Test
    void shouldCreateUser_AdminAuthenticationData_ThenReturnCorrectDTO() throws Exception {
        User user = getTestUser();
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(post("/user")
                        .with(user(bankingUserDetails)).
                        contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk());
    }
    @Test
    void shouldGetAllUsers_ThenReturnCorrectly_DeleteAllEmployeesFromDataBase_ThenReturnEmptyList() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(get("/user/list")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value(user.getUsername()))
                .andExpect(jsonPath("$[0].id").value(user.getId()))
                .andExpect(jsonPath("$[0].accounts").isArray())
                .andExpect(jsonPath("$[0].accounts").isNotEmpty());

        userRepository.deleteAll();

        mockMvc.perform(get("/user/list")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

    }
    @Test
    void shouldGetAllUsers_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(get("/user/list")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isForbidden());

    }
    @Test
    void shouldReturnUserDTO_SaveToDataBase_ThenReturnCorrectUserDTO() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(get("/user/me")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }
    @Test
    void shouldReturnUserDTO_IncorrectAuthenticationData_ThenReturn403Status() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(get("/user/me")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isForbidden());
    }
}