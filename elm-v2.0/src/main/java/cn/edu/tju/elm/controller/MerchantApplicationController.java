package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.ApplicationState;
import cn.edu.tju.elm.model.BO.MerchantApplication;
import cn.edu.tju.elm.service.MerchantApplicationService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications/merchant")
@Tag(name = "管理成为商家申请", description = "提供用户成为商家申请的提交、审核和查询功能")
public class MerchantApplicationController {
    private final UserService userService;
    private final MerchantApplicationService merchantApplicationService;

    public MerchantApplicationController(UserService userService, MerchantApplicationService merchantApplicationService) {
        this.userService = userService;
        this.merchantApplicationService = merchantApplicationService;
    }

    @PostMapping("")
    @Operation(summary = "提交成为商家申请", description = "用户提交成为商家的申请")
    public HttpResult<MerchantApplication> addMerchantApplication(
            @Parameter(description = "商家申请信息", required = true) @RequestBody MerchantApplication merchantApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isBusiness)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY MERCHANT");

        if (merchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication CANT BE NULL");

        EntityUtils.setNewEntity(merchantApplication);
        merchantApplication.setApplicationState(ApplicationState.UNDISPOSED);
        merchantApplication.setApplicant(me);
        User admin = userService.getUserWithUsername("admin");
        merchantApplication.setHandler(admin);
        merchantApplicationService.addApplication(merchantApplication);
        return HttpResult.success(merchantApplication);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "获取所有商家申请", description = "管理员查询所有成为商家的申请列表")
    public HttpResult<List<MerchantApplication>> getMerchantApplications() {
        return HttpResult.success(merchantApplicationService.getAllMerchantApplications());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取商家申请", description = "查询指定商家申请的详细信息")
    public HttpResult<MerchantApplication> getMerchantApplication(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        MerchantApplication merchantApplication = merchantApplicationService.getMerchantApplicationById(id);
        if (merchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");

        if (isAdmin || (isBusiness && me.equals(merchantApplication.getApplicant())))
            return HttpResult.success(merchantApplication);

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "审核商家申请", description = "管理员审核成为商家的申请，通过后用户获得商家权限")
    public HttpResult<MerchantApplication> updateMerchantApplication(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id,
            @Parameter(description = "审核结果", required = true) @RequestBody MerchantApplication newMerchantApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        MerchantApplication merchantApplication = merchantApplicationService.getMerchantApplicationById(id);
        if (merchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");
        if (!merchantApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

        if (newMerchantApplication.getApplicationState() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");
        merchantApplication.setApplicationState(newMerchantApplication.getApplicationState());
        EntityUtils.updateEntity(merchantApplication);
        merchantApplicationService.updateMerchantApplication(merchantApplication);

        if (merchantApplication.getApplicationState().equals(ApplicationState.APPROVED)) {
            User applicant = merchantApplication.getApplicant();
            applicant.setAuthorities(AuthorityUtils.getAuthoritySet("USER BUSINESS"));
            EntityUtils.updateEntity(applicant);
            userService.updateUser(applicant);
        }
        return HttpResult.success(merchantApplication);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的商家申请", description = "用户查询自己提交的所有商家申请")
    public HttpResult<List<MerchantApplication>> getMyMerchantApplication() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User me = meOptional.get();

        return HttpResult.success(merchantApplicationService.getMyMerchantApplications(me.getId()));
    }
}
