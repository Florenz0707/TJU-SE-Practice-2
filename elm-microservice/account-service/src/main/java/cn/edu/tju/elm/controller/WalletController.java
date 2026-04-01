package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "管理钱包", description = "提供钱包的创建和查询功能")
public class WalletController {
  private final WalletService walletService;
  private final TransactionService transactionService;

  public WalletController(WalletService walletService, TransactionService transactionService) {
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

  @GetMapping("/owner/{id}")
  @Operation(summary = "根据钱包ID获取所有者", description = "查询指定钱包的所有者信息")
  public HttpResult<Long> getWalletOwnerById(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long id) {
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
  @Operation(summary = "根据ID获取钱包", description = "查询指定钱包的详细信息")
  public HttpResult<WalletVO> getWalletById(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long id) {
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
  @Operation(summary = "添加优惠券到钱包", description = "向指定钱包添加优惠券")
  public HttpResult<String> addVoucher(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long walletId,
      @Parameter(description = "优惠券金额", required = true) @RequestBody BigDecimal amount) {
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
  @Operation(summary = "获取我的钱包", description = "获取当前用户的钱包信息")
  public HttpResult<WalletVO> getWalletByAuthorization() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }

    try {
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      return HttpResult.success(walletVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("")
  @Operation(summary = "创建钱包", description = "为当前用户创建新钱包")
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
  @Operation(summary = "钱包充值", description = "为当前用户钱包充值")
  public HttpResult<TransactionVO> topup(
      @Parameter(description = "充值金额", required = true) @RequestBody BigDecimal amount) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "充值金额必须大于0");
    }

    try {
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "钱包不存在，请先创建钱包");
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
  @Operation(summary = "钱包提现", description = "从当前用户钱包提现")
  public HttpResult<TransactionVO> withdraw(
      @Parameter(description = "提现金额", required = true) @RequestBody BigDecimal amount) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "提现金额必须大于0");
    }

    try {
      WalletVO walletVO = walletService.getWalletByOwnerId(currentUserId);
      if (walletVO == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "钱包不存在");
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
