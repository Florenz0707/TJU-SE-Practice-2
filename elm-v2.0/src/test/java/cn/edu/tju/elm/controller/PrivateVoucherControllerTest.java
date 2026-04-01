package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivateVoucherControllerTest {

  @Mock private UserService userService;
  @Mock private PublicVoucherService publicVoucherService;
  @Mock private PrivateVoucherService privateVoucherService;
  @Mock private WalletService walletService;

  @InjectMocks private PrivateVoucherController privateVoucherController;

  @Test
  void claimPublicVoucher_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = privateVoucherController.claimPublicVoucher(100L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), result.getCode());
    assertEquals("用户未登录", result.getMessage());
    verify(publicVoucherService, never()).getPublicVoucherById(100L);
  }

  @Test
  void claimPublicVoucher_shouldFailWhenPublicVoucherMissing() {
    User user = new User();
    user.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(publicVoucherService.getPublicVoucherById(100L)).thenReturn(null);

    var result = privateVoucherController.claimPublicVoucher(100L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), result.getCode());
    assertEquals("PublicVoucher NOT FOUND", result.getMessage());
    verify(walletService, never()).getWalletByOwnerId(9L);
  }

  @Test
  void claimPublicVoucher_shouldFailWhenWalletMissing() {
    User user = new User();
    user.setId(9L);
    PublicVoucherVO publicVoucherVO = mock(PublicVoucherVO.class);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(publicVoucherService.getPublicVoucherById(100L)).thenReturn(publicVoucherVO);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(null);

    var result = privateVoucherController.claimPublicVoucher(100L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), result.getCode());
    assertEquals("Wallet NOT FOUND", result.getMessage());
    verify(privateVoucherService, never()).createPrivateVoucher(8L, publicVoucherVO);
  }

  @Test
  void claimPublicVoucher_shouldReturnSuccessWhenVoucherCreated() {
    User user = new User();
    user.setId(9L);
    PublicVoucherVO publicVoucherVO = mock(PublicVoucherVO.class);
    WalletVO walletVO = new WalletVO(8L, BigDecimal.TEN, BigDecimal.ZERO, 9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(publicVoucherService.getPublicVoucherById(100L)).thenReturn(publicVoucherVO);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(walletVO);
    when(privateVoucherService.createPrivateVoucher(8L, publicVoucherVO)).thenReturn(true);

    var result = privateVoucherController.claimPublicVoucher(100L);

    assertTrue(result.getSuccess());
    assertEquals("Claimed", result.getData());
    verify(privateVoucherService).createPrivateVoucher(8L, publicVoucherVO);
  }

  @Test
  void claimPublicVoucher_shouldReturnFailureWhenCreateReturnsFalse() {
    User user = new User();
    user.setId(9L);
    PublicVoucherVO publicVoucherVO = mock(PublicVoucherVO.class);
    WalletVO walletVO = new WalletVO(8L, BigDecimal.TEN, BigDecimal.ZERO, 9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(publicVoucherService.getPublicVoucherById(100L)).thenReturn(publicVoucherVO);
    when(walletService.getWalletByOwnerId(9L)).thenReturn(walletVO);
    when(privateVoucherService.createPrivateVoucher(8L, publicVoucherVO)).thenReturn(false);

    var result = privateVoucherController.claimPublicVoucher(100L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("Claim Failed", result.getMessage());
  }

  @Test
  void myPrivateVouchers_shouldReturnCurrentUserVouchers() {
    User user = new User();
    user.setId(9L);
    List<PrivateVoucherVO> vouchers = List.of(mock(PrivateVoucherVO.class), mock(PrivateVoucherVO.class));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(privateVoucherService.getPrivateVouchers(9L)).thenReturn(vouchers);

    var result = privateVoucherController.myPrivateVouchers();

    assertTrue(result.getSuccess());
    assertSame(vouchers, result.getData());
    verify(privateVoucherService).getPrivateVouchers(9L);
  }

  @Test
  void myPrivateVouchers_shouldReturnFailureWhenServiceThrows() {
    User user = new User();
    user.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(privateVoucherService.getPrivateVouchers(9L))
        .thenThrow(new PrivateVoucherException("voucher query failed"));

    var result = privateVoucherController.myPrivateVouchers();

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("voucher query failed", result.getMessage());
  }

  @Test
  void redeem_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = privateVoucherController.redeem(77L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), result.getCode());
    assertEquals("用户未登录", result.getMessage());
    verify(privateVoucherService, never()).redeemPrivateVoucher(77L);
  }

  @Test
  void redeem_shouldReturnSuccessWhenVoucherRedeemed() {
    User user = new User();
    user.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(privateVoucherService.redeemPrivateVoucher(77L)).thenReturn(true);

    var result = privateVoucherController.redeem(77L);

    assertTrue(result.getSuccess());
    assertEquals("Redeemed", result.getData());
    verify(privateVoucherService).redeemPrivateVoucher(77L);
  }

  @Test
  void redeem_shouldReturnFailureWhenVoucherCannotBeRedeemed() {
    User user = new User();
    user.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(privateVoucherService.redeemPrivateVoucher(77L)).thenReturn(false);

    var result = privateVoucherController.redeem(77L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("Redeem Failed or Expired", result.getMessage());
  }

  @Test
  void redeem_shouldReturnFailureWhenServiceThrows() {
    User user = new User();
    user.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));
    when(privateVoucherService.redeemPrivateVoucher(77L))
        .thenThrow(new PrivateVoucherException("redeem failed"));

    var result = privateVoucherController.redeem(77L);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals("redeem failed", result.getMessage());
  }
}