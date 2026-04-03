package cn.edu.tju.wallet.model.VO;

import cn.edu.tju.wallet.model.BO.PrivateVoucher;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InternalVoucherSnapshotVO {
  private final Long id;
  private final Long ownerId;
  private final Boolean deleted;
  private final LocalDateTime expiryDate;
  private final BigDecimal faceValue;
  private final BigDecimal threshold;

  public InternalVoucherSnapshotVO(PrivateVoucher voucher) {
    this.id = voucher.getId();
    this.ownerId = voucher.getWallet() != null ? voucher.getWallet().getOwnerId() : null;
    this.deleted = voucher.getDeleted();
    this.expiryDate = voucher.getExpiryDate();
    this.faceValue = voucher.getFaceValue();
    this.threshold =
        voucher.getPublicVoucher() != null ? voucher.getPublicVoucher().getThreshold() : null;
  }

  public Long getId() {
    return id;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public LocalDateTime getExpiryDate() {
    return expiryDate;
  }

  public BigDecimal getFaceValue() {
    return faceValue;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }
}
