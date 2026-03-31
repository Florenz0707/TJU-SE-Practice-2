package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.InternalVoucherSnapshotVO;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.AccountInternalService;
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
@RequestMapping("/api/inner/account")
@Tag(name = "账户内部接口", description = "订单服务等内部调用的钱包和优惠券接口")
public class AccountInnerController {
  private final AccountInternalService accountInternalService;

  public AccountInnerController(AccountInternalService accountInternalService) {
    this.accountInternalService = accountInternalService;
  }

  @PostMapping("/wallet/debit")
  @Operation(summary = "钱包扣款", description = "支持幂等的内部钱包扣款接口")
  public HttpResult<TransactionVO> walletDebit(
      @Parameter(description = "钱包扣款请求", required = true) @RequestBody WalletDebitRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.walletDebit(
              request.getRequestId(),
              request.getUserId(),
              request.getAmount(),
              request.getBizId(),
              request.getReason());
      if (transaction == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "wallet debit failed");
      }
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/wallet/refund")
  @Operation(summary = "钱包退款", description = "支持幂等的内部钱包退款接口")
  public HttpResult<TransactionVO> walletRefund(
      @Parameter(description = "钱包退款请求", required = true) @RequestBody
          WalletRefundRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.walletRefund(
              request.getRequestId(),
              request.getUserId(),
              request.getAmount(),
              request.getBizId(),
              request.getReason());
      if (transaction == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "wallet refund failed");
      }
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/voucher/redeem")
  @Operation(summary = "优惠券核销", description = "支持幂等的内部优惠券核销接口")
  public HttpResult<Boolean> redeemVoucher(
      @Parameter(description = "优惠券核销请求", required = true) @RequestBody
          VoucherRedeemRequest request) {
    try {
      boolean success =
          accountInternalService.redeemVoucher(
              request.getRequestId(),
              request.getUserId(),
              request.getVoucherId(),
              request.getOrderId());
      return HttpResult.success(success);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/voucher/rollback")
  @Operation(summary = "优惠券回滚", description = "支持幂等的内部优惠券回滚接口")
  public HttpResult<Boolean> rollbackVoucher(
      @Parameter(description = "优惠券回滚请求", required = true) @RequestBody
          VoucherRollbackRequest request) {
    try {
      boolean success =
          accountInternalService.rollbackVoucher(
              request.getRequestId(),
              request.getUserId(),
              request.getVoucherId(),
              request.getOrderId(),
              request.getReason());
      return HttpResult.success(success);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/transaction/by-biz/{bizId}")
  @Operation(summary = "按业务单号查询交易", description = "查询指定业务单号最近一条交易记录")
  public HttpResult<TransactionVO> getTransactionByBizId(
      @Parameter(description = "业务单号", required = true) @PathVariable("bizId") String bizId) {
    try {
      TransactionVO transaction = accountInternalService.getTransactionByBizId(bizId);
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/wallet/by-user/{userId}")
  @Operation(summary = "按用户查询钱包", description = "支持按需自动创建钱包的内部查询接口")
  public HttpResult<WalletVO> getWalletByUserId(
      @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
      @Parameter(description = "是否不存在时自动创建钱包")
          @RequestParam(name = "createIfAbsent", defaultValue = "false")
          boolean createIfAbsent) {
    try {
      WalletVO wallet = accountInternalService.getWalletByUserId(userId, createIfAbsent);
      return HttpResult.success(wallet);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/wallet")
  @Operation(summary = "内部创建钱包", description = "按用户ID创建钱包")
  public HttpResult<WalletVO> createWallet(
      @Parameter(description = "创建钱包请求", required = true) @RequestBody WalletUserRequest request) {
    try {
      WalletVO wallet = accountInternalService.createWallet(request.getUserId());
      return HttpResult.success(wallet);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/wallet/{walletId}")
  @Operation(summary = "内部查询钱包", description = "按钱包ID查询钱包")
  public HttpResult<WalletVO> getWalletById(
      @Parameter(description = "钱包ID", required = true) @PathVariable("walletId") Long walletId,
      @Parameter(description = "操作人用户ID", required = true) @RequestParam("operatorId") Long operatorId,
      @Parameter(description = "是否管理员") @RequestParam(name = "isAdmin", defaultValue = "false")
          boolean isAdmin) {
    try {
      WalletVO wallet = accountInternalService.getWalletById(walletId, operatorId, isAdmin);
      return HttpResult.success(wallet);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/wallet/owner/{walletId}")
  @Operation(summary = "内部查询钱包所有者", description = "按钱包ID查询所有者")
  public HttpResult<Long> getWalletOwnerById(
      @Parameter(description = "钱包ID", required = true) @PathVariable("walletId") Long walletId) {
    try {
      Long ownerId = accountInternalService.getWalletOwnerIdById(walletId);
      return HttpResult.success(ownerId);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/wallet/voucher")
  @Operation(summary = "内部发券到钱包", description = "向指定钱包添加券余额")
  public HttpResult<String> addVoucher(
      @Parameter(description = "发券请求", required = true) @RequestBody WalletVoucherRequest request) {
    try {
      accountInternalService.addVoucher(request.getWalletId(), request.getAmount());
      return HttpResult.success("Add voucher successfully");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/wallet/topup")
  @Operation(summary = "内部钱包充值", description = "按用户ID执行钱包充值")
  public HttpResult<TransactionVO> topupWallet(
      @Parameter(description = "钱包金额操作请求", required = true) @RequestBody WalletAmountRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.topupWallet(request.getUserId(), request.getAmount());
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/wallet/withdraw")
  @Operation(summary = "内部钱包提现", description = "按用户ID执行钱包提现")
  public HttpResult<TransactionVO> withdrawFromWallet(
      @Parameter(description = "钱包金额操作请求", required = true) @RequestBody WalletAmountRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.withdrawFromWallet(request.getUserId(), request.getAmount());
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/transaction/{id}")
  @Operation(summary = "内部查询交易", description = "按交易ID查询交易")
  public HttpResult<TransactionVO> getTransactionById(
      @Parameter(description = "交易ID", required = true) @PathVariable("id") Long id) {
    try {
      TransactionVO transaction = accountInternalService.getTransactionById(id);
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/transaction/list/{walletId}")
  @Operation(summary = "内部查询钱包交易列表", description = "按钱包ID查询交易列表")
  public HttpResult<TransactionsRecord> getTransactionsByWalletId(
      @Parameter(description = "钱包ID", required = true) @PathVariable("walletId") Long walletId) {
    try {
      TransactionsRecord transactions = accountInternalService.getTransactionsByWalletId(walletId);
      return HttpResult.success(transactions);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/transaction")
  @Operation(summary = "内部创建交易", description = "内部创建充值、提现或支付交易")
  public HttpResult<TransactionVO> createTransaction(
      @Parameter(description = "内部交易请求", required = true) @RequestBody
          InternalTransactionRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.createTransaction(
              request.getAmount(), request.getType(), request.getInWalletId(), request.getOutWalletId());
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PatchMapping("/transaction/finished")
  @Operation(summary = "内部完成交易", description = "内部标记交易完成")
  public HttpResult<TransactionVO> finishTransaction(
      @Parameter(description = "完成交易请求", required = true) @RequestBody
          FinishTransactionRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.finishTransaction(
              request.getId(), request.getOperatorId(), request.isAdmin());
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/voucher/{voucherId}")
  @Operation(summary = "查询优惠券快照", description = "用于订单服务下单前校验优惠券可用性")
  public HttpResult<InternalVoucherSnapshotVO> getVoucherSnapshot(
      @Parameter(description = "私有券ID", required = true) @PathVariable("voucherId")
          Long voucherId) {
    try {
      InternalVoucherSnapshotVO snapshot = accountInternalService.getVoucherSnapshotById(voucherId);
      return HttpResult.success(snapshot);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class WalletDebitRequest {
    private String requestId;
    private Long userId;
    private BigDecimal amount;
    private String bizId;
    private String reason;

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    public String getBizId() {
      return bizId;
    }

    public void setBizId(String bizId) {
      this.bizId = bizId;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  public static class WalletRefundRequest extends WalletDebitRequest {}

  public static class WalletUserRequest {
    private Long userId;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }
  }

  public static class WalletAmountRequest extends WalletUserRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  public static class WalletVoucherRequest {
    private Long walletId;
    private BigDecimal amount;

    public Long getWalletId() {
      return walletId;
    }

    public void setWalletId(Long walletId) {
      this.walletId = walletId;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  public static class InternalTransactionRequest {
    private BigDecimal amount;
    private Integer type;
    private Long inWalletId;
    private Long outWalletId;

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    public Integer getType() {
      return type;
    }

    public void setType(Integer type) {
      this.type = type;
    }

    public Long getInWalletId() {
      return inWalletId;
    }

    public void setInWalletId(Long inWalletId) {
      this.inWalletId = inWalletId;
    }

    public Long getOutWalletId() {
      return outWalletId;
    }

    public void setOutWalletId(Long outWalletId) {
      this.outWalletId = outWalletId;
    }
  }

  public static class FinishTransactionRequest {
    private Long id;
    private Long operatorId;
    private boolean isAdmin;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getOperatorId() {
      return operatorId;
    }

    public void setOperatorId(Long operatorId) {
      this.operatorId = operatorId;
    }

    public boolean isAdmin() {
      return isAdmin;
    }

    public void setAdmin(boolean admin) {
      isAdmin = admin;
    }
  }

  public static class VoucherRedeemRequest {
    private String requestId;
    private Long userId;
    private Long voucherId;
    private String orderId;

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public Long getVoucherId() {
      return voucherId;
    }

    public void setVoucherId(Long voucherId) {
      this.voucherId = voucherId;
    }

    public String getOrderId() {
      return orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }
  }

  public static class VoucherRollbackRequest extends VoucherRedeemRequest {
    private String reason;

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}
