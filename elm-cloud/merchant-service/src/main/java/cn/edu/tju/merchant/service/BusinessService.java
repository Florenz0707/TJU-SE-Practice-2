package cn.edu.tju.merchant.service;

import cn.edu.tju.merchant.model.Business;
import cn.edu.tju.merchant.repository.BusinessRepository;
import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
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

  /**
   * Public browsing endpoint: return only businesses that are considered "open".
   *
   * <p>Rule:
   *
   * <ul>
   *   <li>Soft-deleted businesses are excluded (EntityUtils.filterEntityList)
   *   <li>If openTime/closeTime are present, only keep those open at "now".
   *       <ul>
   *         <li>Supports overnight windows (e.g. 22:00-02:00)
   *       </ul>
   *   <li>If openTime/closeTime are missing, treat as open (backward compatibility).
   * </ul>
   */
  public List<Business> getOpenBusinesses() {
    List<Business> businesses = EntityUtils.filterEntityList(businessRepository.findAll());
    java.time.LocalTime now = java.time.LocalTime.now();

    List<Business> open =
        businesses.stream()
            .filter(
                b -> {
                  if (b == null) return false;
                  java.time.LocalTime openTime = b.getOpenTime();
                  java.time.LocalTime closeTime = b.getCloseTime();

                  // Compatibility: if no schedule info, assume open.
                  if (openTime == null || closeTime == null) return true;

                  // Normal window: [open, close)
                  if (!closeTime.isBefore(openTime) && !closeTime.equals(openTime)) {
                    return !now.isBefore(openTime) && now.isBefore(closeTime);
                  }

                  // Overnight window: e.g. 22:00 - 02:00
                  // Open if now >= open OR now < close
                  if (closeTime.isBefore(openTime)) {
                    return !now.isBefore(openTime) || now.isBefore(closeTime);
                  }

                  // If openTime == closeTime, ambiguous; treat as open.
                  return true;
                })
            .toList();

    compatibilityEnricher.enrichBusinesses(open);
    return open;
  }

  public Business getBusinessById(Long businessId) {
    Optional<Business> businessOptional = businessRepository.findById(businessId);
    Business business = businessOptional.map(e -> e).orElse(null);
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
