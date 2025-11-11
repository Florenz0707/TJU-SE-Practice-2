package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.VO.WalletVO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface WalletService {
    @Transactional
    WalletVO createWallet(User owner);

    @Transactional
    WalletVO addVoucher(BigDecimal amount, User owner);

    User getWalletOwnerById(Long id);

    WalletVO getWalletByOwner(User owner);
}
