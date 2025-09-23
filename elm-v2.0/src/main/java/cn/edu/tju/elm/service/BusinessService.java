package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.User;
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
        return Utils.checkEntityList(businessRepository.findAll());
    }

    public Business getBusinessById(Long businessId) {
        Optional<Business> businessOptional = businessRepository.findOneById(businessId);
        return businessOptional.map(Utils::checkEntity).orElse(null);
    }

    public List<Business> getBusinessByOwner(User owner) {
        List<Business> businessList = businessRepository.findAllByBusinessOwner(owner);
        return Utils.checkEntityList(businessList);
    }

    public void addBusiness(Business business) {
        businessRepository.save(business);
    }

    public void updateBusiness(Business business) {
        businessRepository.save(business);
    }
}
