package cn.edu.tju.wallet.model.VO;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.wallet.model.BO.PublicVoucher;
import java.io.Serializable;
import java.math.BigDecimal;

public class PublicVoucherVO implements Serializable {
  private Long id;
  private BigDecimal threshold;
  private BigDecimal value;
  private Boolean claimable;
  private Integer validDays;

  // total quantity and per-user limit for admin management
  private Integer totalQuantity;
  private Integer perUserLimit;

  public PublicVoucherVO() {}

  public PublicVoucherVO(PublicVoucher publicVoucher) {
    id = publicVoucher.getId();
    threshold = publicVoucher.getThreshold();
    value = publicVoucher.getFaceValue();
    claimable = publicVoucher.getClaimable();
    validDays = publicVoucher.getValidDays();
    totalQuantity = publicVoucher.getTotalQuantity();
    perUserLimit = publicVoucher.getPerUserLimit();
  }

  public static HttpResult<String> isValidPublicVoucherVO(PublicVoucherVO publicVoucherVO) {
    if (publicVoucherVO == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "PublicVoucher CANT BE NULL");
    if (publicVoucherVO.getThreshold() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Threshold CANT BE NULL");
    if (publicVoucherVO.getValue() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Value CANT BE NULL");
    if (publicVoucherVO.getClaimable() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Claimable CANT BE NULL");
    if (publicVoucherVO.getValidDays() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ValidDays CANT BE NULL");
    if (publicVoucherVO.getThreshold().compareTo(BigDecimal.ZERO) < 0)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Threshold CANT BE NEGATIVE");
    if (publicVoucherVO.getValue().compareTo(BigDecimal.ZERO) <= 0)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Value CANT BE NEGATIVE");
    if (publicVoucherVO.getValidDays().compareTo(0) <= 0)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ValidDays CANT BE NEGATIVE");
    return null;
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public BigDecimal getValue() {
    return value;
  }

  public Boolean getClaimable() {
    return claimable;
  }

  public Integer getValidDays() {
    return validDays;
  }

  public Integer getTotalQuantity() {
    return totalQuantity;
  }

  public Integer getPerUserLimit() {
    return perUserLimit;
  }

  @Override
  public String toString() {
    return "VoucherVO: "
        + "id="
        + id
        + ", threshold="
        + threshold
        + ", value="
        + value
        + ", claimable="
        + claimable
        + ", valid_days="
        + validDays
        + ", total_quantity="
        + totalQuantity
        + ", per_user_limit="
        + perUserLimit;
  }
}
