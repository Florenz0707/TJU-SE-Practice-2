package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
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
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public static PrivateVoucher createPrivateVoucher(User owner, PublicVoucher publicVoucher) {
        PrivateVoucher privateVoucher = new PrivateVoucher();
        privateVoucher.owner = owner;
        privateVoucher.value = publicVoucher.getValue();
        privateVoucher.expiryDate = LocalDateTime.now().plusDays(publicVoucher.getValidDays());
        EntityUtils.setNewEntity(privateVoucher);
        return privateVoucher;
    }

    public boolean redeem() {
        LocalDateTime now = LocalDateTime.now();
        setDeleted(true);
        setUpdateTime(now);
        return now.isBefore(this.expiryDate);
    }

    public User getOwner() {
        return owner;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
}
