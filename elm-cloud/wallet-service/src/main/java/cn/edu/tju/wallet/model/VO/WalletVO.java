package cn.edu.tju.wallet.model.VO;

import cn.edu.tju.wallet.model.BO.Wallet;
import java.io.Serializable;
import java.math.BigDecimal;

public class WalletVO implements Serializable {
  private final Long id;
  private final BigDecimal balance;
  private final BigDecimal voucher;
  private final Long ownerId;

  public WalletVO(Wallet wallet) {
    id = wallet.getId();
    balance = wallet.getBalance();
    voucher = wallet.getVoucher();
    ownerId = wallet.getOwnerId();
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public BigDecimal getVoucher() {
    return voucher;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  @Override
  public String toString() {
    return "WalletVO: id="
        + id
        + ", balance="
        + balance
        + ", voucher="
        + voucher
        + ", ownerId="
        + ownerId;
  }
}
