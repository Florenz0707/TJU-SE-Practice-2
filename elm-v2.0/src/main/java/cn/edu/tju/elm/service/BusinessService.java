package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class BusinessService {

    public final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public Business getBusinessById(Long businessId) {
        Optional<Business> businessOptional = businessRepository.findOneById(businessId);
//        if (businessOptional.isEmpty()) return null;
//        return businessOptional.get();
        return businessOptional.orElse(null);
    }

    public Business addBusiness(Business business) {
        return businessRepository.save(business);
    }
}
