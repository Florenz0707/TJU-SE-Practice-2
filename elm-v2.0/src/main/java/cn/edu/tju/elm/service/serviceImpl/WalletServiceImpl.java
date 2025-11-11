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
    private final WalletService walletService;

    public WalletServiceImpl(WalletRepository walletRepository, UserService userService, WalletService walletService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
        this.walletService = walletService;
    }

    public WalletVO createWallet(User owner) {
        if (getWalletByOwner(owner) != null) return null;
        WalletBO walletBO = WalletBO.getNewWallet(owner);
        walletRepository.save(walletBO);
        return new WalletVO(walletBO);
    }

    public WalletVO addVoucher(BigDecimal amount, User owner) {
        WalletBO walletBO = walletRepository.findByOwner(owner).orElse(null);
        if (walletBO == null) return null;
        if (!walletBO.addVoucher(amount)) return null;
        EntityUtils.updateEntity(walletBO, owner);
        walletRepository.save(walletBO);
        return new WalletVO(walletBO);
    }

    public User getWalletOwnerById(Long id) {
        WalletBO walletBO = walletRepository.findById(id).orElse(null);
        if (walletBO == null) return null;
        return walletBO.getOwner();
    }

    public WalletVO getWalletByOwner(User owner) {
        WalletBO walletBO = walletRepository.findByOwner(owner).orElse(null);
        if (walletBO == null) return null;
        return new WalletVO(walletBO);
    }
}
