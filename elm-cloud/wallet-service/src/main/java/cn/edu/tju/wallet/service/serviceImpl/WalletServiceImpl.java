package cn.edu.tju.wallet.service.serviceImpl;

import cn.edu.tju.wallet.exception.WalletException;
import cn.edu.tju.wallet.model.BO.Wallet;
import cn.edu.tju.wallet.model.VO.WalletVO;
import cn.edu.tju.wallet.repository.WalletRepository;
import cn.edu.tju.wallet.service.serviceInterface.WalletService;
import cn.edu.tju.wallet.utils.EntityUtils;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService {
  private final WalletRepository walletRepository;

  public WalletServiceImpl(WalletRepository walletRepository) {
    this.walletRepository = walletRepository;
  }

  public WalletVO createWallet(Long ownerId) throws WalletException {
    if (ownerId == null) throw new WalletException(WalletException.NOT_FOUND);
    if (walletRepository.findByOwnerId(ownerId).isPresent())
      throw new WalletException(WalletException.ALREADY_EXISTS);
    Wallet wallet = Wallet.getNewWallet(ownerId);
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

  public Long getWalletOwnerIdById(Long id) throws WalletException {
    Wallet wallet = walletRepository.findById(id).orElse(null);
    if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
    return wallet.getOwnerId();
  }

  public WalletVO getWalletByOwnerId(Long ownerId) throws WalletException {
    Wallet wallet = walletRepository.findByOwnerId(ownerId).orElse(null);
    if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
    return new WalletVO(wallet);
  }

  public WalletVO getWalletById(Long walletId, Long operatorId, boolean isAdmin)
      throws WalletException {
    Wallet wallet = walletRepository.findById(walletId).orElse(null);
    if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
    if (!wallet.getOwnerId().equals(operatorId) && !isAdmin)
      throw new WalletException(WalletException.FORBIDDEN);
    return new WalletVO(wallet);
  }
}
