package cn.edu.tju.wallet.service;

import cn.edu.tju.wallet.model.BO.Wallet;

import java.util.Optional;

public interface WalletInternalService {
    Optional<Wallet> findByUserId(Long userId);
}
