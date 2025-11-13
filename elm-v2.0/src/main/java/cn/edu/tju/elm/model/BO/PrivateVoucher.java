package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class PrivateVoucher extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private PublicVoucher voucher;

    public static PrivateVoucher createPrivateVoucher(User user, PublicVoucher voucher) {
        PrivateVoucher privateVoucher = new PrivateVoucher();
        privateVoucher.owner = user;
        privateVoucher.voucher = voucher;
        EntityUtils.setNewEntity(privateVoucher);
        return privateVoucher;
    }

    public User getOwner() {
        return owner;
    }

    public PublicVoucher getVoucher() {
        return voucher;
    }

    public boolean redeemVoucher() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = getCreateTime();
        if (start.plusDays(voucher.getValid_days()).isBefore(now)) return false;
        setDeleted(true);
        setUpdateTime(now);
        return true;
    }
}
