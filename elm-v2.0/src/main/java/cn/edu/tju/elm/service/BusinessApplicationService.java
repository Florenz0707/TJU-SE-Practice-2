package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.repository.BusinessApplicationRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessApplicationService {

    private final BusinessApplicationRepository businessApplicationRepository;

    public BusinessApplicationService(BusinessApplicationRepository businessApplicationRepository) {
        this.businessApplicationRepository = businessApplicationRepository;
    }

    public void addApplication(BusinessApplication businessApplication){
        businessApplicationRepository.save(businessApplication);
    }

    public List<BusinessApplication> getAllBusinessApplications(){
        return businessApplicationRepository.findAll();
    }

    public BusinessApplication getBusinessApplicationById(Long id){
        Optional<BusinessApplication> businessApplicationOptional = businessApplicationRepository.findById(id);
        return businessApplicationOptional.map(Utils::filterEntity).orElse(null);
    }

    public BusinessApplication updateBusinessApplication(BusinessApplication businessApplication){
        return businessApplicationRepository.save(businessApplication);
    }

    public List<BusinessApplication> getBusinessApplicationsByApplicant(User applicant){
        return businessApplicationRepository.findAllByApplicant(applicant);
    }
}
