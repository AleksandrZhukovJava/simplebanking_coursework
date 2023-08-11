package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.exception.InvalidAmountException;
import com.skypro.simplebanking.exception.WrongCurrencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.skypro.simplebanking.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private AccountService accountService;
    @InjectMocks
    private TransferService out;

    @Test
    public void correctData_ThenReturnCorrectDTO_CallRepositoryThreeTimes(){
        doNothing().when(accountService).validateCurrency(anyLong(),anyLong());
        when(accountService.withdrawFromAccount(anyLong(), anyLong(), anyLong()))
                .thenReturn(AccountDTO.from(getTestAccount(getTestUser(), AccountCurrency.RUB)));
        when(accountService.depositToAccount(anyLong(),anyLong(),anyLong()))
                .thenReturn(AccountDTO.from(getTestAccount(getTestUser(),AccountCurrency.RUB)));

        out.transfer(getTestRandomLong(), TEST_EMPTY_TRANSFER_REQUEST);

        verify(accountService,times(1)).validateCurrency(anyLong(),anyLong());
        verify(accountService,times(1)).withdrawFromAccount(anyLong(),anyLong(),anyLong());
        verify(accountService,times(1)).depositToAccount(anyLong(),anyLong(),anyLong());
    }
    @Test
    public void uncorrectedCurrency_ThenThrowWrongCurrencyException_CallRepositoryOnlyOnce(){
        doThrow(WrongCurrencyException.class).when(accountService).validateCurrency(anyLong(),anyLong());

        assertThrows(WrongCurrencyException.class,
                () -> out.transfer(getTestRandomLong(), TEST_EMPTY_TRANSFER_REQUEST)
        );

        verify(accountService,times(1)).validateCurrency(anyLong(),anyLong());
        verify(accountService,times(0)).withdrawFromAccount(anyLong(),anyLong(),anyLong());
        verify(accountService,times(0)).depositToAccount(anyLong(),anyLong(),anyLong());
    }
    @Test
    public void uncorrectedAmountToWithdraw_ThenThrowInvalidAmountException_CallRepositoryTwice(){
        doNothing().when(accountService).validateCurrency(anyLong(),anyLong());
        when(accountService.withdrawFromAccount(anyLong(),anyLong(),anyLong()))
                .thenThrow(InvalidAmountException.class);

        assertThrows(InvalidAmountException.class,
                () -> out.transfer(getTestRandomLong(), TEST_EMPTY_TRANSFER_REQUEST)
        );

        verify(accountService,times(1)).validateCurrency(anyLong(),anyLong());
        verify(accountService,times(1)).withdrawFromAccount(anyLong(),anyLong(),anyLong());
        verify(accountService,times(0)).depositToAccount(anyLong(),anyLong(),anyLong());
    }
    @Test
    public void uncorrectedAmountToDeposit_ThenThrowInvalidAmountException_CallRepositoryThreeTimes(){
        doNothing().when(accountService).validateCurrency(anyLong(),anyLong());
        when(accountService.withdrawFromAccount(anyLong(),anyLong(),anyLong()))
                .thenReturn(AccountDTO.from(getTestAccount(getTestUser(),AccountCurrency.RUB)));
        when(accountService.depositToAccount(anyLong(),anyLong(),anyLong()))
                .thenThrow(InvalidAmountException.class);

        assertThrows(InvalidAmountException.class,
                () -> out.transfer(getTestRandomLong(), TEST_EMPTY_TRANSFER_REQUEST)
        );

        verify(accountService,times(1)).validateCurrency(anyLong(),anyLong());
        verify(accountService,times(1)).withdrawFromAccount(anyLong(),anyLong(),anyLong());
        verify(accountService,times(1)).depositToAccount(anyLong(),anyLong(),anyLong());
    }

}
