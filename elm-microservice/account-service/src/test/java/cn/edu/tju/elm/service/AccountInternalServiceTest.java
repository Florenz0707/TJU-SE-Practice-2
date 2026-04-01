package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountInternalServiceTest {
  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private PrivateVoucherRepository privateVoucherRepository;
  @Mock private PrivateVoucherService privateVoucherService;
  @Mock private WalletService walletService;
  @Mock private TransactionService transactionService;

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
  void walletDebit_shouldReturnNull_whenRequestMissingRequiredFields() {
    var result = accountInternalService.walletDebit(null, 9L, new BigDecimal("10"), "biz-2", "reason");

    assertNull(result);
    verify(transactionRepository, never()).findByRequestId(any());
    verify(walletRepository, never()).findByOwnerId(any());
  }

  @Test
  void walletDebit_shouldReturnNull_whenWalletMissing() {
    when(transactionRepository.findByRequestId("req-wallet-missing")).thenReturn(Optional.empty());
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.empty());

    var result =
        accountInternalService.walletDebit(
            "req-wallet-missing", 9L, new BigDecimal("10"), "biz-2", "reason");

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

  @Test
  void walletRefund_shouldBeIdempotent_whenRequestIdExists() {
    Transaction existing = Transaction.createNewTransaction(new BigDecimal("20"), 1, null, null);
    existing.setRequestId("req-refund-1");
    when(transactionRepository.findByRequestId("req-refund-1")).thenReturn(Optional.of(existing));

    var result =
        accountInternalService.walletRefund(
            "req-refund-1", 9L, new BigDecimal("20"), "biz-3", "refund");

    assertNotNull(result);
    assertEquals("req-refund-1", result.getRequestId());
    verify(walletRepository, never()).findByOwnerId(any());
  }

  @Test
  void rollbackVoucher_shouldReturnFalse_whenVoucherDoesNotBelongToUser() {
    Wallet wallet = Wallet.getNewWallet(10L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getWallet()).thenReturn(wallet);
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));

    var result = accountInternalService.rollbackVoucher("req-4", 9L, 100L, "ORD_1", "cancel");

    assertFalse(result);
    verify(privateVoucherService, never()).restoreVoucher(100L);
  }

  @Test
  void rollbackVoucher_shouldReturnTrueWithoutRestore_whenVoucherAlreadyActive() {
    Wallet wallet = Wallet.getNewWallet(9L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(false);
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));

    var result = accountInternalService.rollbackVoucher("req-5", 9L, 100L, "ORD_1", "cancel");

    assertTrue(result);
    verify(privateVoucherService, never()).restoreVoucher(100L);
  }

  @Test
  void rollbackVoucher_shouldRestoreDeletedVoucher_whenOwnedByUser() {
    Wallet wallet = Wallet.getNewWallet(9L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(true);
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));

    var result = accountInternalService.rollbackVoucher("req-6", 9L, 100L, "ORD_1", "cancel");

    assertTrue(result);
    verify(privateVoucherService).restoreVoucher(100L);
  }

  @Test
  void redeemVoucher_shouldReturnTrue_whenVoucherAlreadyDeleted() {
    Wallet wallet = Wallet.getNewWallet(9L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(true);
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));

    var result = accountInternalService.redeemVoucher("req-7", 9L, 100L, "ORD_1");

    assertTrue(result);
    verify(privateVoucherService, never()).redeemPrivateVoucher(100L);
  }

  @Test
  void redeemVoucher_shouldDelegateToService_whenVoucherOwnedAndActive() {
    Wallet wallet = Wallet.getNewWallet(9L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(false);
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));
    when(privateVoucherService.redeemPrivateVoucher(100L)).thenReturn(true);

    var result = accountInternalService.redeemVoucher("req-8", 9L, 100L, "ORD_1");

    assertTrue(result);
    verify(privateVoucherService).redeemPrivateVoucher(100L);
  }

  @Test
  void getWalletByUserId_shouldCreateWallet_whenMissingAndCreateFlagTrue() {
    when(walletRepository.findByOwnerId(77L)).thenReturn(Optional.empty());
    Wallet newWallet = Wallet.getNewWallet(77L);
    when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

    var result = accountInternalService.getWalletByUserId(77L, true);

    assertNotNull(result);
    assertEquals(77L, result.getOwnerId());
    verify(walletRepository).save(any(Wallet.class));
  }

  @Test
  void getVoucherSnapshotById_shouldReturnSnapshot_whenVoucherExists() {
    Wallet wallet = Wallet.getNewWallet(9L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getId()).thenReturn(100L);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(false);
    when(voucher.getFaceValue()).thenReturn(new BigDecimal("8.8"));
    when(privateVoucherRepository.findById(100L)).thenReturn(Optional.of(voucher));

    var result = accountInternalService.getVoucherSnapshotById(100L);

    assertNotNull(result);
    assertEquals(100L, result.getId());
    assertEquals(9L, result.getOwnerId());
  }
}
