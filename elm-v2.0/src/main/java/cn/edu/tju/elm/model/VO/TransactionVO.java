package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.Transaction;

import java.io.Serializable;
import java.math.BigDecimal;

public class TransactionVO implements Serializable {
    private Long id;
    private BigDecimal amount;
    private Integer type;
    private Long inWalletId;
    private Long outWalletId;
    private Boolean finished;

    public TransactionVO() {
    }

    public TransactionVO(Transaction transaction) {
        this.id = transaction.getId();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.inWalletId = transaction.getInWallet() == null ? null : transaction.getInWallet().getId();
        this.outWalletId = transaction.getOutWallet() == null ? null : transaction.getOutWallet().getId();
        this.finished = transaction.isFinished();
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

    public Long getInWalletId() {
        return inWalletId;
    }

    public Long getOutWalletId() {
        return outWalletId;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setInWalletId(Long inWalletId) {
        this.inWalletId = inWalletId;
    }

    public void setOutWalletId(Long outWalletId) {
        this.outWalletId = outWalletId;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "TransactionVO: id=" + id + ", amount=" + amount + ", type=" + type + ", inWalletId=" + inWalletId + ", outWalletId=" + outWalletId + ", finished=" + finished;
    }
}
