package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessService {

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public List<Business> getBusinesses() {
        return Utils.removeDeleted(businessRepository.findAll());
    }

    public Business getBusinessById(Long businessId) {
        Optional<Business> businessOptional = businessRepository.findOneById(businessId);
        if (businessOptional.isEmpty() || businessOptional.get().getDeleted()) return null;
        return businessOptional.get();
    }

    public Business addBusiness(Business business) {
        return businessRepository.save(business);
    }

    public void updateBusiness(Business business) {
        businessRepository.save(business);
    }


}
