package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;

@Entity
public class Wallet extends BaseEntity {
    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal voucher;

    @OneToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getVoucher() {
        return voucher;
    }

    public User getOwner() {
        return owner;
    }

    public static Wallet getNewWallet(User owner) {
        Wallet wallet = new Wallet();
        wallet.owner = owner;
        wallet.balance = BigDecimal.ZERO;
        wallet.voucher = BigDecimal.ZERO;
        EntityUtils.setNewEntity(wallet);
        return wallet;
    }

    public boolean addBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        balance = balance.add(amount);
        return true;
    }

    public boolean decBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (amount.compareTo(balance) > 0) return false;
        balance = balance.subtract(amount);
        return true;
    }

    public boolean addVoucher(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        voucher = voucher.add(amount);
        return true;
    }

    public boolean decVoucher(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (amount.compareTo(voucher) > 0) return false;
        voucher = voucher.subtract(amount);
        return true;
    }
}
