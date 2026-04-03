package cn.edu.tju.wallet.service;

import cn.edu.tju.wallet.constant.TransactionType;
import cn.edu.tju.wallet.model.BO.PrivateVoucher;
import cn.edu.tju.wallet.model.BO.Transaction;
import cn.edu.tju.wallet.model.BO.Wallet;
import cn.edu.tju.wallet.model.VO.InternalVoucherSnapshotVO;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import cn.edu.tju.wallet.model.VO.WalletVO;
import cn.edu.tju.wallet.repository.PrivateVoucherRepository;
import cn.edu.tju.wallet.repository.TransactionRepository;
import cn.edu.tju.wallet.repository.WalletRepository;
import cn.edu.tju.wallet.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.wallet.utils.EntityUtils;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountInternalService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final PrivateVoucherRepository privateVoucherRepository;
  private final PrivateVoucherService privateVoucherService;

  public AccountInternalService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      PrivateVoucherRepository privateVoucherRepository,
      PrivateVoucherService privateVoucherService) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.privateVoucherRepository = privateVoucherRepository;
    this.privateVoucherService = privateVoucherService;
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
