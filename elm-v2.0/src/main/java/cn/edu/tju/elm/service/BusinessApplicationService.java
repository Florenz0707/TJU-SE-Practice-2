package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.model.Cart;
import cn.edu.tju.elm.repository.BusinessApplicationRepository;
import cn.edu.tju.elm.repository.CartItemRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessApplicationService {

    private final BusinessApplicationRepository businessApplicationRepository;

    public BusinessApplicationService(BusinessApplicationRepository businessApplicationRepository) {
        this.businessApplicationRepository = businessApplicationRepository;
    }

    public List<BusinessApplication> getBusinessApplications(long businessId){
        return Utils.removeDeleted(businessApplicationRepository.findALLByBusinessId(businessId));
    }

    public BusinessApplication addApplication(BusinessApplication businessApplication){
        return businessApplicationRepository.save(businessApplication);
    }
}
