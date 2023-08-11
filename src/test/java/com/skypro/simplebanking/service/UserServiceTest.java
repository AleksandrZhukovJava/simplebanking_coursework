package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.ListUserDTO;
import com.skypro.simplebanking.dto.UserDTO;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.exception.UserAlreadyExistsException;
import com.skypro.simplebanking.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.skypro.simplebanking.TestData.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService out;

    @Test
    public void uncorrectedUsername_ThenThrowUsernameNotFoundException_ThenReturnCorrectMassage() {
        String massage = "User not found";
        when(userRepository.findByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException(massage));

        assertThrows(UsernameNotFoundException.class,
                () -> out.loadUserByUsername(getTestUserName()));

        Throwable exception = assertThrows(UsernameNotFoundException.class, () -> {
            out.loadUserByUsername(anyString());
        });
        assertThat(massage).isEqualTo(exception.getMessage());

    }

    @Test
    public void correctUsername_ThenReturnCorrectBankingUserDetails_CallRepositoryOnce() {
        User testUser = getTestUser();
        BankingUserDetails expected = BankingUserDetails.from(testUser);
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(testUser));

        UserDetails actual = out.loadUserByUsername(testUser.getUsername());

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        verify(userRepository, times(1)).findByUsername(anyString());
    }
    @Test
    public void userThatAlreadyExist_ThenThrowUserAlreadyExistsException_CallRepositoryOnce() {
        User expected = getTestUser();
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(expected));

        assertThrows(UserAlreadyExistsException.class,
                () -> out.createUser(expected.getUsername(),expected.getPassword()));

        verify(userRepository, times(0)).save(expected);
    }
    @Test
    @Disabled
    public void correctUserNameAndPassword_ReturnCorrectUserDTO_CallRepositoryTwice() {
        String username = getTestUserName();
        String password = getTestPassword();
        User testUser = getTestUser();
        testUser.setId(getTestRandomLong());
        testUser.setUsername(username);
        testUser.setPassword(password);

        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.ofNullable(null));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        UserDTO expected = UserDTO.from(testUser);
        // я не могу установить ему id внутри сервиса, а без него не могу создать объект
        // mock метода save репозитория не меняет объект внутри сервиса (логично)
        // нет идей как это тестировать без реальной базы данных
        UserDTO actual = out.createUser(username, password);


        Assertions.assertThat(actual)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("id")
                        .build())
                .isEqualTo(expected);
        verify(userRepository,times(1)).findByUsername(anyString());
        verify(userRepository,times(1)).save(any(User.class));
    }
    @Test
    public void correctId_ThenReturnCorrectUserDTO_CallRepositoryOnce() {
        User testUser = getTestUser();

        UserDTO expected = UserDTO.from(testUser);

        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(testUser));

        UserDTO actual = out.getUser(getTestRandomLong());

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        verify(userRepository, times(1)).findById(anyLong());

    }
    @Test
    public void uncorrectedId_ThenThrowRuntimeException() {
        when(userRepository.findById(anyLong()))
                .thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> out.getUser(getTestRandomLong()));
    }
    @Test
    public void returnCorrectListOfUsersDTO_CallRepositoryOnce() {
        User testUserOne = getTestUser();
        User testUserTwo = getTestUser();

        when(userRepository.findAll())
                .thenReturn(List.of(testUserOne,testUserTwo));
        List<ListUserDTO> expected = new ArrayList<>() {{
                add(ListUserDTO.from(testUserOne));
                add(ListUserDTO.from(testUserTwo));
            }};
        List<ListUserDTO> actual = out.listUsers();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        verify(userRepository, times(1)).findAll();
    }
}