package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.AccountInternalService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
}
