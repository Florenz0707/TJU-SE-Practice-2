package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.MerchantApplication;
import cn.edu.tju.elm.repository.MerchantApplicationRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MerchantApplicationService {

  private final MerchantApplicationRepository merchantApplicationRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public MerchantApplicationService(
      MerchantApplicationRepository merchantApplicationRepository,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.merchantApplicationRepository = merchantApplicationRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addApplication(MerchantApplication merchantApplication) {
    merchantApplicationRepository.save(merchantApplication);
  }

  public List<MerchantApplication> getAllMerchantApplications() {
    List<MerchantApplication> apps = merchantApplicationRepository.findAll();
    compatibilityEnricher.enrichMerchantApplications(apps);
    return apps;
  }

  public MerchantApplication getMerchantApplicationById(Long id) {
    Optional<MerchantApplication> merchantApplication = merchantApplicationRepository.findById(id);
    MerchantApplication app = merchantApplication.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichMerchantApplication(app);
    return app;
  }

  public void updateMerchantApplication(MerchantApplication merchantApplication) {
    merchantApplicationRepository.save(merchantApplication);
  }

  public List<MerchantApplication> getMyMerchantApplications(Long applicantId) {
    List<MerchantApplication> apps =
        merchantApplicationRepository.findAllByApplicantId(applicantId);
    compatibilityEnricher.enrichMerchantApplications(apps);
    return apps;
  }
}
