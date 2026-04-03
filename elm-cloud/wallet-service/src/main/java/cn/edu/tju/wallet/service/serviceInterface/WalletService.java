package cn.edu.tju.wallet.service.serviceInterface;

import cn.edu.tju.wallet.exception.WalletException;
import cn.edu.tju.wallet.model.VO.WalletVO;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

public interface WalletService {
  @Transactional
  WalletVO createWallet(Long ownerId) throws WalletException;

  @Transactional
  void addVoucher(Long walletId, BigDecimal amount) throws WalletException;

  WalletVO getWalletById(Long walletId, Long operatorId, boolean isAdmin) throws WalletException;

  Long getWalletOwnerIdById(Long id) throws WalletException;

  WalletVO getWalletByOwnerId(Long ownerId) throws WalletException;
}
