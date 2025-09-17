package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
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
        List<Business> businessList = businessRepository.findAll();
        for (int i = 0; i < businessList.size(); ++i) {
            if (businessList.get(i).getDeleted()) businessList.remove(i--);
        }
        return businessList;
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
