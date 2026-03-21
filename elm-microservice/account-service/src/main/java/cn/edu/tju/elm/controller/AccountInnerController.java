package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.AccountInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
