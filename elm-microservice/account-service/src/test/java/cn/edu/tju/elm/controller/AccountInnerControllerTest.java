package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.InternalVoucherSnapshotVO;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.AccountInternalService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountInnerControllerTest {
  @Mock private AccountInternalService accountInternalService;

  @InjectMocks private AccountInnerController accountInnerController;

  @Test
  void walletDebit_shouldReturnSuccess_whenServiceReturnsTransaction() {
    AccountInnerController.WalletDebitRequest request =
        new AccountInnerController.WalletDebitRequest();
    request.setRequestId("req-1");
    request.setUserId(9L);
    request.setAmount(new BigDecimal("12.34"));
    request.setBizId("BIZ_1");
    request.setReason("test");

    TransactionVO transaction = new TransactionVO();
    transaction.setRequestId("req-1");
    when(accountInternalService.walletDebit("req-1", 9L, new BigDecimal("12.34"), "BIZ_1", "test"))
        .thenReturn(transaction);

    var result = accountInnerController.walletDebit(request);

    assertTrue(result.getSuccess());
    assertEquals("req-1", result.getData().getRequestId());
    verify(accountInternalService)
        .walletDebit("req-1", 9L, new BigDecimal("12.34"), "BIZ_1", "test");
  }

  @Test
  void walletDebit_shouldReturnFailure_whenServiceReturnsNull() {
    AccountInnerController.WalletDebitRequest request =
        new AccountInnerController.WalletDebitRequest();
    request.setRequestId("req-2");
    request.setUserId(10L);
    request.setAmount(new BigDecimal("99.00"));

    when(accountInternalService.walletDebit("req-2", 10L, new BigDecimal("99.00"), null, null))
        .thenReturn(null);

    var result = accountInnerController.walletDebit(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("wallet debit failed", result.getMessage());
  }

  @Test
  void walletDebit_shouldReturnFailure_whenServiceThrows() {
    AccountInnerController.WalletDebitRequest request =
        new AccountInnerController.WalletDebitRequest();
    request.setRequestId("req-throw");
    request.setUserId(10L);
    request.setAmount(new BigDecimal("99.00"));

    when(accountInternalService.walletDebit("req-throw", 10L, new BigDecimal("99.00"), null, null))
        .thenThrow(new RuntimeException("wallet timeout"));

    var result = accountInnerController.walletDebit(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("wallet timeout", result.getMessage());
  }

  @Test
  void rollbackVoucher_shouldReturnTrue_whenServiceReturnsTrue() {
    AccountInnerController.VoucherRollbackRequest request =
        new AccountInnerController.VoucherRollbackRequest();
    request.setRequestId("req-3");
    request.setUserId(9L);
    request.setVoucherId(100L);
    request.setOrderId("ORD_9");
    request.setReason("cancel");

    when(accountInternalService.rollbackVoucher("req-3", 9L, 100L, "ORD_9", "cancel"))
        .thenReturn(true);

    var result = accountInnerController.rollbackVoucher(request);

    assertTrue(result.getSuccess());
    assertTrue(result.getData());
    verify(accountInternalService).rollbackVoucher("req-3", 9L, 100L, "ORD_9", "cancel");
  }

  @Test
  void rollbackVoucher_shouldReturnFalse_whenServiceReturnsFalse() {
    AccountInnerController.VoucherRollbackRequest request =
        new AccountInnerController.VoucherRollbackRequest();
    request.setRequestId("req-4");
    request.setUserId(9L);
    request.setVoucherId(100L);

    when(accountInternalService.rollbackVoucher("req-4", 9L, 100L, null, null)).thenReturn(false);

    var result = accountInnerController.rollbackVoucher(request);

    assertTrue(result.getSuccess());
    assertFalse(result.getData());
  }

  @Test
  void rollbackVoucher_shouldReturnFailure_whenServiceThrows() {
    AccountInnerController.VoucherRollbackRequest request =
        new AccountInnerController.VoucherRollbackRequest();
    request.setRequestId("req-5");
    request.setUserId(9L);
    request.setVoucherId(100L);

    when(accountInternalService.rollbackVoucher("req-5", 9L, 100L, null, null))
        .thenThrow(new RuntimeException("restore failed"));

    var result = accountInnerController.rollbackVoucher(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("restore failed", result.getMessage());
  }

  @Test
  void walletRefund_shouldReturnFailure_whenServiceReturnsNull() {
    AccountInnerController.WalletRefundRequest request =
        new AccountInnerController.WalletRefundRequest();
    request.setRequestId("refund-1");
    request.setUserId(9L);
    request.setAmount(new BigDecimal("8.8"));

    when(accountInternalService.walletRefund("refund-1", 9L, new BigDecimal("8.8"), null, null))
        .thenReturn(null);

    var result = accountInnerController.walletRefund(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("wallet refund failed", result.getMessage());
  }

  @Test
  void redeemVoucher_shouldReturnFailure_whenServiceThrows() {
    AccountInnerController.VoucherRedeemRequest request =
        new AccountInnerController.VoucherRedeemRequest();
    request.setRequestId("redeem-1");
    request.setUserId(9L);
    request.setVoucherId(88L);
    request.setOrderId("ORD_9");

    when(accountInternalService.redeemVoucher("redeem-1", 9L, 88L, "ORD_9"))
        .thenThrow(new RuntimeException("redeem failed"));

    var result = accountInnerController.redeemVoucher(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("redeem failed", result.getMessage());
  }

  @Test
  void getWalletByUserId_shouldReturnWallet_whenServiceReturnsWallet() {
    Wallet wallet = Wallet.getNewWallet(9L);
    WalletVO walletVO = new WalletVO(wallet);
    when(accountInternalService.getWalletByUserId(9L, true)).thenReturn(walletVO);

    var result = accountInnerController.getWalletByUserId(9L, true);

    assertTrue(result.getSuccess());
    assertEquals(9L, result.getData().getOwnerId());
    verify(accountInternalService).getWalletByUserId(9L, true);
  }

  @Test
  void getVoucherSnapshot_shouldReturnSnapshot_whenServiceReturnsSnapshot() {
    Wallet wallet = Wallet.getNewWallet(10L);
    PrivateVoucher voucher = Mockito.mock(PrivateVoucher.class);
    when(voucher.getId()).thenReturn(88L);
    when(voucher.getWallet()).thenReturn(wallet);
    when(voucher.getDeleted()).thenReturn(false);
    when(voucher.getFaceValue()).thenReturn(new BigDecimal("6.5"));
    InternalVoucherSnapshotVO snapshot = new InternalVoucherSnapshotVO(voucher);
    when(accountInternalService.getVoucherSnapshotById(88L)).thenReturn(snapshot);

    var result = accountInnerController.getVoucherSnapshot(88L);

    assertTrue(result.getSuccess());
    assertEquals(88L, result.getData().getId());
    verify(accountInternalService).getVoucherSnapshotById(88L);
  }
}
