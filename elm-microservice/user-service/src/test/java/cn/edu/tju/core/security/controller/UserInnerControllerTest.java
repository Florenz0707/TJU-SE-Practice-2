package cn.edu.tju.core.security.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.internal.InternalServiceTokenFilter;
import cn.edu.tju.core.security.service.UserDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserInnerControllerTest {

  @Mock private UserDomainService userDomainService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    UserInnerController userInnerController = new UserInnerController(userDomainService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(userInnerController)
            .addFilters(new InternalServiceTokenFilter("expected-token"))
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getUserByIdRejectsMissingInternalToken() throws Exception {
    mockMvc
        .perform(get("/api/inner/users/1").servletPath("/api/inner/users/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Invalid or missing internal service token"));

    verifyNoInteractions(userDomainService);
  }

  @Test
  void getUserByIdRejectsInvalidInternalToken() throws Exception {
    mockMvc
        .perform(
            get("/api/inner/users/1")
          .servletPath("/api/inner/users/1")
                .header(InternalServiceTokenFilter.INTERNAL_SERVICE_TOKEN_HEADER, "wrong-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    verifyNoInteractions(userDomainService);
  }

  @Test
  void getUserByIdAllowsRequestWithValidInternalToken() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setUsername("internal-user");
    when(userDomainService.getUserById(1L)).thenReturn(user);

    mockMvc
        .perform(
            get("/api/inner/users/1")
          .servletPath("/api/inner/users/1")
                .header(
                    InternalServiceTokenFilter.INTERNAL_SERVICE_TOKEN_HEADER,
                    "expected-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.username").value("internal-user"));

    verify(userDomainService).getUserById(1L);
  }
}