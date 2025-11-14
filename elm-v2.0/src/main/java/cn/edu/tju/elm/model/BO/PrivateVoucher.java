package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class PrivateVoucher extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    private BigDecimal faceValue;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public static PrivateVoucher createPrivateVoucher(Wallet wallet, PublicVoucherVO publicVoucherVO) {
        PrivateVoucher privateVoucher = new PrivateVoucher();
        privateVoucher.wallet = wallet;
        privateVoucher.faceValue = publicVoucherVO.getValue();
        privateVoucher.expiryDate = LocalDateTime.now().plusDays(publicVoucherVO.getValidDays());
        EntityUtils.setNewEntity(privateVoucher);
        return privateVoucher;
    }

    public boolean redeem() {
        LocalDateTime now = LocalDateTime.now();
        setDeleted(true);
        setUpdateTime(now);
        return now.isBefore(this.expiryDate);
    }

    public Wallet getWallet() {
        return wallet;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
}
