package cn.edu.tju.wallet.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.wallet.model.VO.InternalVoucherSnapshotVO;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import cn.edu.tju.wallet.model.VO.WalletVO;
import cn.edu.tju.wallet.service.AccountInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/api/inner/account")

public class AccountInnerController {
  private final AccountInternalService accountInternalService;

  public AccountInnerController(AccountInternalService accountInternalService) {
    this.accountInternalService = accountInternalService;
  }

  @PostMapping("/wallet/debit")
  
  public HttpResult<TransactionVO> walletDebit(
      @RequestBody WalletDebitRequest request) {
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
  
  public HttpResult<TransactionVO> walletRefund(
      @RequestBody
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

  @PostMapping("/wallet/credit")
  
  public HttpResult<TransactionVO> walletCredit(
      @RequestBody WalletCreditRequest request) {
    try {
      TransactionVO transaction =
          accountInternalService.walletCredit(
              request.getRequestId(),
              request.getUserId(),
              request.getAmount(),
              request.getBizId(),
              request.getReason());
      if (transaction == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "wallet credit failed");
      }
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class WalletCreditRequest {
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

  @PostMapping("/voucher/redeem")
  
  public HttpResult<Boolean> redeemVoucher(
      @RequestBody
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
  
  public HttpResult<Boolean> rollbackVoucher(
      @RequestBody
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
  
  public HttpResult<TransactionVO> getTransactionByBizId(
      @PathVariable("bizId") String bizId) {
    try {
      TransactionVO transaction = accountInternalService.getTransactionByBizId(bizId);
      return HttpResult.success(transaction);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/wallet/by-user/{userId}")
  
  public HttpResult<WalletVO> getWalletByUserId(
      @PathVariable("userId") Long userId,
      @RequestParam(name = "createIfAbsent", defaultValue = "false")
          boolean createIfAbsent) {
    try {
      WalletVO wallet = accountInternalService.getWalletByUserId(userId, createIfAbsent);
      return HttpResult.success(wallet);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/voucher/{voucherId}")
  
  public HttpResult<InternalVoucherSnapshotVO> getVoucherSnapshot(
      @PathVariable("voucherId")
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
