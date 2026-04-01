package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessControllerTest {

  @Mock private UserService userService;
  @Mock private BusinessService businessService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private BusinessController businessController;

  @Test
  void getBusiness_shouldFailWhenNotFound() {
    when(businessService.getBusinessById(100L)).thenReturn(null);

    var result = businessController.getBusiness(100L);

    assertFalse(result.getSuccess());
    assertEquals("Business NOT FOUND", result.getMessage());
  }

  @Test
  void addBusiness_shouldFailWhenOwnerMissing() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business request = new Business();
    request.setBusinessName("Shop");
    request.setBusinessOwnerId(20L);
    when(userService.getUserById(20L)).thenReturn(null);

    var result = businessController.addBusiness(request);

    assertFalse(result.getSuccess());
    assertEquals("BusinessOwner NOT FOUND", result.getMessage());
    verify(businessService, never()).addBusiness(request);
  }

  @Test
  void addBusiness_shouldAllowOwnerBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User owner = new User();
    owner.setId(9L);
    Business request = new Business();
    request.setBusinessName("Shop");
    request.setBusinessOwnerId(9L);
    when(userService.getUserById(9L)).thenReturn(owner);

    var result = businessController.addBusiness(request);

    assertTrue(result.getSuccess());
    assertSame(request, result.getData());
    assertFalse(Boolean.TRUE.equals(request.getDeleted()));
    assertNotNull(request.getCreateTime());
    verify(businessService).addBusiness(request);
    verify(compatibilityEnricher).enrichBusiness(request);
  }

  @Test
  void patchBusiness_shouldFillMissingFieldsFromExistingBusiness() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business existing = new Business();
    existing.setId(100L);
    existing.setBusinessOwnerId(9L);
    existing.setBusinessName("Old Shop");
    existing.setBusinessAddress("Old Address");
    existing.setBusinessExplain("Old Explain");
    existing.setStartPrice(new BigDecimal("20"));
    existing.setDeliveryPrice(new BigDecimal("3"));
    when(businessService.getBusinessById(100L)).thenReturn(existing);
    Business patch = new Business();
    patch.setRemarks("new remarks");

    var result = businessController.patchBusiness(100L, patch);

    assertTrue(result.getSuccess());
    assertEquals("Old Shop", patch.getBusinessName());
    assertEquals("Old Address", patch.getBusinessAddress());
    assertEquals("Old Explain", patch.getBusinessExplain());
    assertEquals(new BigDecimal("20"), patch.getStartPrice());
    assertEquals(new BigDecimal("3"), patch.getDeliveryPrice());
    assertEquals(9L, patch.getBusinessOwnerId());
    verify(businessService).updateBusiness(same(existing));
    verify(businessService).updateBusiness(same(patch));
  }

  @Test
  void deleteBusiness_shouldSoftDeleteForOwner() {
    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(9L);
    business.setDeleted(false);
    when(businessService.getBusinessById(100L)).thenReturn(business);
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = businessController.deleteBusiness(100L);

    assertTrue(result.getSuccess());
    assertEquals("Delete business successfully.", result.getData());
    assertTrue(Boolean.TRUE.equals(business.getDeleted()));
    verify(businessService).updateBusiness(business);
  }

  @Test
  void getMyBusinesses_shouldReturnOwnedBusinessesForBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setId(100L);
    when(businessService.getBusinessByOwnerId(9L)).thenReturn(List.of(business));

    var result = businessController.getMyBusinesses();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
  }
}