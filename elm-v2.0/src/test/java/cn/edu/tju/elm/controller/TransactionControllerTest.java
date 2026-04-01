package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  @Mock private UserService userService;
  @Mock private WalletService walletService;
  @Mock private TransactionService transactionService;

  @InjectMocks private TransactionController transactionController;

  @Test
  void getMyTransactions_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = transactionController.getMyTransactions();

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(walletService, never()).getWalletByOwnerId(9L);
  }

  @Test
  void getMyTransactions_shouldFailWhenWalletMissing() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletService.getWalletByOwnerId(9L)).thenReturn(null);

    var result = transactionController.getMyTransactions();

    assertFalse(result.getSuccess());
    assertEquals("钱包不存在", result.getMessage());
    verify(transactionService, never()).getTransactionsByWalletId(9L);
  }

  @Test
  void getTransactionsByWalletId_shouldFailWhenWalletBelongsToAnotherUser() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletService.getWalletOwnerIdById(100L)).thenReturn(10L);

    var result = transactionController.getTransactionsByWalletId(100L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(transactionService, never()).getTransactionsByWalletId(100L);
  }

  @Test
  void getTransactionsByWalletId_shouldReturnTransactionsWhenAuthorized() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletService.getWalletOwnerIdById(100L)).thenReturn(9L);
    TransactionsRecord record = new TransactionsRecord(List.of(), List.of());
    when(transactionService.getTransactionsByWalletId(100L)).thenReturn(record);

    var result = transactionController.getTransactionsByWalletId(100L);

    assertTrue(result.getSuccess());
    assertSame(record, result.getData());
  }

  @Test
  void createTransaction_shouldFailWhenTypeInvalid() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    TransactionVO request = new TransactionVO();
    request.setAmount(BigDecimal.ONE);
    request.setType(99);

    var result = transactionController.createTransaction(request);

    assertFalse(result.getSuccess());
    assertEquals("Type NOT VALID", result.getMessage());
    verify(transactionService, never()).createTransaction(null, null, null, null);
  }

  @Test
  void createTransaction_shouldFailWhenPaymentMissingWalletIds() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    TransactionVO request = new TransactionVO();
    request.setAmount(BigDecimal.TEN);
    request.setType(TransactionType.PAYMENT);
    request.setInWalletId(1L);

    var result = transactionController.createTransaction(request);

    assertFalse(result.getSuccess());
    assertEquals("OutWalletId CANT BE NULL", result.getMessage());
    verify(transactionService, never()).createTransaction(null, null, null, null);
  }

  @Test
  void createTransaction_shouldDelegateWhenRequestValid() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    TransactionVO request = new TransactionVO();
    request.setAmount(new BigDecimal("20"));
    request.setType(TransactionType.TOP_UP);
    request.setInWalletId(100L);
    TransactionVO created = new TransactionVO();
    created.setId(501L);
    when(transactionService.createTransaction(new BigDecimal("20"), TransactionType.TOP_UP, 100L, null))
        .thenReturn(created);

    var result = transactionController.createTransaction(request);

    assertTrue(result.getSuccess());
    assertSame(created, result.getData());
  }

  @Test
  void finishTransaction_shouldPassAdminFlag() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    TransactionVO finished = new TransactionVO();
    finished.setId(700L);
    when(transactionService.finishTransaction(700L, 9L, true)).thenReturn(finished);

    var result = transactionController.finishTransaction(700L);

    assertTrue(result.getSuccess());
    assertSame(finished, result.getData());
    verify(transactionService).finishTransaction(700L, 9L, true);
  }

  @Test
  void getMyTransactions_shouldReturnTransactionsForCurrentWallet() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    WalletVO walletVO = new WalletVO(101L, BigDecimal.TEN, BigDecimal.ZERO, 9L);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(walletVO);
    TransactionsRecord record = new TransactionsRecord(List.of(), List.of());
    when(transactionService.getTransactionsByWalletId(101L)).thenReturn(record);

    var result = transactionController.getMyTransactions();

    assertTrue(result.getSuccess());
    assertSame(record, result.getData());
  }
}