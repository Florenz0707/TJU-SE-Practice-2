package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface TransactionService {
    @Transactional
    TransactionVO createTransaction(BigDecimal amount, Integer type, Long inWalletId, Long outWalletId) throws TransactionException;

    @Transactional
    TransactionVO finishTransaction(Long id, User operator) throws TransactionException;

    TransactionsRecord getTransactionsByWalletId(Long walletId) throws TransactionException;

    TransactionVO getTransactionById(Long id) throws TransactionException;
}
