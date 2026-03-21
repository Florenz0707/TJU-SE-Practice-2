package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Tag(name = "管理交易", description = "提供钱包交易的创建和查询功能")
public class TransactionController {
  private final WalletService walletService;
  private final TransactionService transactionService;

  public TransactionController(WalletService walletService, TransactionService transactionService) {
    this.walletService = walletService;
    this.transactionService = transactionService;
  }

  private boolean hasAdminAuthority() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(authority -> authority.getAuthority().toUpperCase())
        .anyMatch(name -> "ADMIN".equals(name) || "ROLE_ADMIN".equals(name));
  }

  @GetMapping("/{id}")
  @Operation(summary = "根据ID获取交易", description = "查询指定交易的详细信息")
  public HttpResult<TransactionVO> getTransactionById(
      @Parameter(description = "交易ID", required = true) @PathVariable("id") Long id) {
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
  @Operation(summary = "根据钱包ID获取交易列表", description = "查询指定钱包的所有交易记录")
  public HttpResult<TransactionsRecord> getTransactionsByWalletId(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long walletId) {
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
  @Operation(summary = "获取我的交易记录", description = "查询当前用户钱包的所有交易记录")
  public HttpResult<TransactionsRecord> getMyTransactions() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }

    try {
      var walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "钱包不存在");
      }

      TransactionsRecord transactionsRecord =
          transactionService.getTransactionsByWalletId(walletVO.getId());
      return HttpResult.success(transactionsRecord);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("")
  @Operation(summary = "创建交易", description = "创建充值、提现或转账交易")
  public HttpResult<TransactionVO> createTransaction(
      @Parameter(description = "交易信息", required = true) @RequestBody TransactionVO transactionVO) {
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
  @Operation(summary = "完成交易", description = "标记交易为已完成状态")
  public HttpResult<TransactionVO> finishTransaction(
      @Parameter(description = "交易ID", required = true) @RequestParam Long id) {
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
