package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PrivateVoucher;

public class PrivateVoucherVO {
    private Long ownerId;
    private Long voucherId;

    public PrivateVoucherVO() {
    }

    public PrivateVoucherVO(PrivateVoucher privateVoucher) {
        ownerId = privateVoucher.getOwner().getId();
        voucherId = privateVoucher.getVoucher().getId();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    @Override
    public String toString() {
        return "PrivateVoucherVO: " +
                "ownerId=" + ownerId +
                ", voucherId=" + voucherId;
    }
}
