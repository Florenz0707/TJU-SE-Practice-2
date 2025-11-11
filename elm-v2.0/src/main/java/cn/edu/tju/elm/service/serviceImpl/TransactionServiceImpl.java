package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public TransactionVO getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElse(null);
        return transaction == null ? null : new TransactionVO(transaction);
    }

    public TransactionVO createTransaction(BigDecimal amount, Integer type, Long enterWalletId, Long outWalletId, User operator) {
        Wallet enterWallet = null;
        Wallet outWallet = null;
        if (type.equals(TransactionType.TOP_UP)) {
            if (enterWalletId == null) return null;
            enterWallet = walletRepository.findById(enterWalletId).orElse(null);
            if (enterWallet == null) return null;
            if (!enterWallet.addBalance(amount)) return null;
            EntityUtils.updateEntity(enterWallet, operator);
            walletRepository.save(enterWallet);
        } else if (type.equals(TransactionType.PAYMENT)) {
            if (enterWalletId == null || outWalletId == null) return null;
            enterWallet = walletRepository.findById(enterWalletId).orElse(null);
            outWallet = walletRepository.findById(outWalletId).orElse(null);
            if (enterWallet == null || outWallet == null) return null;
            if (!outWallet.decBalance(amount)) return null;
            EntityUtils.updateEntity(outWallet, operator);
            walletRepository.save(outWallet);
            // 暂时冻结，不入账
        } else if (type.equals(TransactionType.WITHDRAW)) {
            if (outWalletId == null) return null;
            outWallet = walletRepository.findById(outWalletId).orElse(null);
            if (outWallet == null) return null;
            if (!outWallet.decBalance(amount)) return null;
            EntityUtils.updateEntity(outWallet, operator);
            walletRepository.save(outWallet);
        }

        Transaction transaction = Transaction.createNewTransaction(amount, type, enterWallet, outWallet);
        EntityUtils.setNewEntity(transaction, operator);
        transactionRepository.save(transaction);
        return new TransactionVO(transaction);
    }

    public TransactionVO finishTransaction(Long id, User operator) {
        Transaction transaction = transactionRepository.findById(id).orElse(null);
        if (transaction == null) return null;
        if (transaction.isFinished()) return null;

        if (transaction.getType().equals(TransactionType.PAYMENT)) {
            Wallet outWallet = transaction.getOutWallet();
            outWallet.addBalance(transaction.getAmount());
            EntityUtils.updateEntity(transaction, operator);
            walletRepository.save(outWallet);
        }

        transaction.finish();
        EntityUtils.updateEntity(transaction, operator);
        transactionRepository.save(transaction);
        return new TransactionVO(transaction);
    }

    public TransactionsRecord getTransactionsByWalletId(Long walletId) {
        List<TransactionVO> enterWalletList = new ArrayList<>();
        for (Transaction transaction : transactionRepository.findAllByInWalletId(walletId)) {
            enterWalletList.add(new TransactionVO(transaction));
        }
        List<TransactionVO> outWalletList = new ArrayList<>();
        for (Transaction transaction : transactionRepository.findAllByOutWalletId(walletId)) {
            outWalletList.add(new TransactionVO(transaction));
        }
        return new TransactionsRecord(enterWalletList, outWalletList);
    }
}
