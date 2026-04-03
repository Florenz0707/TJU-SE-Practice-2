package cn.edu.tju.merchant.util;
import org.springframework.stereotype.Component;
import java.util.List;
import cn.edu.tju.merchant.model.*;

@Component
public class ResponseCompatibilityEnricher {
    public void enrichBusiness(Business business) {}
    public void enrichBusinesses(List<Business> businesses) {}
    public void enrichBusinessApplication(BusinessApplication app) {}
    public void enrichBusinessApplications(List<BusinessApplication> apps) {}
    public void enrichMerchantApplication(MerchantApplication app) {}
    public void enrichMerchantApplications(List<MerchantApplication> apps) {}
}
