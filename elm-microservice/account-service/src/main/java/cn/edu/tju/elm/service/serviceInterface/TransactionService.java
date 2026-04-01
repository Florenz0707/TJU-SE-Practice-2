package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

public interface TransactionService {
  @Transactional
  TransactionVO createTransaction(
      BigDecimal amount, Integer type, Long inWalletId, Long outWalletId)
      throws TransactionException;

  @Transactional
  TransactionVO finishTransaction(Long id, Long operatorId, boolean isAdmin)
      throws TransactionException;

  TransactionsRecord getTransactionsByWalletId(Long walletId) throws TransactionException;

  TransactionVO getTransactionById(Long id) throws TransactionException;
}
