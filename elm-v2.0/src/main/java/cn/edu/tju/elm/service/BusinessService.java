package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
import cn.edu.tju.elm.utils.EntityUtils;
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
        return EntityUtils.filterEntityList(businessRepository.findAll());
    }

    public Business getBusinessById(Long businessId) {
        Optional<Business> businessOptional = businessRepository.findById(businessId);
        return businessOptional.map(EntityUtils::filterEntity).orElse(null);
    }

    public List<Business> getBusinessByOwner(User owner) {
        List<Business> businessList = businessRepository.findAllByBusinessOwner(owner);
        return EntityUtils.filterEntityList(businessList);
    }

    public void addBusiness(Business business) {
        businessRepository.save(business);
    }

    public void updateBusiness(Business business) {
        businessRepository.save(business);
    }
}
