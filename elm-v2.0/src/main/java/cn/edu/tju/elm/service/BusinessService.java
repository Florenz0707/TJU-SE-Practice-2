package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.repository.BusinessRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessService {

  private final BusinessRepository businessRepository;
  private final InternalCatalogClient internalCatalogClient;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessService(
      BusinessRepository businessRepository,
      InternalCatalogClient internalCatalogClient,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.businessRepository = businessRepository;
    this.internalCatalogClient = internalCatalogClient;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public List<Business> getBusinesses() {
    List<Business> businesses = EntityUtils.filterEntityList(businessRepository.findAll());
    if (businesses.isEmpty()) {
      businesses =
          new ArrayList<>(
              internalCatalogClient.getBusinessSnapshots().stream()
                  .filter(snapshot -> !Boolean.TRUE.equals(snapshot.deleted()))
                  .map(this::toBusiness)
                  .toList());
    }
    compatibilityEnricher.enrichBusinesses(businesses);
    return businesses;
  }

  public Business getBusinessById(Long businessId) {
    Optional<Business> businessOptional = businessRepository.findById(businessId);
    Business business = businessOptional.map(EntityUtils::filterEntity).orElse(null);
    if (business == null) {
      InternalCatalogClient.BusinessSnapshot snapshot =
          internalCatalogClient.getBusinessSnapshot(businessId);
      if (snapshot != null && !Boolean.TRUE.equals(snapshot.deleted())) {
        business = toBusiness(snapshot);
      }
    }
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

  private Business toBusiness(InternalCatalogClient.BusinessSnapshot snapshot) {
    Business business = new Business();
    business.setId(snapshot.businessId());
    business.setBusinessName(snapshot.businessName());
    business.setBusinessOwnerId(snapshot.businessOwnerId());
    business.setBusinessAddress(snapshot.businessAddress());
    business.setBusinessExplain(snapshot.businessExplain());
    business.setBusinessImg(snapshot.businessImg());
    business.setOrderTypeId(snapshot.orderTypeId());
    business.setDeleted(snapshot.deleted());
    business.setStartPrice(snapshot.startPrice());
    business.setDeliveryPrice(snapshot.deliveryPrice());
    business.setRemarks(snapshot.remarks());
    business.setOpenTime(snapshot.openTime());
    business.setCloseTime(snapshot.closeTime());
    return business;
  }
}
