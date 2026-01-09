package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.TOPUPPublicVoucherSelectorImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    // 提现最小金额（单位：元），以及提现冷却期（秒）
    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10");
    private static final long WITHDRAWAL_COOLDOWN_SECONDS = 24 * 3600; // 24 小时

    private final PublicVoucherService publicVoucherService;
    private final PrivateVoucherService privateVoucherService;

    public TransactionServiceImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            PublicVoucherService publicVoucherService,
            PrivateVoucherService privateVoucherService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.publicVoucherService = publicVoucherService;
        this.privateVoucherService = privateVoucherService;
    }

    public TransactionVO getTransactionById(Long id) throws TransactionException {
        Transaction transaction = transactionRepository.findById(id).orElse(null);
        if (transaction == null)
            throw new TransactionException(TransactionException.NOT_FOUND);
        return new TransactionVO(transaction);
    }

    @Transactional
    public TransactionVO createTransaction(BigDecimal amount, Integer type, Long inWalletId, Long outWalletId) throws TransactionException {
        Wallet inWallet = null;
        Wallet outWallet = null;
        if (type.equals(TransactionType.TOP_UP)) {
            inWallet = walletRepository.findById(inWalletId).orElse(null);
            if (inWallet == null)
                throw new TransactionException(TransactionException.IN_WALLET_NOT_FOUND);
            if (!inWallet.addBalance(amount))
                throw new TransactionException(TransactionException.UNKNOWN_EXCEPTION);
            EntityUtils.updateEntity(inWallet);
            walletRepository.save(inWallet);
        } else if (type.equals(TransactionType.PAYMENT)) {
            inWallet = walletRepository.findById(inWalletId).orElse(null);
            outWallet = walletRepository.findById(outWalletId).orElse(null);
            if (inWallet == null)
                throw new TransactionException(TransactionException.IN_WALLET_NOT_FOUND);
            if (outWallet == null)
                throw new TransactionException(TransactionException.OUT_WALLET_NOT_FOUND);
            if (!outWallet.decBalance(amount))
                throw new TransactionException(TransactionException.BALANCE_NOT_ENOUGH);
            EntityUtils.updateEntity(outWallet);
            walletRepository.save(outWallet);
            // 暂时冻结，不入账
        } else if (type.equals(TransactionType.WITHDRAW)) {
            outWallet = walletRepository.findById(outWalletId).orElse(null);
            if (outWallet == null)
                throw new TransactionException(TransactionException.OUT_WALLET_NOT_FOUND);
            // 最小提现金额校验
            if (amount.compareTo(MIN_WITHDRAWAL) < 0) {
                throw new TransactionException(TransactionException.WITHDRAWAL_MINIMUM);
            }

            // 冷却期检查
            LocalDateTime last = outWallet.getLastWithdrawalAt();
            if (last != null) {
                long since = Duration.between(last, LocalDateTime.now()).getSeconds();
                if (since < WITHDRAWAL_COOLDOWN_SECONDS) {
                    throw new TransactionException(TransactionException.WITHDRAWAL_COOLDOWN);
                }
            }

            // 支持透支：尝试使用带信用额度的扣减方法
            if (!outWallet.decBalanceWithCredit(amount))
                throw new TransactionException(TransactionException.BALANCE_NOT_ENOUGH);

            // 记录提现时间
            outWallet.setLastWithdrawalAt(LocalDateTime.now());
            EntityUtils.updateEntity(outWallet);
            walletRepository.save(outWallet);
        }

        Transaction transaction = Transaction.createNewTransaction(amount, type, inWallet, outWallet);
        EntityUtils.setNewEntity(transaction);
        if (type.equals(TransactionType.TOP_UP)) {
            PublicVoucherVO publicVoucherVO = publicVoucherService.chooseBestPublicVoucherForTransaction(new TransactionVO(transaction), new TOPUPPublicVoucherSelectorImpl());
            if (publicVoucherVO != null)
                privateVoucherService.createPrivateVoucher(inWalletId, publicVoucherVO);
        }
        transactionRepository.save(transaction);
        return new TransactionVO(transaction);
    }

    @Transactional
    public TransactionVO finishTransaction(Long id, User operator) throws TransactionException {
        Transaction transaction = transactionRepository.findById(id).orElse(null);
        if (transaction == null)
            throw new TransactionException(TransactionException.NOT_FOUND);
        if (transaction.isFinished())
            throw new TransactionException(TransactionException.ALREADY_FINISHED);

        if (transaction.getType().equals(TransactionType.PAYMENT)) {
            // PAYMENT 完成时，将金额入账给收款方（inWallet）
            Wallet inWallet = transaction.getInWallet();
            if (inWallet == null)
                throw new TransactionException(TransactionException.IN_WALLET_NOT_FOUND);
            inWallet.addBalance(transaction.getAmount());
            EntityUtils.updateEntity(inWallet);
            walletRepository.save(inWallet);
        }

        transaction.finish();
        EntityUtils.updateEntity(transaction);
        transactionRepository.save(transaction);
        return new TransactionVO(transaction);
    }

    public TransactionsRecord getTransactionsByWalletId(Long walletId) {
        List<TransactionVO> inWalletList = new ArrayList<>();
        List<TransactionVO> outWalletList = new ArrayList<>();

        for (Transaction transaction : transactionRepository.findAllByInWalletId(walletId)) {
            inWalletList.add(new TransactionVO(transaction));
        }
        for (Transaction transaction : transactionRepository.findAllByOutWalletId(walletId)) {
            outWalletList.add(new TransactionVO(transaction));
        }
        return new TransactionsRecord(inWalletList, outWalletList);
    }
}
