package cn.edu.tju.wallet.utils;

import cn.edu.tju.wallet.model.VO.PublicVoucherVO;
import cn.edu.tju.wallet.model.VO.TransactionVO;
import java.util.List;

public interface PublicVoucherSelector {
  PublicVoucherVO getBestPublicVoucher(
      List<PublicVoucherVO> publicVoucherVOList, TransactionVO transactionVO);
}
