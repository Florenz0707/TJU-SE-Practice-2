package cn.edu.tju.elm.utils;

import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.TransactionVO;

import java.math.BigDecimal;
import java.util.List;

public class TOPUPPublicVoucherSelectorImpl implements PublicVoucherSelector {
    public PublicVoucherVO getBestPublicVoucher(
            List<PublicVoucherVO> publicVoucherVOList,
            TransactionVO transactionVO) {
        BigDecimal amount = transactionVO == null || transactionVO.getAmount() == null
                ? BigDecimal.ZERO : transactionVO.getAmount();
        PublicVoucherVO ret = null;
        for (PublicVoucherVO publicVoucherVO : publicVoucherVOList) {
            if (!publicVoucherVO.getClaimable()) continue;
            // threshold 表示券的最低消费门槛，门槛大于交易金额则不满足
            if (publicVoucherVO.getThreshold().compareTo(amount) > 0) continue;
            if (ret == null || publicVoucherVO.getValue().compareTo(ret.getValue()) > 0)
                ret = publicVoucherVO;
        }
        return ret;
    }
}
