package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.service.IntegrationOutboxService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class IntegrationOutboxAdminControllerTest {

  @Mock private IntegrationOutboxService integrationOutboxService;

  @InjectMocks private IntegrationOutboxAdminController controller;

  @AfterEach
  void cleanSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void summary_shouldReturnForbidden_whenNotAdmin() {
    SecurityContextHolder.getContext().setAuthentication(null);

    HttpResult<Map<String, Object>> result = controller.summary();

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), result.getCode());
  }

  @Test
  void summary_shouldReturnData_whenAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", java.util.List.of(() -> "ROLE_ADMIN")));
    Map<String, Object> summary = Map.of("pending", 1L, "failed", 0L);
    when(integrationOutboxService.getStatusSummary()).thenReturn(summary);

    HttpResult<Map<String, Object>> result = controller.summary();

    assertTrue(result.getSuccess());
    assertEquals(summary, result.getData());
    verify(integrationOutboxService).getStatusSummary();
  }

  @Test
  void dispatchNow_shouldReturnProcessedCount_whenAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", java.util.List.of(() -> "ADMIN")));
    when(integrationOutboxService.dispatchBatch()).thenReturn(3);

    HttpResult<Integer> result = controller.dispatchNow();

    assertTrue(result.getSuccess());
    assertEquals(3, result.getData());
    verify(integrationOutboxService).dispatchBatch();
  }
}
