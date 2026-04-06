package cn.edu.tju.wallet.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.wallet.constant.TransactionType;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import cn.edu.tju.wallet.model.VO.WalletVO;
import cn.edu.tju.wallet.service.serviceInterface.TransactionService;
import cn.edu.tju.wallet.service.serviceInterface.WalletService;
import java.math.BigDecimal;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")

public class WalletController {
  private final WalletService walletService;
  private final TransactionService transactionService;

  public WalletController(WalletService walletService, TransactionService transactionService) {
    this.walletService = walletService;
    this.transactionService = transactionService;
  }

  private boolean hasAdminAuthority() {
    return false;
  }

  @GetMapping("/owner/{id}")
  
  public HttpResult<Long> getWalletOwnerById(
      @PathVariable("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");
    }

    try {
      Long ownerId = walletService.getWalletOwnerIdById(id);
      if (!hasAdminAuthority() && !currentUserId.equals(ownerId)) {
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
      }
      return HttpResult.success(ownerId);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("{id}")
  
  public HttpResult<WalletVO> getWalletById(
      @PathVariable("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");
    }

    try {
      WalletVO walletVO = walletService.getWalletById(id, currentUserId, hasAdminAuthority());
      return HttpResult.success(walletVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/voucher/{walletId}")
  
  public HttpResult<String> addVoucher(
      @PathVariable("walletId") Long walletId,
      @RequestBody BigDecimal amount) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (amount == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NEGATIVE");
    }

    try {
      Long ownerId = walletService.getWalletOwnerIdById(walletId);
      if (!hasAdminAuthority() && !currentUserId.equals(ownerId)) {
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
      }
      walletService.addVoucher(walletId, amount);
      return HttpResult.success("Add voucher successfully");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/my")
  
  public HttpResult<WalletVO> getWalletByAuthorization() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }

    try {
      // Frontend expects this endpoint to be safe to call and to always return a wallet.
      // If the user doesn't have a wallet yet, create one idempotently (owner_id is unique).
      WalletVO walletVO;
      try {
        walletVO = walletService.getWalletByOwnerId(currentUserId);
      } catch (Exception notFound) {
        walletVO = walletService.createWallet(currentUserId);
      }
      return HttpResult.success(walletVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("")
  
  public HttpResult<WalletVO> createWalletByAuthorization() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }

    try {
      WalletVO walletVO = walletService.createWallet(currentUserId);
      return HttpResult.success(walletVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/my/topup")
  
  public HttpResult<TransactionVO> topup(
      @RequestBody BigDecimal amount) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "鍏呭€奸噾棰濆繀椤诲ぇ浜?");
    }

    try {
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "閽卞寘涓嶅瓨鍦紝璇峰厛鍒涘缓閽卞寘");
      }
      TransactionVO transaction =
          transactionService.createTransaction(
              amount, TransactionType.TOP_UP, walletVO.getId(), null);
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/my/withdraw")
  
  public HttpResult<TransactionVO> withdraw(
      @RequestBody BigDecimal amount) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "鎻愮幇閲戦蹇呴』澶т簬0");
    }

    try {
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
      }
      TransactionVO transaction =
          transactionService.createTransaction(
              amount, TransactionType.WITHDRAW, null, walletVO.getId());
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}



