package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.TransactionBO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionBO, Long> {
    List<TransactionBO> findAllByEnterWalletId(Long walletId);

    List<TransactionBO> findAllByOutWalletId(Long walletId);
}
