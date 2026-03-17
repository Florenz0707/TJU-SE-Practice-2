package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "管理交易", description = "提供钱包交易的创建和查询功能")
public class TransactionController {
  private final UserService userService;
  private final WalletService walletService;
  private final TransactionService transactionService;

  public TransactionController(
      UserService userService,
      @Qualifier("walletServiceImpl") WalletService walletServiceImpl,
      @Qualifier("transactionServiceImpl") TransactionService transactionServiceImpl) {
    this.userService = userService;
    this.walletService = walletServiceImpl;
    this.transactionService = transactionServiceImpl;
  }

  @GetMapping("/{id}")
  @Operation(summary = "根据ID获取交易", description = "查询指定交易的详细信息")
  public HttpResult<TransactionVO> getTransactionById(
      @Parameter(description = "交易ID", required = true) @PathVariable("id") Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

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
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (walletId == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

    User owner = walletService.getWalletOwnerById(walletId);
    if (owner == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
    if (!me.equals(owner)) return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    try {
      TransactionsRecord transactionsRecord =
          transactionService.getTransactionsByWalletId(walletId);
      return HttpResult.success(transactionsRecord);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("")
  @Operation(summary = "创建交易", description = "创建充值、提现或转账交易")
  public HttpResult<TransactionVO> createTransaction(
      @Parameter(description = "交易信息", required = true) @RequestBody TransactionVO transactionVO) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

    if (transactionVO == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "TransactionVO CANT BE NULL");
    if (transactionVO.getAmount() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
    if (transactionVO.getType() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Type CANT BE NULL");
    if (!TransactionType.isValidTransactionType(transactionVO.getType()))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Type NOT VALID");

    if (transactionVO.getAmount().compareTo(BigDecimal.ZERO) <= 0)
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Amount NOT VALID");

    // Validate wallet IDs based on transaction type
    if (transactionVO.getType().equals(TransactionType.PAYMENT)) {
      // Payment requires both wallets
      if (transactionVO.getInWalletId() == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
      if (transactionVO.getOutWalletId() == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
    } else if (transactionVO.getType().equals(TransactionType.TOP_UP)) {
      // Top-up only requires inWalletId (money comes from external source)
      if (transactionVO.getInWalletId() == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
      // outWalletId is not required for top-up (external payment source)
    } else if (transactionVO.getType().equals(TransactionType.WITHDRAW)) {
      // Withdraw only requires outWalletId (money goes to external account)
      if (transactionVO.getOutWalletId() == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
      // inWalletId is not required for withdrawal (external payment destination)
    }

    try {
      TransactionVO retTransactionVO =
          transactionService.createTransaction(
              transactionVO.getAmount(), transactionVO.getType(),
              transactionVO.getInWalletId(), transactionVO.getOutWalletId());
      return HttpResult.success(retTransactionVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PatchMapping("/finished")
  @Operation(summary = "完成交易", description = "标记交易为已完成状态")
  public HttpResult<TransactionVO> finishTransaction(
      @Parameter(description = "交易ID", required = true) @RequestParam Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

    try {
      TransactionVO transactionVO = transactionService.finishTransaction(id, me);
      return HttpResult.success(transactionVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}
