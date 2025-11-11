package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.Transaction;

import java.math.BigDecimal;

public class TransactionVO {
    private final Long id;
    private final BigDecimal amount;
    private final Integer type;
    private final Long enterWalletId;
    private final Long outWalletId;

    public TransactionVO(Transaction transaction) {
        this.id = transaction.getId();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.enterWalletId = transaction.getInWallet().getId();
        this.outWalletId = transaction.getOutWallet().getId();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getType() {
        return type;
    }

    public Long getEnterWalletId() {
        return enterWalletId;
    }

    public Long getOutWalletId() {
        return outWalletId;
    }

    @Override
    public String toString() {
        return "TransactionVO: id=" + id + ", amount=" + amount + ", type=" + type;
    }
}
