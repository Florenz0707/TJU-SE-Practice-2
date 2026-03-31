package cn.edu.tju.elm.service;

import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.InternalVoucherSnapshotVO;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.utils.EntityUtils;
import java.math.BigDecimal;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountInternalService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final PrivateVoucherRepository privateVoucherRepository;
  private final PrivateVoucherService privateVoucherService;
  private final WalletService walletService;
  private final TransactionService transactionService;

  public AccountInternalService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      PrivateVoucherRepository privateVoucherRepository,
      PrivateVoucherService privateVoucherService,
      WalletService walletService,
      TransactionService transactionService) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.privateVoucherRepository = privateVoucherRepository;
    this.privateVoucherService = privateVoucherService;
    this.walletService = walletService;
    this.transactionService = transactionService;
  }

  @Transactional
  public WalletVO createWallet(Long userId) {
    if (userId == null) {
      return null;
    }
    return walletService.createWallet(userId);
  }

  @Transactional(readOnly = true)
  public WalletVO getWalletById(Long walletId, Long operatorId, boolean isAdmin) {
    if (walletId == null || operatorId == null) {
      return null;
    }
    return walletService.getWalletById(walletId, operatorId, isAdmin);
  }

  @Transactional(readOnly = true)
  public Long getWalletOwnerIdById(Long walletId) {
    if (walletId == null) {
      return null;
    }
    return walletService.getWalletOwnerIdById(walletId);
  }

  @Transactional
  public void addVoucher(Long walletId, BigDecimal amount) {
    walletService.addVoucher(walletId, amount);
  }

  @Transactional
  public TransactionVO topupWallet(Long userId, BigDecimal amount) {
    WalletVO wallet = walletService.getWalletByOwnerId(userId);
    if (wallet == null) {
      return null;
    }
    return transactionService.createTransaction(amount, TransactionType.TOP_UP, wallet.getId(), null);
  }

  @Transactional
  public TransactionVO withdrawFromWallet(Long userId, BigDecimal amount) {
    WalletVO wallet = walletService.getWalletByOwnerId(userId);
    if (wallet == null) {
      return null;
    }
    return transactionService.createTransaction(amount, TransactionType.WITHDRAW, null, wallet.getId());
  }

  @Transactional(readOnly = true)
  public TransactionVO getTransactionById(Long transactionId) {
    if (transactionId == null) {
      return null;
    }
    return transactionService.getTransactionById(transactionId);
  }

  @Transactional(readOnly = true)
  public TransactionsRecord getTransactionsByWalletId(Long walletId) {
    if (walletId == null) {
      return null;
    }
    return transactionService.getTransactionsByWalletId(walletId);
  }

  @Transactional
  public TransactionVO createTransaction(
      BigDecimal amount, Integer type, Long inWalletId, Long outWalletId) {
    return transactionService.createTransaction(amount, type, inWalletId, outWalletId);
  }

  @Transactional
  public TransactionVO finishTransaction(Long id, Long operatorId, boolean isAdmin) {
    if (id == null || operatorId == null) {
      return null;
    }
    return transactionService.finishTransaction(id, operatorId, isAdmin);
  }

  @Transactional
  public TransactionVO walletDebit(
      String requestId, Long userId, BigDecimal amount, String bizId, String reason) {
    if (requestId == null || requestId.isEmpty() || userId == null || amount == null) {
      return null;
    }
    Optional<Transaction> existing = transactionRepository.findByRequestId(requestId);
    if (existing.isPresent()) {
      return new TransactionVO(existing.get());
    }

    Wallet wallet = walletRepository.findByOwnerId(userId).orElse(null);
    if (wallet == null) {
      return null;
    }
    if (!wallet.decBalanceWithCredit(amount)) {
      return null;
    }
    EntityUtils.updateEntity(wallet);
    walletRepository.save(wallet);

    Transaction transaction =
        Transaction.createNewTransaction(amount, TransactionType.PAYMENT, null, wallet);
    transaction.setRequestId(requestId);
    transaction.setBizId(bizId);
    transaction.setReason(reason != null ? reason : "internal wallet debit");
    transaction.finish();
    EntityUtils.setNewEntity(transaction);
    transactionRepository.save(transaction);
    return new TransactionVO(transaction);
  }

  @Transactional
  public TransactionVO walletRefund(
      String requestId, Long userId, BigDecimal amount, String bizId, String reason) {
    if (requestId == null || requestId.isEmpty() || userId == null || amount == null) {
      return null;
    }
    Optional<Transaction> existing = transactionRepository.findByRequestId(requestId);
    if (existing.isPresent()) {
      return new TransactionVO(existing.get());
    }

    Wallet wallet = walletRepository.findByOwnerId(userId).orElse(null);
    if (wallet == null) {
      return null;
    }
    if (!wallet.addBalance(amount)) {
      return null;
    }
    EntityUtils.updateEntity(wallet);
    walletRepository.save(wallet);

    Transaction transaction =
        Transaction.createNewTransaction(amount, TransactionType.TOP_UP, wallet, null);
    transaction.setRequestId(requestId);
    transaction.setBizId(bizId);
    transaction.setReason(reason != null ? reason : "internal wallet refund");
    transaction.finish();
    EntityUtils.setNewEntity(transaction);
    transactionRepository.save(transaction);
    return new TransactionVO(transaction);
  }

  @Transactional
  public boolean redeemVoucher(String requestId, Long userId, Long voucherId, String orderId) {
    if (userId == null || voucherId == null) {
      return false;
    }
    PrivateVoucher voucher = privateVoucherRepository.findById(voucherId).orElse(null);
    if (voucher == null
        || voucher.getWallet() == null
        || !userId.equals(voucher.getWallet().getOwnerId())) {
      return false;
    }
    if (voucher.getDeleted() != null && voucher.getDeleted()) {
      return true;
    }
    return privateVoucherService.redeemPrivateVoucher(voucherId);
  }

  @Transactional
  public boolean rollbackVoucher(
      String requestId, Long userId, Long voucherId, String orderId, String reason) {
    if (userId == null || voucherId == null) {
      return false;
    }
    PrivateVoucher voucher = privateVoucherRepository.findById(voucherId).orElse(null);
    if (voucher == null
        || voucher.getWallet() == null
        || !userId.equals(voucher.getWallet().getOwnerId())) {
      return false;
    }
    if (voucher.getDeleted() == null || !voucher.getDeleted()) {
      return true;
    }
    privateVoucherService.restoreVoucher(voucherId);
    return true;
  }

  @Transactional(readOnly = true)
  public TransactionVO getTransactionByBizId(String bizId) {
    if (bizId == null || bizId.isEmpty()) {
      return null;
    }
    return transactionRepository
        .findTopByBizIdOrderByCreateTimeDesc(bizId)
        .map(TransactionVO::new)
        .orElse(null);
  }

  @Transactional
  public WalletVO getWalletByUserId(Long userId, boolean createIfAbsent) {
    if (userId == null) {
      return null;
    }
    Wallet wallet = walletRepository.findByOwnerId(userId).orElse(null);
    if (wallet == null && createIfAbsent) {
      wallet = walletRepository.save(Wallet.getNewWallet(userId));
    }
    return wallet == null ? null : new WalletVO(wallet);
  }

  @Transactional(readOnly = true)
  public InternalVoucherSnapshotVO getVoucherSnapshotById(Long voucherId) {
    if (voucherId == null) {
      return null;
    }
    return privateVoucherRepository
        .findById(voucherId)
        .map(InternalVoucherSnapshotVO::new)
        .orElse(null);
  }
}
