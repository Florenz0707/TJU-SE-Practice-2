package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.ApplicationState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.BusinessApplication;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications/business")
@Tag(name = "管理开店申请", description = "对商家提出的开店申请进行增删改查")
public class BusinessApplicationController {
    private final UserService userService;
    private final BusinessService businessService;
    private final BusinessApplicationService businessApplicationService;

    public BusinessApplicationController(UserService userService, BusinessService businessService, BusinessApplicationService businessApplicationService) {
        this.userService = userService;
        this.businessService = businessService;
        this.businessApplicationService = businessApplicationService;
    }


    @PostMapping("")
    public HttpResult<BusinessApplication> addBusinessApplication(
            @RequestBody BusinessApplication businessApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (!AuthorityUtils.hasAuthority(me, "BUSINESS"))
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");

        if (businessApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication CANT BE NULL");

        if (businessApplication.getBusiness() == null || businessApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

        Business business = businessApplication.getBusiness();
        EntityUtils.setNewEntity(business);
        business.setDeleted(true);
        business.setBusinessOwner(me);
        businessService.addBusiness(business);

        EntityUtils.setNewEntity(businessApplication);
        businessApplication.setBusiness(business);
        businessApplication.setApplicationState(ApplicationState.UNDISPOSED);
        User admin = userService.getUserWithUsername("admin");
        businessApplication.setHandler(admin);
        businessApplicationService.addApplication(businessApplication);
        return HttpResult.success(businessApplication);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<BusinessApplication>> getBusinessApplications() {
        return HttpResult.success(businessApplicationService.getAllBusinessApplications());
    }

    @GetMapping("/{id}")
    public HttpResult<BusinessApplication> getBusinessApplication(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        BusinessApplication businessApplication = businessApplicationService.getBusinessApplicationById(id);
        if (businessApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

        if (isAdmin || (isBusiness && me.equals(businessApplication.getBusiness().getBusinessOwner())))
            return HttpResult.success(businessApplication);
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<BusinessApplication> handleBusinessApplication(
            @PathVariable Long id,
            @RequestBody BusinessApplication businessApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        BusinessApplication oldApplication = businessApplicationService.getBusinessApplicationById(id);
        if (oldApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

        if (oldApplication.getBusiness() == null || oldApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

        if (!oldApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

        if (businessApplication == null || businessApplication.getApplicationState() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");

        if (!ApplicationState.isValidApplicationState(oldApplication.getApplicationState()))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ApplicationState NOT VALID");

        if (me.equals(oldApplication.getHandler())) {
            oldApplication.setApplicationState(businessApplication.getApplicationState());
            EntityUtils.updateEntity(oldApplication);
            businessApplicationService.updateBusinessApplication(oldApplication);

            if (oldApplication.getApplicationState().equals(ApplicationState.APPROVED)) {
                Business business = oldApplication.getBusiness();
                business.setDeleted(false);
                business.setUpdateTime(LocalDateTime.now());
                businessService.updateBusiness(business);
            }
            return HttpResult.success(oldApplication);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/my")
    public HttpResult<List<BusinessApplication>> getMyBusinessApplication() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (!AuthorityUtils.hasAuthority(me, "BUSINESS"))
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");

        return HttpResult.success(businessApplicationService.getBusinessApplicationsByApplicant(me));
    }
}
