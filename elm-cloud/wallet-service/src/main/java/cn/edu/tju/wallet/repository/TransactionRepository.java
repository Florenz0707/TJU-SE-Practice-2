package cn.edu.tju.wallet.repository;

import cn.edu.tju.wallet.model.BO.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findAllByInWalletId(Long walletId);

  List<Transaction> findAllByOutWalletId(Long walletId);

  Optional<Transaction> findByRequestId(String requestId);

  Optional<Transaction> findTopByBizIdOrderByCreateTimeDesc(String bizId);
}
