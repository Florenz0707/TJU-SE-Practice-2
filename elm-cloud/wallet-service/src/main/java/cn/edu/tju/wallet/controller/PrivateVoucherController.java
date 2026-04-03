package cn.edu.tju.wallet.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.wallet.exception.PrivateVoucherException;
import cn.edu.tju.wallet.model.VO.PrivateVoucherVO;
import cn.edu.tju.wallet.model.VO.PublicVoucherVO;
import cn.edu.tju.wallet.model.VO.WalletVO;
import cn.edu.tju.wallet.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.wallet.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.wallet.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privateVoucher")

public class PrivateVoucherController {
  private final PublicVoucherService publicVoucherService;
  private final PrivateVoucherService privateVoucherService;
  private final WalletService walletService;

  public PrivateVoucherController(
      PublicVoucherService publicVoucherService,
      PrivateVoucherService privateVoucherService,
      WalletService walletService) {
    this.publicVoucherService = publicVoucherService;
    this.privateVoucherService = privateVoucherService;
    this.walletService = walletService;
  }

  @PostMapping("/claim/{publicVoucherId}")
  
  public HttpResult<String> claimPublicVoucher(
      @PathVariable("publicVoucherId") Long publicVoucherId) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
    }
    try {
      PublicVoucherVO publicVoucherVO = publicVoucherService.getPublicVoucherById(publicVoucherId);
      if (publicVoucherVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "PublicVoucher NOT FOUND");
      }
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
      }
      boolean ok = privateVoucherService.createPrivateVoucher(walletVO.getId(), publicVoucherVO);
      if (ok) {
        return HttpResult.success("Claimed");
      }
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Claim Failed");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/my")
  @Operation(summary = "My Private Vouchers")
  public HttpResult<List<PrivateVoucherVO>> myPrivateVouchers() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
    }
    WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
    List<PrivateVoucherVO> vouchers = privateVoucherService.getPrivateVouchers(walletVO.getId());
    return HttpResult.success(vouchers);
  }

  @PostMapping("/redeem/{id}")
  @Operation(summary = "Redeem Private Voucher")
  public HttpResult<String> redeem(
      @Parameter(description = "privateVoucher id") @PathVariable("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
    }
    try {
      boolean ok = privateVoucherService.redeemPrivateVoucher(id);
      if (ok) {
        return HttpResult.success("Redeemed");
      }
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Redeem Failed or Expired");
    } catch (PrivateVoucherException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}

