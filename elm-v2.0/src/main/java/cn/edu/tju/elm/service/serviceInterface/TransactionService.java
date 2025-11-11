package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface TransactionService {
    @Transactional
    TransactionVO createTransaction(BigDecimal amount, Integer type, Long enterWalletId, Long outWalletId, User operator);

    @Transactional
    TransactionVO finishTransaction(Long id, User operator);

    TransactionsRecord getTransactionsByWalletId(Long walletId);

    TransactionVO getTransactionById(Long id);
}
