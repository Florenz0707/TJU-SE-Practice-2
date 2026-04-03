package cn.edu.tju.wallet.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.wallet.constant.TransactionType;
import cn.edu.tju.wallet.model.RECORD.TransactionsRecord;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import cn.edu.tju.wallet.service.serviceInterface.TransactionService;
import cn.edu.tju.wallet.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transaction")

public class TransactionController {
  private final WalletService walletService;
  private final TransactionService transactionService;

  public TransactionController(WalletService walletService, TransactionService transactionService) {
    this.walletService = walletService;
    this.transactionService = transactionService;
  }

  private boolean hasAdminAuthority() {
    return false;
  }

  @GetMapping("/{id}")
  
  public HttpResult<TransactionVO> getTransactionById(
      @PathVariable("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      TransactionVO transactionVO = transactionService.getTransactionById(id);
      return HttpResult.success(transactionVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/list/{walletId}")
  
  public HttpResult<TransactionsRecord> getTransactionsByWalletId(
      @PathVariable("walletId") Long walletId) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (walletId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");
    }

    Long ownerId = walletService.getWalletOwnerIdById(walletId);
    if (ownerId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
    }
    if (!currentUserId.equals(ownerId)) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    try {
      TransactionsRecord transactionsRecord =
          transactionService.getTransactionsByWalletId(walletId);
      return HttpResult.success(transactionsRecord);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/my")
  
  public HttpResult<TransactionsRecord> getMyTransactions() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }

    try {
      var walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Unauthenticated");
    }
  }

  @PostMapping("")
  
  public HttpResult<TransactionVO> createTransaction(
      @RequestBody TransactionVO transactionVO) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (transactionVO == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "TransactionVO CANT BE NULL");
    }
    if (transactionVO.getAmount() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
    }
    if (transactionVO.getType() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Type CANT BE NULL");
    }
    if (!TransactionType.isValidTransactionType(transactionVO.getType())) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Type NOT VALID");
    }
    if (transactionVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Amount NOT VALID");
    }

    if (transactionVO.getType().equals(TransactionType.PAYMENT)) {
      if (transactionVO.getInWalletId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
      }
      if (transactionVO.getOutWalletId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
      }
    } else if (transactionVO.getType().equals(TransactionType.TOP_UP)) {
      if (transactionVO.getInWalletId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
      }
    } else if (transactionVO.getType().equals(TransactionType.WITHDRAW)) {
      if (transactionVO.getOutWalletId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
      }
    }

    try {
      TransactionVO retTransactionVO =
          transactionService.createTransaction(
              transactionVO.getAmount(),
              transactionVO.getType(),
              transactionVO.getInWalletId(),
              transactionVO.getOutWalletId());
      return HttpResult.success(retTransactionVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PatchMapping("/finished")
  
  public HttpResult<TransactionVO> finishTransaction(
      @RequestParam("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      TransactionVO transactionVO =
          transactionService.finishTransaction(id, currentUserId, hasAdminAuthority());
      return HttpResult.success(transactionVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}



