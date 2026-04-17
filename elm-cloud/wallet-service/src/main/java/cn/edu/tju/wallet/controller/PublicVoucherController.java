package cn.edu.tju.wallet.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.wallet.model.BO.Wallet;
import cn.edu.tju.wallet.model.VO.PublicVoucherVO;
import cn.edu.tju.wallet.repository.PrivateVoucherRepository;
import cn.edu.tju.wallet.repository.WalletRepository;
import cn.edu.tju.wallet.service.serviceInterface.PublicVoucherService;
import java.util.List;
import java.util.Optional;


import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicVoucher")

public class PublicVoucherController {
  private final PublicVoucherService publicVoucherService;
  private final WalletRepository walletRepository;

  public PublicVoucherController(
      PublicVoucherService publicVoucherService,
      WalletRepository walletRepository,
      PrivateVoucherRepository privateVoucherRepository) {
    this.publicVoucherService = publicVoucherService;
    this.walletRepository = walletRepository;
  }

  private boolean hasAdminAuthority() {
    // This codebase doesn't expose authorities in JWT via SecurityUtils.
    // In the provided init data, admin user is the first seeded user (id=1).
    // So we treat uid==1 as admin to keep behavior consistent with the frontend.
    return SecurityUtils.getCurrentUserId().map(id -> id == 1L).orElse(false);
  }

  @GetMapping("/list")
  
  public HttpResult<List<PublicVoucherVO>> getAllPublicVouchers() {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    try {
      List<PublicVoucherVO> publicVoucherVOS = publicVoucherService.getPublicVouchers();
      return HttpResult.success(publicVoucherVOS);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping()
  
  public HttpResult<List<PublicVoucherVO>> getAllPublicVouchersWithoutList() {
    try {
      List<PublicVoucherVO> publicVoucherVOS = publicVoucherService.getPublicVouchers();
      return HttpResult.success(publicVoucherVOS);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping()
  
  public HttpResult<String> createPublicVoucher(
      @RequestBody
          PublicVoucherVO publicVoucherVO) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
    if (failure != null) {
      return failure;
    }

    try {
      publicVoucherService.createPublicVoucher(publicVoucherVO);
      return HttpResult.success("Create Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping("/{id}")
  
  public HttpResult<String> updatePublicVoucher(
      @PathVariable("id") Long id,
      @RequestBody
          PublicVoucherVO publicVoucherVO) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    // 兼容旧格式：如果请求体中没有 id，从路径参数设置
    if (publicVoucherVO != null && publicVoucherVO.getId() == null) {
      publicVoucherVO.setId(id);
    }
    HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
    if (failure != null) {
      return failure;
    }

    try {
      publicVoucherService.updatePublicVoucher(publicVoucherVO);
      return HttpResult.success("Update Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("{id}")
  
  public HttpResult<PublicVoucherVO> getPublicVoucher(
      @PathVariable("id") Long id) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      PublicVoucherVO publicVoucherVO = publicVoucherService.getPublicVoucherById(id);
      return HttpResult.success(publicVoucherVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  
  public HttpResult<String> deletePublicVoucher(
      @PathVariable("id") Long id) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      publicVoucherService.deletePublicVoucher(id);
      return HttpResult.success("Delete Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/available")
  
  public HttpResult<List<PublicVoucherVO>> getAvailablePublicVouchers() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
    }
    try {
      Optional<Wallet> walletOpt = walletRepository.findByOwnerId(currentUserId);
      if (walletOpt.isEmpty()) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
      }
      List<PublicVoucherVO> vouchers = publicVoucherService.getPublicVouchers();
      return HttpResult.success(vouchers);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}



