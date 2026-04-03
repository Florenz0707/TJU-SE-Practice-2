package cn.edu.tju.wallet.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.wallet.model.BO.Wallet;
import cn.edu.tju.wallet.model.VO.PublicVoucherVO;
import cn.edu.tju.wallet.repository.PrivateVoucherRepository;
import cn.edu.tju.wallet.repository.WalletRepository;
import cn.edu.tju.wallet.service.serviceInterface.PublicVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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
  private final PrivateVoucherRepository privateVoucherRepository;

  public PublicVoucherController(
      PublicVoucherService publicVoucherService,
      WalletRepository walletRepository,
      PrivateVoucherRepository privateVoucherRepository) {
    this.publicVoucherService = publicVoucherService;
    this.walletRepository = walletRepository;
    this.privateVoucherRepository = privateVoucherRepository;
  }

  private boolean hasAdminAuthority() {
    return false;
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

  @PutMapping()
  
  public HttpResult<String> updatePublicVoucher(
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



