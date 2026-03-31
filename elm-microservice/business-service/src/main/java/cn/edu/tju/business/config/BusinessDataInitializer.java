package cn.edu.tju.business.config;

import cn.edu.tju.business.model.bo.Business;
import cn.edu.tju.business.repository.BusinessRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BusinessDataInitializer implements CommandLineRunner {

  private final BusinessRepository businessRepository;

  public BusinessDataInitializer(BusinessRepository businessRepository) {
    this.businessRepository = businessRepository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (businessRepository.count() > 0) {
      return;
    }

    Business business = new Business();
    business.setBusinessName("示例商家");
    business.setBusinessOwnerId(1L);
    business.setBusinessAddress("天津大学北洋园校区");
    business.setBusinessExplain("默认联调用商家");
    business.setOrderTypeId(1);
    business.setStartPrice(new BigDecimal("20.00"));
    business.setDeliveryPrice(new BigDecimal("3.00"));
    business.setRemarks("seed");
    business.setOpenTime(LocalTime.of(8, 0));
    business.setCloseTime(LocalTime.of(22, 0));
    business.setDeleted(false);
    business.setCreateTime(LocalDateTime.now());
    business.setUpdateTime(LocalDateTime.now());
    businessRepository.save(business);
  }
}