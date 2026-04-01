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
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.BusinessApplication;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
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
class BusinessApplicationControllerTest {

  @Mock private UserService userService;
  @Mock private BusinessService businessService;
  @Mock private BusinessApplicationService businessApplicationService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private BusinessApplicationController businessApplicationController;

  @Test
  void addBusinessApplication_shouldRejectNonBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = businessApplicationController.addBusinessApplication(new BusinessApplication());

    assertFalse(result.getSuccess());
    assertEquals("NOT A MERCHANT YET", result.getMessage());
    verify(businessService, never()).addBusiness(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void addBusinessApplication_shouldCreateHiddenBusinessAndApplication() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User admin = new User();
    admin.setId(1L);
    when(userService.getUserWithUsername("admin")).thenReturn(admin);
    Business business = new Business();
    business.setBusinessName("Test Shop");
    BusinessApplication request = new BusinessApplication();
    request.setBusiness(business);

    var result = businessApplicationController.addBusinessApplication(request);

    assertTrue(result.getSuccess());
    assertSame(request, result.getData());
    assertTrue(Boolean.TRUE.equals(business.getDeleted()));
    assertEquals(9L, business.getBusinessOwnerId());
    assertFalse(Boolean.TRUE.equals(request.getDeleted()));
    assertEquals(ApplicationState.UNDISPOSED, request.getApplicationState());
    assertEquals(1L, request.getHandlerId());
    assertNotNull(request.getCreateTime());
    verify(businessService).addBusiness(business);
    verify(businessApplicationService).addApplication(request);
    verify(compatibilityEnricher).enrichBusinessApplication(request);
  }

  @Test
  void getBusinessApplication_shouldAllowOwnerBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setBusinessOwnerId(9L);
    BusinessApplication application = new BusinessApplication();
    application.setBusiness(business);
    when(businessApplicationService.getBusinessApplicationById(100L)).thenReturn(application);

    var result = businessApplicationController.getBusinessApplication(100L);

    assertTrue(result.getSuccess());
    assertSame(application, result.getData());
  }

  @Test
  void handleBusinessApplication_shouldRejectWhenHandlerMismatch() {
    User me = new User();
    me.setId(2L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setBusinessName("Test Shop");
    BusinessApplication stored = new BusinessApplication();
    stored.setBusiness(business);
    stored.setHandlerId(1L);
    stored.setApplicationState(ApplicationState.UNDISPOSED);
    when(businessApplicationService.getBusinessApplicationById(100L)).thenReturn(stored);
    BusinessApplication request = new BusinessApplication();
    request.setApplicationState(ApplicationState.APPROVED);

    var result = businessApplicationController.handleBusinessApplication(100L, request);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
  }

  @Test
  void handleBusinessApplication_shouldPublishBusinessWhenApproved() {
    User me = new User();
    me.setId(1L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setBusinessName("Test Shop");
    business.setDeleted(true);
    BusinessApplication stored = new BusinessApplication();
    stored.setBusiness(business);
    stored.setHandlerId(1L);
    stored.setApplicationState(ApplicationState.UNDISPOSED);
    when(businessApplicationService.getBusinessApplicationById(100L)).thenReturn(stored);
    BusinessApplication request = new BusinessApplication();
    request.setApplicationState(ApplicationState.APPROVED);

    var result = businessApplicationController.handleBusinessApplication(100L, request);

    assertTrue(result.getSuccess());
    assertEquals(ApplicationState.APPROVED, stored.getApplicationState());
    assertFalse(Boolean.TRUE.equals(business.getDeleted()));
    assertNotNull(business.getUpdateTime());
    verify(businessApplicationService).updateBusinessApplication(stored);
    verify(businessService).updateBusiness(business);
    verify(compatibilityEnricher).enrichBusinessApplication(stored);
  }

  @Test
  void getMyBusinessApplication_shouldReturnMineForBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    BusinessApplication application = new BusinessApplication();
    when(businessApplicationService.getBusinessApplicationsByApplicantId(9L))
        .thenReturn(List.of(application));

    var result = businessApplicationController.getMyBusinessApplication();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
  }
}