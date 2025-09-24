package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.MerchantApplicationService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理开店申请", description = "对商家提出的开店申请进行增删改查")
public class BusinessApplicationController {

    @Autowired
    private UserService userService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BusinessApplicationService businessApplicationService;

    @PostMapping("/applications/business")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<BusinessApplication> addBusinessApplication(@RequestBody BusinessApplication businessApplication) {
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

    @GetMapping("/applications/business")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<BusinessApplication>> getBusinessApplications() {
        return HttpResult.success(businessApplicationService.getAllBusinessApplications());
    }

    @GetMapping("/applications/business/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<BusinessApplication> getBusinessApplication(@PathVariable Long id) {
        BusinessApplication businessApplication = businessApplicationService.getBusinessApplicationById(id);
        if (businessApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");
        return HttpResult.success(businessApplicationService.getBusinessApplicationById(id));
    }

    @PatchMapping("/applications/business/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<BusinessApplication> approveBusinessApplication(@PathVariable Long id, @RequestBody BusinessApplication businessApplication) {
        Optional<User>  meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        BusinessApplication oldApplication = businessApplicationService.getBusinessApplicationById(id);
        if (oldApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

        if (oldApplication.getBusiness() == null || oldApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

        if (!oldApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

        businessApplication.setApplicationState(ApplicationState.APPROVED);
        businessApplication.setHandler(me);
        return HttpResult.success(businessApplication);

    }

    @PatchMapping("/applications/business/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<BusinessApplication> rejectBusinessApplication(@PathVariable Long id, @RequestBody BusinessApplication businessApplication) {
        Optional<User>  meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        BusinessApplication oldApplication = businessApplicationService.getBusinessApplicationById(id);
        if (oldApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

        if (oldApplication.getBusiness() == null || oldApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

        if (!oldApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

        businessApplication.setApplicationState(ApplicationState.REJECTED);
        businessApplication.setHandler(me);
        return HttpResult.success(businessApplication);
    }

}
