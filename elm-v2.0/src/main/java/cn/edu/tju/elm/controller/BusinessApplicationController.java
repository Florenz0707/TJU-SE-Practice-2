package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications/business")
@Tag(name = "管理开店申请", description = "对商家提出的开店申请进行增删改查")
public class BusinessApplicationController {

    @Autowired
    private UserService userService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BusinessApplicationService businessApplicationService;

    @PostMapping("")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<BusinessApplication> addBusinessApplication(
            @RequestBody BusinessApplication businessApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (businessApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication CANT BE NULL");

        if (businessApplication.getBusiness() == null || businessApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

        Business business = businessApplication.getBusiness();
        Utils.setNewEntity(business, me);
        business.setDeleted(true);
        businessService.addBusiness(business);

        Utils.setNewEntity(businessApplication, me);
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

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        boolean isBusiness = Utils.hasAuthority(me, "BUSINESS");

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

        if (me.equals(businessApplication.getHandler())) {
            oldApplication.setApplicationState(businessApplication.getApplicationState());
            oldApplication.setUpdateTime(LocalDateTime.now());
            oldApplication.setUpdater(me.getId());
            businessApplicationService.updateBusinessApplication(oldApplication);

            if (businessApplication.getApplicationState().equals(ApplicationState.APPROVED)) {
                Business business = businessApplication.getBusiness();
                business.setDeleted(false);
                business.setUpdater(me.getId());
                business.setUpdateTime(LocalDateTime.now());
                businessService.updateBusiness(business);
            }
            return HttpResult.success(businessApplication);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<List<BusinessApplication>> getMyBusinessApplication() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        return HttpResult.success(businessApplicationService.getBusinessApplicationsByApplicant(me));
    }
}
