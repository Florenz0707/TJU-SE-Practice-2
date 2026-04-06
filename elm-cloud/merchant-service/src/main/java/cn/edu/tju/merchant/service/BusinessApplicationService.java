package cn.edu.tju.merchant.service;

import cn.edu.tju.merchant.model.BusinessApplication;
import cn.edu.tju.merchant.repository.BusinessApplicationRepository;
import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessApplicationService {

  private final BusinessApplicationRepository businessApplicationRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessApplicationService(
      BusinessApplicationRepository businessApplicationRepository,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.businessApplicationRepository = businessApplicationRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addApplication(BusinessApplication businessApplication) {
    businessApplicationRepository.save(businessApplication);
  }

  public List<BusinessApplication> getAllBusinessApplications() {
    List<BusinessApplication> apps = businessApplicationRepository.findAll();
    compatibilityEnricher.enrichBusinessApplications(apps);
    return apps;
  }

  public BusinessApplication getBusinessApplicationById(Long id) {
    Optional<BusinessApplication> businessApplicationOptional =
        businessApplicationRepository.findById(id);
    BusinessApplication app =
        businessApplicationOptional.orElse(null);
    compatibilityEnricher.enrichBusinessApplication(app);
    return app;
  }

  public void updateBusinessApplication(BusinessApplication businessApplication) {
    businessApplicationRepository.save(businessApplication);
  }

  public List<BusinessApplication> getBusinessApplicationsByApplicantId(Long applicantId) {
    if (applicantId == null) {
      return List.of();
    }
    List<BusinessApplication> allBusinessApplications = businessApplicationRepository.findAll();
    List<BusinessApplication> businessApplicationsByApplicant =
        new ArrayList<>(allBusinessApplications.size());
    for (BusinessApplication businessApplication : allBusinessApplications) {
      if (businessApplication == null) continue;

      // Prefer explicit applicantId (new field)
      if (businessApplication.getApplicantId() != null
          && businessApplication.getApplicantId().equals(applicantId)) {
        businessApplicationsByApplicant.add(businessApplication);
        continue;
      }

      // Backward compatibility: fall back to businessOwnerId matching
      if (businessApplication.getBusiness() != null
          && businessApplication.getBusiness().getBusinessOwnerId() != null
          && businessApplication.getBusiness().getBusinessOwnerId().equals(applicantId)) {
        businessApplicationsByApplicant.add(businessApplication);
      }
    }
    compatibilityEnricher.enrichBusinessApplications(businessApplicationsByApplicant);
    return businessApplicationsByApplicant;
  }
}
