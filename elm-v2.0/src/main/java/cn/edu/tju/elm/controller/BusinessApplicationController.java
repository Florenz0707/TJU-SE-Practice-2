package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.service.AddressService;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PostMapping("/business-applications")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<BusinessApplication> addBusinessApplication(@RequestBody BusinessApplication businessApplication) {

        Optional<User> meOptional = userService.getUserWithAuthorities();
        if(meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if(businessApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication CANT BE NULL");

        if(businessApplication.getBusiness() == null || businessApplication.getBusiness().getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");

        Business business = businessApplication.getBusiness();
        business.setDeleted(true);
        business.setCreator(me.getId());
        business.setCreateTime(LocalDateTime.now());
        business.setUpdateTime(LocalDateTime.now());
        business.setUpdater(me.getId());
        business.setBusinessOwner(me);

        businessApplication.setBusiness(business);
               // getBusinessById(businessApplication.getBusiness().getId());
//        if(business == null)
//            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");


        businessApplication.setCreateTime(LocalDateTime.now());
        businessApplication.setUpdateTime(LocalDateTime.now());
        businessApplication.setCreator(business.getBusinessOwner().getId());
        businessApplication.setUpdater(business.getBusinessOwner().getId());
        businessApplication.setApplicationState(ApplicationState.UNDISPOSED);
        businessApplicationService.addApplication(businessApplication);
        return HttpResult.success(businessApplication);

    }
}
