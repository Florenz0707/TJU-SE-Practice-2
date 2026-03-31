package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.utils.InternalAccountClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements TransactionService {
  private final InternalAccountClient internalAccountClient;

  public TransactionServiceImpl(
      InternalAccountClient internalAccountClient) {
    this.internalAccountClient = internalAccountClient;
  }

  @Transactional(readOnly = true)
  public TransactionVO getTransactionById(Long id) throws TransactionException {
    try {
      TransactionVO transaction = internalAccountClient.getTransactionById(id);
      if (transaction == null) throw new TransactionException(TransactionException.NOT_FOUND);
      return transaction;
    } catch (IllegalStateException e) {
      throw mapTransactionException(e.getMessage());
    }
  }

  @Transactional
  public TransactionVO createTransaction(
      BigDecimal amount, Integer type, Long inWalletId, Long outWalletId)
      throws TransactionException {
    try {
      TransactionVO transaction =
          internalAccountClient.createTransaction(amount, type, inWalletId, outWalletId);
      if (transaction == null) throw new TransactionException(TransactionException.UNKNOWN_EXCEPTION);
      return transaction;
    } catch (IllegalStateException e) {
      throw mapTransactionException(e.getMessage());
    }
  }

  @Transactional
  public TransactionVO finishTransaction(Long id, Long operatorId, boolean isAdmin)
      throws TransactionException {
    try {
      TransactionVO transaction = internalAccountClient.finishTransaction(id, operatorId, isAdmin);
      if (transaction == null) throw new TransactionException(TransactionException.NOT_FOUND);
      return transaction;
    } catch (IllegalStateException e) {
      throw mapTransactionException(e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public TransactionsRecord getTransactionsByWalletId(Long walletId) {
    try {
      TransactionsRecord transactions = internalAccountClient.getTransactionsByWalletId(walletId);
      if (transactions == null) throw new TransactionException(TransactionException.NOT_FOUND);
      return transactions;
    } catch (IllegalStateException e) {
      throw mapTransactionException(e.getMessage());
    }
  }

  private TransactionException mapTransactionException(String message) {
    if (message == null || message.isBlank()) {
      return new TransactionException(TransactionException.UNKNOWN_EXCEPTION);
    }
    return new TransactionException(message);
  }
}
