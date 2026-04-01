package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessService {

  private final BusinessRepository businessRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessService(
      BusinessRepository businessRepository, ResponseCompatibilityEnricher compatibilityEnricher) {
    this.businessRepository = businessRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public List<Business> getBusinesses() {
    List<Business> businesses = EntityUtils.filterEntityList(businessRepository.findAll());
    compatibilityEnricher.enrichBusinesses(businesses);
    return businesses;
  }

  public Business getBusinessById(Long businessId) {
    Optional<Business> businessOptional = businessRepository.findById(businessId);
    Business business = businessOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichBusiness(business);
    return business;
  }

  public List<Business> getBusinessByOwnerId(Long ownerId) {
    List<Business> businessList = businessRepository.findAllByBusinessOwnerId(ownerId);
    List<Business> ret = EntityUtils.filterEntityList(businessList);
    compatibilityEnricher.enrichBusinesses(ret);
    return ret;
  }

  public void addBusiness(Business business) {
    businessRepository.save(business);
  }

  public void updateBusiness(Business business) {
    businessRepository.save(business);
  }
}
