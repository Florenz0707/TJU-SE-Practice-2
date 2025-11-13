package cn.edu.tju.elm.service.serviceInterface;

import cn.edu.tju.elm.exception.PublicVoucherException;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PublicVoucherService {
    List<PublicVoucherVO> getPublicVouchers() throws PublicVoucherException;

    PublicVoucherVO getPublicVoucherById(Long id) throws PublicVoucherException;

    @Transactional
    void createPublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException;

    @Transactional
    void deletePublicVoucher(Long id) throws PublicVoucherException;

    @Transactional
    void updatePublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException;
}
