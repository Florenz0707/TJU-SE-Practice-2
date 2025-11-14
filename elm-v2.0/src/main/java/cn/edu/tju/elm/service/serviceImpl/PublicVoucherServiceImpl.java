package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.elm.exception.PublicVoucherException;
import cn.edu.tju.elm.model.BO.PublicVoucher;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.repository.PublicVoucherRepository;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.PublicVoucherSelector;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicVoucherServiceImpl implements PublicVoucherService {
    private final PublicVoucherRepository publicVoucherRepository;

    public PublicVoucherServiceImpl(PublicVoucherRepository publicVoucherRepository) {
        this.publicVoucherRepository = publicVoucherRepository;
    }

    public List<PublicVoucherVO> getPublicVouchers() throws PublicVoucherException {
        List<PublicVoucher> publicVouchers = EntityUtils.filterEntityList(publicVoucherRepository.findAll());
        List<PublicVoucherVO> publicVoucherVOS = new ArrayList<>();
        for (PublicVoucher publicVoucher : publicVouchers) {
            publicVoucherVOS.add(new PublicVoucherVO(publicVoucher));
        }
        return publicVoucherVOS;
    }

    public PublicVoucherVO getPublicVoucherById(Long id) throws PublicVoucherException {
        PublicVoucher publicVoucher = publicVoucherRepository.findById(id).orElse(null);
        if (publicVoucher == null || publicVoucher.getDeleted())
            throw new PublicVoucherException(PublicVoucherException.NOT_FOUND);
        return new PublicVoucherVO(publicVoucher);
    }

    public void createPublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException {
        PublicVoucher publicVoucher = PublicVoucher.createVoucher(
                publicVoucherVO.getThreshold(), publicVoucherVO.getValue(),
                publicVoucherVO.getClaimable(), publicVoucherVO.getValidDays()
        );
        publicVoucherRepository.save(publicVoucher);
    }

    public void deletePublicVoucher(Long id) throws PublicVoucherException {
        PublicVoucher publicVoucher = publicVoucherRepository.findById(id).orElse(null);
        if (publicVoucher == null || publicVoucher.getDeleted())
            throw new PublicVoucherException(PublicVoucherException.NOT_FOUND);
        EntityUtils.deleteEntity(publicVoucher);
        publicVoucherRepository.save(publicVoucher);
    }

    public void updatePublicVoucher(PublicVoucherVO publicVoucherVO) throws PublicVoucherException {
        PublicVoucher publicVoucher = publicVoucherRepository.findById(publicVoucherVO.getId()).orElse(null);
        if (publicVoucher == null || publicVoucher.getDeleted())
            throw new PublicVoucherException(PublicVoucherException.NOT_FOUND);
        PublicVoucher newPublicVoucher = PublicVoucher.createVoucher(
                publicVoucherVO.getThreshold(), publicVoucherVO.getValue(),
                publicVoucherVO.getClaimable(), publicVoucherVO.getValidDays()
        );
        EntityUtils.substituteEntity(publicVoucher, newPublicVoucher);
        publicVoucherRepository.save(publicVoucher);
        publicVoucherRepository.save(newPublicVoucher);
    }

    public PublicVoucherVO chooseBestPublicVoucherForTransaction(TransactionVO transactionVO, PublicVoucherSelector selector) throws PublicVoucherException {
        return selector.getBestPublicVoucher(getPublicVouchers(), transactionVO);
    }
}
