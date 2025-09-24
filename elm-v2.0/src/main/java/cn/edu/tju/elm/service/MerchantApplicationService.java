package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.MerchantApplication;
import cn.edu.tju.elm.repository.MerchantApplicationRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MerchantApplicationService {

    private final MerchantApplicationRepository merchantApplicationRepository;

    public MerchantApplicationService(MerchantApplicationRepository merchantApplicationRepository) {
        this.merchantApplicationRepository = merchantApplicationRepository;
    }

    public void addApplication(MerchantApplication merchantApplication){
        merchantApplicationRepository.save(merchantApplication);
    }

    public List<MerchantApplication> getAllMerchantApplications() {
        return merchantApplicationRepository.findAll();
    }

    public MerchantApplication getMerchantApplicationById(Long id) {
        Optional<MerchantApplication> merchantApplication = merchantApplicationRepository.findById(id);
        return merchantApplication.map(Utils::filterEntity).orElse(null);
    }

    public void updateMerchantApplication(MerchantApplication merchantApplication) {
        merchantApplicationRepository.save(merchantApplication);
    }
}
