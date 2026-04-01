package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface PrivateVoucherService {
  @Transactional
  boolean createPrivateVoucher(Long walletId, PublicVoucherVO publicVoucherVO)
      throws PrivateVoucherException;

  @Transactional
  boolean redeemPrivateVoucher(Long id) throws PrivateVoucherException;

  @Transactional
  void restoreVoucher(Long voucherId) throws PrivateVoucherException;

  List<PrivateVoucherVO> getPrivateVouchers(Long userId) throws PrivateVoucherException;

  @Transactional
  void clearExpiredPrivateVouchers(Long userId) throws PrivateVoucherException;
}
