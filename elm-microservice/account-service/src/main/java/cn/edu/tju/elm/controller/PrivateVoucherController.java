package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
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
@Tag(name = "管理私有优惠券", description = "提供用户优惠券的领取和使用功能")
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
  @Operation(summary = "领取公共优惠券", description = "用户领取平台发放的公共优惠券到个人钱包")
  public HttpResult<String> claimPublicVoucher(
      @Parameter(description = "公共优惠券ID", required = true) @PathVariable Long publicVoucherId) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
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
  @Operation(summary = "获取我的优惠券", description = "查询当前用户的所有优惠券")
  public HttpResult<List<PrivateVoucherVO>> myPrivateVouchers() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    try {
      List<PrivateVoucherVO> list = privateVoucherService.getPrivateVouchers(currentUserId);
      return HttpResult.success(list);
    } catch (PrivateVoucherException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/redeem/{id}")
  @Operation(summary = "使用优惠券", description = "在订单中使用指定优惠券")
  public HttpResult<String> redeem(
      @Parameter(description = "私有优惠券ID", required = true) @PathVariable Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
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
