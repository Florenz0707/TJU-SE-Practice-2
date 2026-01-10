package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.repository.PublicVoucherRepository;
import cn.edu.tju.elm.model.BO.PublicVoucher;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrivateVoucherServiceImpl implements PrivateVoucherService {
    private final WalletRepository walletRepository;
    private final PrivateVoucherRepository privateVoucherRepository;
    private final PublicVoucherRepository publicVoucherRepository;

    public PrivateVoucherServiceImpl(
            WalletRepository walletRepository,
            PrivateVoucherRepository privateVoucherRepository,
            PublicVoucherRepository publicVoucherRepository) {
        this.walletRepository = walletRepository;
        this.privateVoucherRepository = privateVoucherRepository;
        this.publicVoucherRepository = publicVoucherRepository;
    }

    @Transactional
    public boolean createPrivateVoucher(Long walletId, PublicVoucherVO publicVoucherVO) throws PrivateVoucherException {
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null)
            throw new PrivateVoucherException("Wallet Not Found");
        Long pubId = publicVoucherVO.getId();
        if (pubId != null) {
            if (privateVoucherRepository.existsByWalletIdAndPublicVoucherId(walletId, pubId)) {
                return false;
            }
            PublicVoucher pub = publicVoucherRepository.findById(pubId).orElse(null);
            if (pub == null) throw new PrivateVoucherException("PublicVoucher NOT FOUND");
            PrivateVoucher privateVoucher = PrivateVoucher.createPrivateVoucher(wallet, publicVoucherVO);
            privateVoucher.setPublicVoucher(pub);
            privateVoucherRepository.save(privateVoucher);
            return true;
        }
        PrivateVoucher privateVoucher = PrivateVoucher.createPrivateVoucher(wallet, publicVoucherVO);
        privateVoucherRepository.save(privateVoucher);
        return true;
    }

    @Transactional
    public boolean redeemPrivateVoucher(Long id) throws PrivateVoucherException {
        PrivateVoucher pv = privateVoucherRepository.findById(id).orElse(null);
        if (pv == null || pv.getDeleted() != null && pv.getDeleted())
            throw new PrivateVoucherException("PrivateVoucher NOT FOUND");
        boolean ok = pv.redeem();
        privateVoucherRepository.save(pv);
        return ok;
    }

    public List<PrivateVoucherVO> getPrivateVouchers(User user) throws PrivateVoucherException {
        if (user == null) throw new PrivateVoucherException("User CANT BE NULL");
        List<PrivateVoucher> pvList = privateVoucherRepository.findByWalletOwnerId(user.getId());
        java.util.List<PrivateVoucherVO> ret = new java.util.ArrayList<>();
        for (PrivateVoucher pv : pvList) {
            ret.add(new PrivateVoucherVO(pv));
        }
        return ret;
    }

    public void clearExpiredPrivateVouchers(User user) throws PrivateVoucherException {

    }
}
