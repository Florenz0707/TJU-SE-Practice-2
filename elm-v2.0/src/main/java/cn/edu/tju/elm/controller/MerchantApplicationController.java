package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.MerchantApplication;
import cn.edu.tju.elm.repository.MerchantApplicationRepository;
import cn.edu.tju.elm.service.MerchantApplicationService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理成为商家申请", description = "对用户提出的成为商家申请进行增删改查")
public class MerchantApplicationController {

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantApplicationService merchantApplicationService;
    @Autowired
    private MerchantApplicationRepository merchantApplicationRepository;

    @PostMapping("/applications/merchant")
    public HttpResult<MerchantApplication> addMerchantApplication(@RequestBody MerchantApplication merchantApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORYTY NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        boolean isBusiness = Utils.hasAuthority(me, "BUSINESS");
        if(isAdmin || isBusiness)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY MERCHANT");

        if(merchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MERCHANTAPPLICATION NOT FOUND");

        Utils.setNewEntity(merchantApplication, me);
        merchantApplication.setApplicationState(ApplicationState.UNDISPOSED);
        User admin = userService.getUserWithUsername("admin");
        merchantApplication.setHandler(admin);
        merchantApplicationService.addApplication(merchantApplication);
        return HttpResult.success(merchantApplication);
    }

    @GetMapping("/applications/merchant")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<MerchantApplication>> getMerchantApplications() {
        return HttpResult.success(merchantApplicationService.getAllMerchantApplications());
    }

    @GetMapping("/applications/merchant/{id}")
    public HttpResult<MerchantApplication> getMerchantApplication(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        boolean isBusiness = Utils.hasAuthority(me, "BUSINESS");

        MerchantApplication merchantApplication = merchantApplicationService.getMerchantApplicationById(id);
        if (merchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");

        if (isAdmin || (me.equals(merchantApplication.getApplicant())))
            return HttpResult.success(merchantApplication);

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN);
    }

    @PatchMapping("/applications/merchant/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<MerchantApplication> updateMerchantApplication(@PathVariable Long id, @RequestBody MerchantApplication merchantApplication) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORYTY NOT FOUND");
        User me = meOptional.get();

        MerchantApplication oldMerchantApplication = merchantApplicationService.getMerchantApplicationById(id);
        if (oldMerchantApplication == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");

        if (merchantApplication.getApplicant() == null || merchantApplication.getApplicant().getUsername() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Applicant.Username NOT FOUND");

        if (!oldMerchantApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

        oldMerchantApplication.setApplicationState(merchantApplication.getApplicationState());
        oldMerchantApplication.setUpdater(me.getId());
        oldMerchantApplication.setUpdateTime(LocalDateTime.now());
        merchantApplicationService.updateMerchantApplication(oldMerchantApplication);

        if (oldMerchantApplication.getApplicationState().equals(ApplicationState.APPROVED)) {
            User applicant = oldMerchantApplication.getApplicant();
            applicant.setAuthorities(Utils.getAuthoritySet("USER BUSINESS"));
            applicant.setUpdater(me.getId());
            applicant.setUpdateTime(LocalDateTime.now());
            userService.updateUser(applicant);
        }
        return HttpResult.success(oldMerchantApplication);
    }
}
