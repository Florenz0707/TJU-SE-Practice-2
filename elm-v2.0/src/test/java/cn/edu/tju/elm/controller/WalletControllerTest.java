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
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

  @Mock private UserService userService;
  @Mock private WalletService walletService;
  @Mock private TransactionService transactionService;

  @InjectMocks private WalletController walletController;

  @Test
  void getWalletByAuthorization_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = walletController.getWalletByAuthorization();

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(walletService, never()).getWalletByOwnerId(9L);
  }

  @Test
  void getWalletById_shouldPassAdminFlag() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    WalletVO walletVO = new WalletVO(100L, BigDecimal.TEN, BigDecimal.ONE, 20L);
    when(walletService.getWalletById(100L, 9L, true)).thenReturn(walletVO);

    var result = walletController.getWalletById(100L);

    assertTrue(result.getSuccess());
    assertSame(walletVO, result.getData());
    verify(walletService).getWalletById(100L, 9L, true);
  }

  @Test
  void createWalletByAuthorization_shouldReturnCreatedWallet() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    WalletVO walletVO = new WalletVO(101L, BigDecimal.ZERO, BigDecimal.ZERO, 9L);
    when(walletService.createWallet(9L)).thenReturn(walletVO);

    var result = walletController.createWalletByAuthorization();

    assertTrue(result.getSuccess());
    assertSame(walletVO, result.getData());
    verify(walletService).createWallet(9L);
  }

  @Test
  void topup_shouldFailWhenAmountInvalid() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = walletController.topup(BigDecimal.ZERO);

    assertFalse(result.getSuccess());
    assertEquals("充值金额必须大于0", result.getMessage());
    verify(walletService, never()).getWalletByOwnerId(9L);
    verify(transactionService, never()).createTransaction(null, null, null, null);
  }

  @Test
  void topup_shouldFailWhenWalletMissing() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletService.getWalletByOwnerId(9L)).thenReturn(null);

    var result = walletController.topup(new BigDecimal("20"));

    assertFalse(result.getSuccess());
    assertEquals("钱包不存在，请先创建钱包", result.getMessage());
    verify(transactionService, never()).createTransaction(null, null, null, null);
  }

  @Test
  void topup_shouldCreateTopupTransaction() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    WalletVO walletVO = new WalletVO(102L, BigDecimal.TEN, BigDecimal.ZERO, 9L);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(walletVO);
    TransactionVO transactionVO = new TransactionVO();
    transactionVO.setId(501L);
    when(transactionService.createTransaction(
            new BigDecimal("20"), TransactionType.TOP_UP, 102L, null))
        .thenReturn(transactionVO);

    var result = walletController.topup(new BigDecimal("20"));

    assertTrue(result.getSuccess());
    assertSame(transactionVO, result.getData());
    verify(transactionService)
        .createTransaction(new BigDecimal("20"), TransactionType.TOP_UP, 102L, null);
  }

  @Test
  void withdraw_shouldFailWhenAmountInvalid() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = walletController.withdraw(new BigDecimal("-1"));

    assertFalse(result.getSuccess());
    assertEquals("提现金额必须大于0", result.getMessage());
    verify(walletService, never()).getWalletByOwnerId(9L);
  }

  @Test
  void withdraw_shouldFailWhenWalletMissing() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletService.getWalletByOwnerId(9L)).thenReturn(null);

    var result = walletController.withdraw(new BigDecimal("10"));

    assertFalse(result.getSuccess());
    assertEquals("钱包不存在", result.getMessage());
    verify(transactionService, never()).createTransaction(null, null, null, null);
  }

  @Test
  void withdraw_shouldCreateWithdrawTransaction() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    WalletVO walletVO = new WalletVO(103L, new BigDecimal("50"), BigDecimal.ZERO, 9L);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(walletVO);
    TransactionVO transactionVO = new TransactionVO();
    transactionVO.setId(601L);
    when(transactionService.createTransaction(
            new BigDecimal("10"), TransactionType.WITHDRAW, null, 103L))
        .thenReturn(transactionVO);

    var result = walletController.withdraw(new BigDecimal("10"));

    assertTrue(result.getSuccess());
    assertSame(transactionVO, result.getData());
    verify(transactionService)
        .createTransaction(new BigDecimal("10"), TransactionType.WITHDRAW, null, 103L);
  }

  @Test
  void addVoucher_shouldFailWhenAmountNegative() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = walletController.addVoucher(100L, new BigDecimal("-5"));

    assertFalse(result.getSuccess());
    assertEquals("Amount CANT BE NEGATIVE", result.getMessage());
    verify(walletService, never()).addVoucher(100L, new BigDecimal("-5"));
  }
}