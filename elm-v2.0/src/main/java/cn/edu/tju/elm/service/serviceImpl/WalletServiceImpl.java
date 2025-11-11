package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;

    private final UserService userService;

    public WalletServiceImpl(WalletRepository walletRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
    }

    public WalletVO createWallet(User owner) {
        if (getWalletByOwner(owner) != null) return null;
        Wallet wallet = Wallet.getNewWallet(owner);
        walletRepository.save(wallet);
        return new WalletVO(wallet);
    }

    public WalletVO addVoucher(BigDecimal amount, User owner) {
        Wallet wallet = walletRepository.findByOwner(owner).orElse(null);
        if (wallet == null) return null;
        if (!wallet.addVoucher(amount)) return null;
        EntityUtils.updateEntity(wallet, owner);
        walletRepository.save(wallet);
        return new WalletVO(wallet);
    }

    public User getWalletOwnerById(Long id) {
        Wallet wallet = walletRepository.findById(id).orElse(null);
        if (wallet == null) return null;
        return wallet.getOwner();
    }

    public WalletVO getWalletByOwner(User owner) {
        Wallet wallet = walletRepository.findByOwner(owner).orElse(null);
        if (wallet == null) return null;
        return new WalletVO(wallet);
    }
}
