package cn.edu.tju.elm.utils;

import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.TransactionVO;

import java.math.BigDecimal;
import java.util.List;

public class TOPUPPublicVoucherSelectorImpl implements PublicVoucherSelector {
    public PublicVoucherVO getBestPublicVoucher(
            List<PublicVoucherVO> publicVoucherVOList,
            TransactionVO transactionVO) {
        BigDecimal amount = transactionVO.getAmount();
        PublicVoucherVO ret = null;
        for (PublicVoucherVO publicVoucherVO : publicVoucherVOList) {
            if (!publicVoucherVO.getClaimable()) continue;
            if (publicVoucherVO.getThreshold().compareTo(amount) < 0) continue;
            if (ret == null || publicVoucherVO.getValue().compareTo(ret.getValue()) > 0)
                ret = publicVoucherVO;
        }
        return ret;
    }
}
