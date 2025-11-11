package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.Wallet;

import java.math.BigDecimal;

public class WalletVO {
    private final Long id;
    private final BigDecimal balance;
    private final BigDecimal voucher;
    private final Long ownerId;

    public WalletVO(Wallet wallet) {
        id = wallet.getId();
        balance = wallet.getBalance();
        voucher = wallet.getVoucher();
        ownerId = wallet.getOwner().getId();
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

    @Override
    public String toString() {
        return "WalletVO: id=" + id + ", balance=" + balance + ", voucher=" + voucher + ", ownerId=" + ownerId;
    }
}
