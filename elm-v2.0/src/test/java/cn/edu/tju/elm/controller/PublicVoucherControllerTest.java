package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicVoucherControllerTest {

  @Mock private PublicVoucherService publicVoucherService;
  @Mock private UserService userService;
  @Mock private WalletRepository walletRepository;
  @Mock private PrivateVoucherRepository privateVoucherRepository;

  @InjectMocks private PublicVoucherController publicVoucherController;

  @Test
  void createPublicVoucher_shouldFailWhenVoucherInvalid() {
    var result = publicVoucherController.createPublicVoucher(null);

    assertFalse(result.getSuccess());
    assertEquals("PublicVoucher CANT BE NULL", result.getMessage());
    verify(publicVoucherService, never()).createPublicVoucher(null);
  }

  @Test
  void deletePublicVoucher_shouldFailWhenIdMissing() {
    var result = publicVoucherController.deletePublicVoucher(null);

    assertFalse(result.getSuccess());
    assertEquals("ID CANT BE NULL", result.getMessage());
    verify(publicVoucherService, never()).deletePublicVoucher(null);
  }

  @Test
  void getAvailablePublicVouchers_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = publicVoucherController.getAvailablePublicVouchers();

    assertFalse(result.getSuccess());
    assertEquals("用户未登录", result.getMessage());
    verify(walletRepository, never()).findByOwnerId(9L);
  }

  @Test
  void getAvailablePublicVouchers_shouldFailWhenWalletMissing() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.empty());

    var result = publicVoucherController.getAvailablePublicVouchers();

    assertFalse(result.getSuccess());
    assertEquals("钱包未找到", result.getMessage());
    verify(publicVoucherService, never()).getPublicVouchers();
  }

  @Test
  void getAvailablePublicVouchers_shouldFilterOutUnclaimableAndClaimedVouchers() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Wallet wallet = new Wallet();
    wallet.setId(100L);
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.of(wallet));

    PublicVoucherVO availableVoucher = mock(PublicVoucherVO.class);
    when(availableVoucher.getId()).thenReturn(1L);
    when(availableVoucher.getClaimable()).thenReturn(true);

    PublicVoucherVO claimedVoucher = mock(PublicVoucherVO.class);
    when(claimedVoucher.getId()).thenReturn(2L);
    when(claimedVoucher.getClaimable()).thenReturn(true);

    PublicVoucherVO unclaimableVoucher = mock(PublicVoucherVO.class);
    when(unclaimableVoucher.getClaimable()).thenReturn(false);

    List<PublicVoucherVO> vouchers = List.of(availableVoucher, claimedVoucher, unclaimableVoucher);
    when(publicVoucherService.getPublicVouchers()).thenReturn(vouchers);
    when(privateVoucherRepository.existsByWalletIdAndPublicVoucherId(100L, 1L)).thenReturn(false);
    when(privateVoucherRepository.existsByWalletIdAndPublicVoucherId(100L, 2L)).thenReturn(true);

    var result = publicVoucherController.getAvailablePublicVouchers();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
    assertSame(availableVoucher, result.getData().getFirst());
  }

  @Test
  void getAvailablePublicVouchers_shouldReturnFailureWhenServiceThrows() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Wallet wallet = new Wallet();
    wallet.setId(100L);
    when(walletRepository.findByOwnerId(9L)).thenReturn(Optional.of(wallet));
    when(publicVoucherService.getPublicVouchers()).thenThrow(new RuntimeException("voucher service down"));

    var result = publicVoucherController.getAvailablePublicVouchers();

    assertFalse(result.getSuccess());
    assertEquals("voucher service down", result.getMessage());
  }
}