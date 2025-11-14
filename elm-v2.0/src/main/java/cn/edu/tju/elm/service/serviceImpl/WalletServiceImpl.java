package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.WalletException;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;

    public WalletServiceImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public WalletVO createWallet(User owner) throws WalletException {
        if (walletRepository.findByOwner(owner).isPresent()) throw new WalletException(WalletException.ALREADY_EXISTS);
        Wallet wallet = Wallet.getNewWallet(owner);
        walletRepository.save(wallet);
        return new WalletVO(wallet);
    }

    public void addVoucher(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
        if (!wallet.addVoucher(amount)) throw new WalletException(WalletException.ADD_VOUCHER_FAILED);
        EntityUtils.updateEntity(wallet);
        walletRepository.save(wallet);
        new WalletVO(wallet);
    }

    public User getWalletOwnerById(Long id) throws WalletException {
        Wallet wallet = walletRepository.findById(id).orElse(null);
        if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
        return wallet.getOwner();
    }

    public WalletVO getWalletByOwner(User owner) throws WalletException {
        Wallet wallet = walletRepository.findByOwner(owner).orElse(null);
        if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
        return new WalletVO(wallet);
    }

    public WalletVO getWalletById(Long walletId, User operator) throws WalletException {
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null)
            throw new WalletException(WalletException.NOT_FOUND);
        if (!wallet.getOwner().equals(operator) && !AuthorityUtils.hasAuthority(operator, "ADMIN"))
            throw new WalletException(WalletException.FORBIDDEN);
        return new WalletVO(wallet);
    }
}
