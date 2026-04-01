package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findAllByInWalletId(Long walletId);

  List<Transaction> findAllByOutWalletId(Long walletId);
}
