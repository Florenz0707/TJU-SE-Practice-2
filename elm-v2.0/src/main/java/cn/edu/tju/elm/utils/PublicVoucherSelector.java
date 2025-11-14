package cn.edu.tju.elm.utils;

import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.TransactionVO;

import java.util.List;

public interface PublicVoucherSelector {
    PublicVoucherVO getBestPublicVoucher(List<PublicVoucherVO> publicVoucherVOList, TransactionVO transactionVO);
}
