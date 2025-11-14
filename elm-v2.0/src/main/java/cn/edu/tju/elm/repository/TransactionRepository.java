package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByInWalletId(Long walletId);

    List<Transaction> findAllByOutWalletId(Long walletId);
}
