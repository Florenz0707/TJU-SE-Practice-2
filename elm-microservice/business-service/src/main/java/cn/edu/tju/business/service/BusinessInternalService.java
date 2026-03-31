package cn.edu.tju.business.service;

import cn.edu.tju.business.model.vo.BusinessSnapshotVO;
import cn.edu.tju.business.repository.BusinessRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessInternalService {
  private final BusinessRepository businessRepository;

  public BusinessInternalService(BusinessRepository businessRepository) {
    this.businessRepository = businessRepository;
  }

  @Transactional(readOnly = true)
  public List<BusinessSnapshotVO> getBusinessSnapshots() {
    return businessRepository.findAll().stream()
        .filter(business -> !Boolean.TRUE.equals(business.getDeleted()))
        .map(BusinessSnapshotVO::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public BusinessSnapshotVO getBusinessSnapshotById(Long businessId) {
    if (businessId == null) {
      return null;
    }
    return businessRepository.findById(businessId).map(BusinessSnapshotVO::new).orElse(null);
  }
}
