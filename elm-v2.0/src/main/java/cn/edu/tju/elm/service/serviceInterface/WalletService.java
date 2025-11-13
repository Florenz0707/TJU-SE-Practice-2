package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.WalletException;
import cn.edu.tju.elm.model.VO.WalletVO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface WalletService {
    @Transactional
    WalletVO createWallet(User owner) throws WalletException;

    @Transactional
    void addVoucher(Long walletId, BigDecimal amount) throws WalletException;

    WalletVO getWalletById(Long walletId, User operator) throws WalletException;

    User getWalletOwnerById(Long id) throws WalletException;

    WalletVO getWalletByOwner(User owner) throws WalletException;
}
