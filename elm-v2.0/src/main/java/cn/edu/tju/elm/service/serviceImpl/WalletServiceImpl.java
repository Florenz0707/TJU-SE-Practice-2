package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.WalletBO;
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

    public WalletVO createWallet(User user) {
        WalletBO walletBO = WalletBO.getNewWallet(user);
        walletRepository.save(walletBO);
        return new WalletVO(walletBO);
    }

    public WalletVO getWalletById(Long id) {
        WalletBO walletBO = walletRepository.findById(id).orElse(null);
        return walletBO == null ? null : new WalletVO(walletBO);
    }

    public WalletVO getWalletByOwnerId(Long ownerId) {
        WalletBO walletBO = walletRepository.findByOwnerId(ownerId).orElse(null);
        return walletBO == null ? null : new WalletVO(walletBO);
    }

    public WalletVO addVoucher(Long walletId, BigDecimal amount, User operator) {
        WalletBO walletBO = walletRepository.findByOwnerId(walletId).orElse(null);
        if (walletBO == null) return null;
        if (!walletBO.addVoucher(amount)) return null;
        EntityUtils.updateEntity(walletBO, operator);
        walletRepository.save(walletBO);
        return null;
    }
}
