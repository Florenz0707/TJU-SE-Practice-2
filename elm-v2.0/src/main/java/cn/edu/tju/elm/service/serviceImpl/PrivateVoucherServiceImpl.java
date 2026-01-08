package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivateVoucherServiceImpl implements PrivateVoucherService {
    private final WalletRepository walletRepository;
    private final PrivateVoucherRepository privateVoucherRepository;

    public PrivateVoucherServiceImpl(
            WalletRepository walletRepository,
            PrivateVoucherRepository privateVoucherRepository) {
        this.walletRepository = walletRepository;
        this.privateVoucherRepository = privateVoucherRepository;
    }

    public boolean createPrivateVoucher(Long walletId, PublicVoucherVO publicVoucherVO) throws PrivateVoucherException {
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null)
            throw new PrivateVoucherException("Wallet Not Found");
        PrivateVoucher privateVoucher = PrivateVoucher.createPrivateVoucher(wallet, publicVoucherVO);
        privateVoucherRepository.save(privateVoucher);
        return true;
    }

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
