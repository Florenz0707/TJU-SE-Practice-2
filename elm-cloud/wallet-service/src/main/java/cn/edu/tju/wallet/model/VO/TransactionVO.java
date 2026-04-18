package cn.edu.tju.wallet.model.VO;

import cn.edu.tju.wallet.model.BO.Transaction;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionVO implements Serializable {
  private Long id;
  private BigDecimal amount;
  private Integer type;
  private Long inWalletId;
  private Long outWalletId;
  private Boolean finished;
  private String requestId;
  private String bizId;
  private String reason;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public TransactionVO() {}

  public TransactionVO(Transaction transaction) {
    this.id = transaction.getId();
    this.amount = transaction.getAmount();
    this.type = transaction.getType();
    this.inWalletId = transaction.getInWallet() == null ? null : transaction.getInWallet().getId();
    this.outWalletId =
        transaction.getOutWallet() == null ? null : transaction.getOutWallet().getId();
    this.finished = transaction.isFinished();
    this.requestId = transaction.getRequestId();
    this.bizId = transaction.getBizId();
    this.reason = transaction.getReason();
    this.createTime = transaction.getCreateTime();
    this.updateTime = transaction.getUpdateTime();
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Integer getType() {
    return type;
  }

  public Long getInWalletId() {
    return inWalletId;
  }

  public Long getOutWalletId() {
    return outWalletId;
  }

  public Boolean getFinished() {
    return finished;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getBizId() {
    return bizId;
  }

  public String getReason() {
    return reason;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public void setType(Integer type) {
    this.type = type;
  }

  public void setInWalletId(Long inWalletId) {
    this.inWalletId = inWalletId;
  }

  public void setOutWalletId(Long outWalletId) {
    this.outWalletId = outWalletId;
  }

  public void setFinished(Boolean finished) {
    this.finished = finished;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public void setBizId(String bizId) {
    this.bizId = bizId;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public String toString() {
    return "TransactionVO: id="
        + id
        + ", amount="
        + amount
        + ", type="
        + type
        + ", inWalletId="
        + inWalletId
        + ", outWalletId="
        + outWalletId
        + ", finished="
        + finished
        + ", requestId="
        + requestId
        + ", bizId="
        + bizId
        + ", reason="
        + reason;
  }
}
