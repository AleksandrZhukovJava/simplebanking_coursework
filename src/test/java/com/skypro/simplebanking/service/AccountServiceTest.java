package com.skypro.simplebanking.service;

import com.skypro.simplebanking.TestData;
import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.exception.AccountNotFoundException;
import com.skypro.simplebanking.exception.InsufficientFundsException;
import com.skypro.simplebanking.exception.InvalidAmountException;
import com.skypro.simplebanking.exception.WrongCurrencyException;
import com.skypro.simplebanking.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.skypro.simplebanking.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private AccountService out;

    @Test
    public void correctUser_ThenAccountsCreated_ThenCallRepositoryThreeTimes() {
        when(accountRepository.save(any(Account.class)))
                .thenReturn(any(Account.class));
        out.createDefaultAccounts(TestData.getTestUser());
        verify(accountRepository, times(AccountCurrency.values().length)).save(any(Account.class));
    }

    @Test
    public void uncorrectedUser_ThenAccountsDoesntCreate_ThenThrowNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> out.createDefaultAccounts(null)
        );
    }

    @Test
    public void correctUserId_CorrectAccountId_ThenAccountDTOReturnsCorrectly_AndCallRepositoryOnce() {
        Account account = getTestAccount(getTestUser(), AccountCurrency.RUB);

        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(account));

        AccountDTO expected = AccountDTO.from(account);

        AccountDTO actual = out.getAccount(1L, 1L);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        verify(accountRepository, times(1)).getAccountByUser_IdAndId(1L, 1L);
    }

    @Test
    public void uncorrectedUserId_UncorrectedAccountId_ThenThrowAccountNotFoundException() {
        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class,
                () -> out.getAccount(anyLong(), anyLong())
        );
    }

    @Test
    public void correctAccounts_CallsRepositoryTwice() {
        when(accountRepository.findById(anyLong()))
                .thenReturn(Optional.of(TestData.getTestAccount(getTestUser(),AccountCurrency.RUB)));

        out.validateCurrency(1L, 2L);

        verify(accountRepository, times(2)).findById(anyLong());
    }

    @Test
    public void unmatchedCurrency_CallsRepositoryTwice_ThenThrowWrongCurrencyException() {
        Optional<Account> testAccount = Optional.of(TestData.getTestAccount(getTestUser(),AccountCurrency.RUB));
        Optional<Account> testAccount2 = Optional.of(TestData.getTestAccount(getTestUser(),AccountCurrency.EUR));

        when(accountRepository.findById(1L))
                .thenReturn(testAccount);
        when(accountRepository.findById(2L))
                .thenReturn(testAccount2);

        assertThrows(WrongCurrencyException.class,
                () -> out.validateCurrency(1L, 2L));

        verify(accountRepository, times(2)).findById(anyLong());
    }

    @Test
    public void uncorrectedIdToDeposit_ThenThrowAccountNotFoundException_CallRepositoryOnlyOnce() {
        when(accountRepository.findById(anyLong()))
                .thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class,
                () -> out.validateCurrency(getTestRandomLong(), getTestRandomLong()));

        verify(accountRepository, times(1)).findById(anyLong());
    }

    @Test
    public void uncorrectedAmountToDeposit_ThenThrowInvalidAmountException_DoesntCallRepository() {
        assertThrows(InvalidAmountException.class,
                () -> out.depositToAccount(getTestRandomLong(), getTestRandomLong(), INVALID_AMOUNT));

        verify(accountRepository, times(0)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }

    @Test
    public void correctDataToDeposit_ReturnCorrectAccountDTO_AmountWasChangedCorrectly_CallRepositoryOnce() {
        Account testAccount = TestData.getTestAccount(getTestUser(),AccountCurrency.RUB);
        Long amount = getTestRandomLong();
        Long expected = testAccount.getAmount() + amount;

        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testAccount));


        AccountDTO actual = out.depositToAccount(getTestRandomLong(), getTestRandomLong(), amount);
        AccountDTO expectedDTO = AccountDTO.from(testAccount);

        assertThat(expected).isEqualTo(actual.getAmount());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedDTO);


        verify(accountRepository, times(1)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }

    @Test
    public void uncorrectedAmountToWithdraw_ThenThrowInvalidAmountException_DoesntCallRepository2() {
        assertThrows(InvalidAmountException.class,
                () -> out.withdrawFromAccount(getTestRandomLong(), getTestRandomLong(), INVALID_AMOUNT));

        verify(accountRepository, times(0)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }

    @Test
    public void uncorrectedIdToWithdraw_ThenThrowAccountNotFoundException_CallRepositoryOnlyOnce() {
        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class,
                () -> out.withdrawFromAccount(getTestRandomLong(), getTestRandomLong(), getTestRandomLong()));

        verify(accountRepository, times(1)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }

    @Test
    public void uncorrectedAmountToWithdraw_ThenThrowInsufficientFundsException_CallRepositoryOnce() {
        Account testAccount = TestData.getTestAccount(getTestUser(),AccountCurrency.RUB);

        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testAccount));

        assertThrows(InsufficientFundsException.class,
                () -> out.withdrawFromAccount(getTestRandomLong(), getTestRandomLong(), testAccount.getAmount() + 1L));

        verify(accountRepository, times(1)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }

    @Test
    public void uncorrectedAmountToWithdraw_ThenMassageIsCorrect() {
        Account testAccount = TestData.getTestAccount(getTestUser(),AccountCurrency.RUB);
        long newAmount = testAccount.getAmount() + 1L;

        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testAccount));
        Throwable exception = assertThrows(InsufficientFundsException.class, () -> {
            out.withdrawFromAccount(getTestRandomLong(), getTestRandomLong(), newAmount);
        });
        assertThat(String.format("Cannot withdraw %d %s",newAmount,testAccount.getAccountCurrency().name()))
                .isEqualTo(exception.getMessage());
    }

    @Test
    public void correctDataToWithdraw_ReturnCorrectAccountDTO_AmountWasChangedCorrectly_CallRepositoryOnce() {
        Account testAccount = TestData.getTestAccount(getTestUser(),AccountCurrency.RUB);
        Long expected = testAccount.getAmount();

        when(accountRepository.getAccountByUser_IdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testAccount));


        AccountDTO actual = out.withdrawFromAccount(getTestRandomLong(), getTestRandomLong(), 0);
        AccountDTO expectedDTO = AccountDTO.from(testAccount);

        assertThat(expected).isEqualTo(actual.getAmount());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedDTO);


        verify(accountRepository, times(1)).getAccountByUser_IdAndId(anyLong(), anyLong());
    }
}
