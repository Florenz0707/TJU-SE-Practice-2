package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.BO.TransactionBO;
import cn.edu.tju.elm.model.BO.WalletBO;
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
        TransactionBO transactionBO = transactionRepository.findById(id).orElse(null);
        return transactionBO == null ? null : new TransactionVO(transactionBO);
    }

    public TransactionVO createTransaction(BigDecimal amount, Integer type, Long enterWalletId, Long outWalletId, User operator) {
        WalletBO enterWalletBO = null;
        WalletBO outWalletBO = null;
        if (type.equals(TransactionType.TOP_UP)) {
            if (enterWalletId == null) return null;
            enterWalletBO = walletRepository.findById(enterWalletId).orElse(null);
            if (enterWalletBO == null) return null;
            if (!enterWalletBO.addBalance(amount)) return null;
            EntityUtils.updateEntity(enterWalletBO, operator);
            walletRepository.save(enterWalletBO);
        } else if (type.equals(TransactionType.PAYMENT)) {
            if (enterWalletId == null || outWalletId == null) return null;
            enterWalletBO = walletRepository.findById(enterWalletId).orElse(null);
            outWalletBO = walletRepository.findById(outWalletId).orElse(null);
            if (enterWalletBO == null || outWalletBO == null) return null;
            if (!outWalletBO.decBalance(amount)) return null;
            EntityUtils.updateEntity(outWalletBO, operator);
            walletRepository.save(outWalletBO);
            // 暂时冻结，不入账
        } else if (type.equals(TransactionType.WITHDRAW)) {
            if (outWalletId == null) return null;
            outWalletBO = walletRepository.findById(outWalletId).orElse(null);
            if (outWalletBO == null) return null;
            if (!outWalletBO.decBalance(amount)) return null;
            EntityUtils.updateEntity(outWalletBO, operator);
            walletRepository.save(outWalletBO);
        }

        TransactionBO transactionBO = TransactionBO.createNewTransaction(amount, type, enterWalletBO, outWalletBO);
        EntityUtils.setNewEntity(transactionBO, operator);
        transactionRepository.save(transactionBO);
        return new TransactionVO(transactionBO);
    }

    public TransactionVO finishTransaction(Long id, User operator) {
        TransactionBO transactionBO = transactionRepository.findById(id).orElse(null);
        if (transactionBO == null) return null;
        if (transactionBO.isFinished()) return null;

        if (transactionBO.getType().equals(TransactionType.PAYMENT)) {
            WalletBO outWalletBO = transactionBO.getOutWallet();
            outWalletBO.addBalance(transactionBO.getAmount());
            EntityUtils.updateEntity(transactionBO, operator);
            walletRepository.save(outWalletBO);
        }

        transactionBO.finish();
        EntityUtils.updateEntity(transactionBO, operator);
        transactionRepository.save(transactionBO);
        return new TransactionVO(transactionBO);
    }

    public TransactionsRecord getTransactionsByWalletId(Long walletId) {
        List<TransactionVO> enterWalletList = new ArrayList<>();
        for (TransactionBO transactionBO : transactionRepository.findAllByEnterWalletId(walletId)) {
            enterWalletList.add(new TransactionVO(transactionBO));
        }
        List<TransactionVO> outWalletList = new ArrayList<>();
        for (TransactionBO transactionBO : transactionRepository.findAllByOutWalletId(walletId)) {
            outWalletList.add(new TransactionVO(transactionBO));
        }
        return new TransactionsRecord(enterWalletList, outWalletList);
    }
}
