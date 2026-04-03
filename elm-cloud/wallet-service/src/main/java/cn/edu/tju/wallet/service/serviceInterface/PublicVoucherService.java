package cn.edu.tju.wallet.service.serviceInterface;

import cn.edu.tju.wallet.exception.PublicVoucherException;
import cn.edu.tju.wallet.model.VO.PublicVoucherVO;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import cn.edu.tju.wallet.utils.PublicVoucherSelector;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface PublicVoucherService {
  List<PublicVoucherVO> getPublicVouchers() throws PublicVoucherException;

  PublicVoucherVO getPublicVoucherById(Long id) throws PublicVoucherException;

  @Transactional
  void createPublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException;

  @Transactional
  void deletePublicVoucher(Long id) throws PublicVoucherException;

  @Transactional
  void updatePublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException;

  PublicVoucherVO chooseBestPublicVoucherForTransaction(
      TransactionVO transactionVO, PublicVoucherSelector selector) throws PublicVoucherException;
}
