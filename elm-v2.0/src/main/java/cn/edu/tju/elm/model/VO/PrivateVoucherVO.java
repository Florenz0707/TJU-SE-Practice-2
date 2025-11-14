package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PrivateVoucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrivateVoucherVO {
    private Long id;
    private Long walletId;
    private BigDecimal value;
    private LocalDateTime expiryDate;

    public PrivateVoucherVO() {
    }

    public PrivateVoucherVO(PrivateVoucher privateVoucher) {
        id = privateVoucher.getId();
        walletId = privateVoucher.getWallet().getId();
        value = privateVoucher.getFaceValue();
        expiryDate = privateVoucher.getExpiryDate();
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    @Override
    public String toString() {
        return "PrivateVoucherVO{" +
                "id=" + id +
                ", walletId=" + walletId +
                ", value=" + value +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
