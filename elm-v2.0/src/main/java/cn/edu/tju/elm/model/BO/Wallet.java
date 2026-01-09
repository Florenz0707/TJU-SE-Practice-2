package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Wallet extends BaseEntity {
    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal voucher;

    @OneToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "last_withdrawal_at")
    private LocalDateTime lastWithdrawalAt;

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
        synchronized (this) {
            balance = balance.add(amount);
        }
        return true;
    }

    public boolean decBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        synchronized (this) {
            if (amount.compareTo(balance) > 0) return false;
            balance = balance.subtract(amount);
            return true;
        }
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

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit == null ? BigDecimal.ZERO : creditLimit;
    }

    public LocalDateTime getLastWithdrawalAt() {
        return lastWithdrawalAt;
    }

    public void setLastWithdrawalAt(LocalDateTime lastWithdrawalAt) {
        this.lastWithdrawalAt = lastWithdrawalAt;
    }

    /**
     * 支持透支扣减：允许余额变为负值，但不超过 creditLimit
     */
    public boolean decBalanceWithCredit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        synchronized (this) {
            BigDecimal allowed = balance.add(creditLimit);
            if (allowed.compareTo(amount) < 0) return false;
            balance = balance.subtract(amount);
            return true;
        }
    }
}
