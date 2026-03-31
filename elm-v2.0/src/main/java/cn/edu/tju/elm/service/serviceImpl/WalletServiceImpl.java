package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.elm.exception.WalletException;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.InternalAccountClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService {
  private final InternalAccountClient internalAccountClient;

  public WalletServiceImpl(InternalAccountClient internalAccountClient) {
    this.internalAccountClient = internalAccountClient;
  }

  public WalletVO createWallet(Long ownerId) throws WalletException {
    try {
      WalletVO wallet = internalAccountClient.createWallet(ownerId);
      if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
      return wallet;
    } catch (IllegalStateException e) {
      throw mapWalletException(e.getMessage());
    }
  }

  public void addVoucher(Long walletId, BigDecimal amount) {
    try {
      internalAccountClient.addVoucher(walletId, amount);
    } catch (IllegalStateException e) {
      throw mapWalletException(e.getMessage());
    }
  }

  public Long getWalletOwnerIdById(Long id) throws WalletException {
    try {
      Long ownerId = internalAccountClient.getWalletOwnerById(id);
      if (ownerId == null) throw new WalletException(WalletException.NOT_FOUND);
      return ownerId;
    } catch (IllegalStateException e) {
      throw mapWalletException(e.getMessage());
    }
  }

  public WalletVO getWalletByOwnerId(Long ownerId) throws WalletException {
    try {
      WalletVO wallet = internalAccountClient.getWalletByOwnerId(ownerId);
      if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
      return wallet;
    } catch (IllegalStateException e) {
      throw mapWalletException(e.getMessage());
    }
  }

  public WalletVO getWalletById(Long walletId, Long operatorId, boolean isAdmin)
      throws WalletException {
    try {
      WalletVO wallet = internalAccountClient.getWalletById(walletId, operatorId, isAdmin);
      if (wallet == null) throw new WalletException(WalletException.NOT_FOUND);
      return wallet;
    } catch (IllegalStateException e) {
      throw mapWalletException(e.getMessage());
    }
  }

  private WalletException mapWalletException(String message) {
    if (WalletException.ALREADY_EXISTS.equals(message)) {
      return new WalletException(WalletException.ALREADY_EXISTS);
    }
    if (WalletException.FORBIDDEN.equals(message)) {
      return new WalletException(WalletException.FORBIDDEN);
    }
    if (WalletException.ADD_VOUCHER_FAILED.equals(message)) {
      return new WalletException(WalletException.ADD_VOUCHER_FAILED);
    }
    return new WalletException(WalletException.NOT_FOUND.equals(message) ? WalletException.NOT_FOUND : message);
  }
}
