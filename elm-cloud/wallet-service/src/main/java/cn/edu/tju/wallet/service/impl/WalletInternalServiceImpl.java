package cn.edu.tju.wallet.service.impl;

import cn.edu.tju.wallet.model.Wallet;
import cn.edu.tju.wallet.repository.WalletRepository;
import cn.edu.tju.wallet.service.WalletInternalService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WalletInternalServiceImpl implements WalletInternalService {

    private final WalletRepository repository;

    public WalletInternalServiceImpl(WalletRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Wallet> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
}
