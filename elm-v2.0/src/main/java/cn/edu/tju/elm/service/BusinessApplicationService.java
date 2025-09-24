package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.repository.BusinessApplicationRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BusinessApplicationService {

    private final BusinessApplicationRepository businessApplicationRepository;

    public BusinessApplicationService(BusinessApplicationRepository businessApplicationRepository) {
        this.businessApplicationRepository = businessApplicationRepository;
    }

    public List<BusinessApplication> getBusinessApplications(Long businessId){
        return Utils.filterEntityList(businessApplicationRepository.findAllByBusinessId(businessId));
    }

    public void addApplication(BusinessApplication businessApplication){
        businessApplicationRepository.save(businessApplication);
    }
}
