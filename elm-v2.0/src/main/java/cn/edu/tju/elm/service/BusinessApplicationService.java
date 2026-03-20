package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.BusinessApplication;
import cn.edu.tju.elm.repository.BusinessApplicationRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessApplicationService {

  private final BusinessApplicationRepository businessApplicationRepository;

  public BusinessApplicationService(BusinessApplicationRepository businessApplicationRepository) {
    this.businessApplicationRepository = businessApplicationRepository;
  }

  public void addApplication(BusinessApplication businessApplication) {
    businessApplicationRepository.save(businessApplication);
  }

  public List<BusinessApplication> getAllBusinessApplications() {
    return businessApplicationRepository.findAll();
  }

  public BusinessApplication getBusinessApplicationById(Long id) {
    Optional<BusinessApplication> businessApplicationOptional =
        businessApplicationRepository.findById(id);
    return businessApplicationOptional.map(EntityUtils::filterEntity).orElse(null);
  }

  public void updateBusinessApplication(BusinessApplication businessApplication) {
    businessApplicationRepository.save(businessApplication);
  }

  public List<BusinessApplication> getBusinessApplicationsByApplicantId(Long applicantId) {
    List<BusinessApplication> allBusinessApplications = businessApplicationRepository.findAll();
    List<BusinessApplication> businessApplicationsByApplicant =
        new ArrayList<>(allBusinessApplications.size());
    for (BusinessApplication businessApplication : allBusinessApplications) {
      if (businessApplication.getBusiness().getBusinessOwnerId().equals(applicantId)) {
        businessApplicationsByApplicant.add(businessApplication);
      }
    }
    return businessApplicationsByApplicant;
  }
}
