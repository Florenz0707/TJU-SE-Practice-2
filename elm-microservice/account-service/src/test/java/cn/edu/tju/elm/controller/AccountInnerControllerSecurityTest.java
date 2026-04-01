package cn.edu.tju.elm.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.core.security.internal.InternalServiceTokenFilter;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.AccountInternalService;
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
class AccountInnerControllerSecurityTest {

  @Mock private AccountInternalService accountInternalService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AccountInnerController controller = new AccountInnerController(accountInternalService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .addFilters(new InternalServiceTokenFilter("expected-token"))
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getWalletByUserIdRejectsMissingInternalToken() throws Exception {
    mockMvc
        .perform(
            get("/api/inner/account/wallet/by-user/9")
                .queryParam("createIfAbsent", "true")
                .servletPath("/api/inner/account/wallet/by-user/9"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    verifyNoInteractions(accountInternalService);
  }

  @Test
  void getWalletByUserIdRejectsInvalidInternalToken() throws Exception {
    mockMvc
        .perform(
            get("/api/inner/account/wallet/by-user/9")
                .queryParam("createIfAbsent", "true")
                .servletPath("/api/inner/account/wallet/by-user/9")
                .header(InternalServiceTokenFilter.INTERNAL_SERVICE_TOKEN_HEADER, "wrong-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    verifyNoInteractions(accountInternalService);
  }

  @Test
  void getWalletByUserIdAllowsValidInternalToken() throws Exception {
    Wallet wallet = Wallet.getNewWallet(9L);
    wallet.setId(18L);
    wallet.addBalance(new java.math.BigDecimal("20.50"));
    when(accountInternalService.getWalletByUserId(9L, true)).thenReturn(new WalletVO(wallet));

    mockMvc
        .perform(
            get("/api/inner/account/wallet/by-user/9")
                .queryParam("createIfAbsent", "true")
                .servletPath("/api/inner/account/wallet/by-user/9")
                .header(
                    InternalServiceTokenFilter.INTERNAL_SERVICE_TOKEN_HEADER,
                    "expected-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.id").value(18))
        .andExpect(jsonPath("$.data.ownerId").value(9));

    verify(accountInternalService).getWalletByUserId(9L, true);
  }
}