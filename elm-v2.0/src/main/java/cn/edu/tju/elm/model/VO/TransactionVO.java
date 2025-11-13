package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.TransactionBO;

import java.math.BigDecimal;

public class TransactionVO {
    private final Long id;
    private final BigDecimal amount;
    private final Integer type;
    private final Long enterWalletId;
    private final Long outWalletId;

    public TransactionVO(TransactionBO transactionBO) {
        this.id = transactionBO.getId();
        this.amount = transactionBO.getAmount();
        this.type = transactionBO.getType();
        this.enterWalletId = transactionBO.getEnterWallet().getId();
        this.outWalletId = transactionBO.getOutWallet().getId();
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
