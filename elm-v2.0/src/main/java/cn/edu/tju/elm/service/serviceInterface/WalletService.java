package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.VO.WalletVO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface WalletService {
    @Transactional
    WalletVO createWallet(User user);

    WalletVO getWalletById(Long id);

    WalletVO getWalletByOwnerId(Long ownerId);

    @Transactional
    WalletVO addVoucher(Long walletId, BigDecimal amount, User operator);
}
