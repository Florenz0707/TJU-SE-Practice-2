package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface TransactionService {
    TransactionVO getTransactionById(Long id);

    @Transactional
    TransactionVO createTransaction(BigDecimal amount, Integer type, Long enterWalletId, Long outWalletId, User operator);

    @Transactional
    TransactionVO updateTransactionStatus(Long id, Boolean isFinished, User operator);

    TransactionsRecord getTransactionsByWalletId(Long walletId);
}
