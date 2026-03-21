package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountInternalServiceTest {
  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private PrivateVoucherRepository privateVoucherRepository;
  @Mock private PrivateVoucherService privateVoucherService;

  @InjectMocks private AccountInternalService accountInternalService;

  @Test
  void walletDebit_shouldBeIdempotent_whenRequestIdExists() {
    Transaction existing = Transaction.createNewTransaction(new BigDecimal("10"), 2, null, null);
    existing.setRequestId("req-1");
    when(transactionRepository.findByRequestId("req-1")).thenReturn(Optional.of(existing));

    var result =
        accountInternalService.walletDebit("req-1", 9L, new BigDecimal("10"), "biz-1", "reason");

    assertNotNull(result);
    assertEquals("req-1", result.getRequestId());
    verify(walletRepository, never()).findByOwnerId(any());
  }

  @Test
  void walletDebit_shouldReturnNull_whenBalanceInsufficient() {
    when(transactionRepository.findByRequestId("req-2")).thenReturn(Optional.empty());
    Wallet wallet = Wallet.getNewWallet(9L);
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.of(wallet));

    var result =
        accountInternalService.walletDebit("req-2", 9L, new BigDecimal("99"), "biz-2", "reason");

    assertNull(result);
  }

  @Test
  void walletRefund_shouldCreateTransaction_whenValid() {
    when(transactionRepository.findByRequestId("req-3")).thenReturn(Optional.empty());
    Wallet wallet = Wallet.getNewWallet(9L);
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.of(wallet));
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        accountInternalService.walletRefund("req-3", 9L, new BigDecimal("20"), "biz-3", "refund");

    assertNotNull(result);
    assertEquals("req-3", result.getRequestId());
    verify(transactionRepository).save(any(Transaction.class));
  }
}
