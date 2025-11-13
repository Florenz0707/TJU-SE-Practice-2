package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

@Entity
public class TransactionBO extends BaseEntity {
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer type;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = true)
    private WalletBO enterWallet;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = true)
    private WalletBO outWallet;

    @Column(nullable = false)
    private Boolean isFinished;

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getType() {
        return type;
    }

    public WalletBO getEnterWallet() {
        return enterWallet;
    }

    public WalletBO getOutWallet() {
        return outWallet;
    }

    public Boolean getIsFinished() {
        return isFinished;
    }

    public static TransactionBO createNewTransaction(BigDecimal amount, Integer type, WalletBO enterWallet, WalletBO outWallet) {
        TransactionBO transaction = new TransactionBO();
        transaction.amount = amount;
        transaction.type = type;
        transaction.enterWallet = enterWallet;
        transaction.outWallet = outWallet;
        transaction.isFinished = false;
        return transaction;
    }

    public void setIsFinished(Boolean isFinished) {
        this.isFinished = isFinished;
    }
}
