package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Transaction extends BaseEntity {
  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private Integer type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "in_wallet_id")
  private Wallet inWallet;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "out_wallet_id")
  private Wallet outWallet;

  @Column(nullable = false)
  private Boolean isFinished;

  @Column(name = "request_id", length = 128, unique = true)
  private String requestId;

  @Column(name = "biz_id", length = 128)
  private String bizId;

  @Column(name = "reason", length = 255)
  private String reason;

  public BigDecimal getAmount() {
    return amount;
  }

  public Integer getType() {
    return type;
  }

  public Wallet getInWallet() {
    return inWallet;
  }

  public Wallet getOutWallet() {
    return outWallet;
  }

  public Boolean isFinished() {
    return isFinished;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
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

  public static Transaction createNewTransaction(
      BigDecimal amount, Integer type, Wallet enterWallet, Wallet outWallet) {
    Transaction transaction = new Transaction();
    transaction.amount = amount;
    transaction.type = type;
    transaction.inWallet = enterWallet;
    transaction.outWallet = outWallet;
    transaction.isFinished = false;
    return transaction;
  }

  public void finish() {
    isFinished = true;
  }
}
