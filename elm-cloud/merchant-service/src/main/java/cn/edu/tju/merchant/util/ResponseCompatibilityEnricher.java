package cn.edu.tju.merchant.util;
import org.springframework.stereotype.Component;
import java.util.List;
import cn.edu.tju.merchant.model.*;
import cn.edu.tju.merchant.service.UserService;
import java.util.Optional;

@Component
public class ResponseCompatibilityEnricher {

    private final UserService userService;

    public ResponseCompatibilityEnricher(UserService userService) {
        this.userService = userService;
    }

    private UserSummaryView toSummaryView(Optional<User> userOptional) {
        if (userOptional == null || userOptional.isEmpty()) return null;
        User u = userOptional.get();
        return new UserSummaryView(u.getId(), u.getUsername());
    }

    public void enrichBusiness(Business business) {
        if (business == null) return;
        if (business.getBusinessOwner() == null && business.getBusinessOwnerId() != null) {
            business.setBusinessOwner(toSummaryView(userService.getUserById(business.getBusinessOwnerId())));
        }
    }

    public void enrichBusinesses(List<Business> businesses) {
        if (businesses == null) return;
        for (Business b : businesses) {
            enrichBusiness(b);
        }
    }

    public void enrichBusinessApplication(BusinessApplication app) {
        if (app == null) return;
        enrichBusiness(app.getBusiness());

        // Fill applicant summary if missing
        if (app.getApplicant() == null && app.getApplicantId() != null) {
            userService.getUserById(app.getApplicantId())
                .ifPresentOrElse(
                    u -> app.setApplicant(toSummaryView(Optional.of(u))),
                    () -> app.setApplicant(new UserSummaryView(app.getApplicantId(), null))
                );
        }

        if (app.getHandler() == null && app.getHandlerId() != null) {
            app.setHandler(toSummaryView(userService.getUserById(app.getHandlerId())));
        }
    }

    public void enrichBusinessApplications(List<BusinessApplication> apps) {
        if (apps == null) return;
        for (BusinessApplication a : apps) {
            enrichBusinessApplication(a);
        }
    }

    public void enrichMerchantApplication(MerchantApplication app) {
        if (app == null) return;
        // 前端 types.ts 需要 applicant: User（非可选），这里用精简视图填充。
        if (app.getApplicant() == null && app.getApplicantId() != null) {
            app.setApplicant(toSummaryView(userService.getUserById(app.getApplicantId())));
            // 如果 user-service 暂时不可用，至少回填 id，避免前端直接崩。
            if (app.getApplicant() == null) {
                app.setApplicant(new UserSummaryView(app.getApplicantId(), null));
            }
        }
        if (app.getHandler() == null && app.getHandlerId() != null) {
            app.setHandler(toSummaryView(userService.getUserById(app.getHandlerId())));
        }
    }

    public void enrichMerchantApplications(List<MerchantApplication> apps) {
        if (apps == null) return;
        for (MerchantApplication a : apps) {
            enrichMerchantApplication(a);
        }
    }
}
