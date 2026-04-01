package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.ApplicationState;
import cn.edu.tju.elm.model.BO.MerchantApplication;
import cn.edu.tju.elm.service.MerchantApplicationService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantApplicationControllerTest {

  @Mock private UserService userService;
  @Mock private MerchantApplicationService merchantApplicationService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private MerchantApplicationController merchantApplicationController;

  @Test
  void addMerchantApplication_shouldFailWhenAlreadyMerchant() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = merchantApplicationController.addMerchantApplication(new MerchantApplication());

    assertFalse(result.getSuccess());
    assertEquals("ALREADY MERCHANT", result.getMessage());
    verify(merchantApplicationService, never()).addApplication(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void addMerchantApplication_shouldSetDefaultsAndPersist() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User admin = new User();
    admin.setId(1L);
    when(userService.getUserWithUsername("admin")).thenReturn(admin);
    MerchantApplication request = new MerchantApplication();

    var result = merchantApplicationController.addMerchantApplication(request);

    assertTrue(result.getSuccess());
    assertSame(request, result.getData());
    assertEquals(ApplicationState.UNDISPOSED, request.getApplicationState());
    assertEquals(9L, request.getApplicantId());
    assertEquals(1L, request.getHandlerId());
    assertFalse(Boolean.TRUE.equals(request.getDeleted()));
    assertNotNull(request.getCreateTime());
    verify(merchantApplicationService).addApplication(request);
    verify(compatibilityEnricher).enrichMerchantApplication(request);
  }

  @Test
  void getMerchantApplication_shouldAllowAdmin() {
    User me = new User();
    me.setId(1L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    MerchantApplication application = new MerchantApplication();
    application.setId(100L);
    application.setApplicantId(9L);
    when(merchantApplicationService.getMerchantApplicationById(100L)).thenReturn(application);

    var result = merchantApplicationController.getMerchantApplication(100L);

    assertTrue(result.getSuccess());
    assertSame(application, result.getData());
  }

  @Test
  void updateMerchantApplication_shouldRejectDisposedApplication() {
    User me = new User();
    me.setId(1L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    MerchantApplication stored = new MerchantApplication();
    stored.setId(100L);
    stored.setApplicationState(ApplicationState.APPROVED);
    when(merchantApplicationService.getMerchantApplicationById(100L)).thenReturn(stored);
    MerchantApplication request = new MerchantApplication();
    request.setApplicationState(ApplicationState.REJECTED);

    var result = merchantApplicationController.updateMerchantApplication(100L, request);

    assertFalse(result.getSuccess());
    assertEquals("ALREADY DISPOSED", result.getMessage());
  }

  @Test
  void updateMerchantApplication_shouldGrantBusinessAuthorityWhenApproved() {
    User me = new User();
    me.setId(1L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    MerchantApplication stored = new MerchantApplication();
    stored.setId(100L);
    stored.setApplicantId(9L);
    stored.setApplicationState(ApplicationState.UNDISPOSED);
    when(merchantApplicationService.getMerchantApplicationById(100L)).thenReturn(stored);
    MerchantApplication request = new MerchantApplication();
    request.setApplicationState(ApplicationState.APPROVED);
    User applicant = new User();
    applicant.setId(9L);
    applicant.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserById(9L)).thenReturn(applicant);

    var result = merchantApplicationController.updateMerchantApplication(100L, request);

    assertTrue(result.getSuccess());
    assertEquals(ApplicationState.APPROVED, stored.getApplicationState());
    assertTrue(AuthorityUtils.hasAuthority(applicant, "BUSINESS"));
    verify(merchantApplicationService).updateMerchantApplication(stored);
    verify(userService).updateUser(applicant);
    verify(compatibilityEnricher).enrichMerchantApplication(stored);
  }

  @Test
  void getMyMerchantApplication_shouldReturnMine() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    MerchantApplication application = new MerchantApplication();
    application.setApplicantId(9L);
    when(merchantApplicationService.getMyMerchantApplications(9L)).thenReturn(List.of(application));

    var result = merchantApplicationController.getMyMerchantApplication();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
  }
}