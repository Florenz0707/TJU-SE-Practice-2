package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PrivateVoucherService {
    @Transactional
    boolean createPrivateVoucher(User user, PublicVoucherVO publicVoucherVO) throws PrivateVoucherException;

    @Transactional
    boolean redeemPrivateVoucher(Long id) throws PrivateVoucherException;

    List<PrivateVoucherVO> getPrivateVouchers(User user) throws PrivateVoucherException;

    @Transactional
    void clearExpiredPrivateVouchers(User user) throws PrivateVoucherException;
}
