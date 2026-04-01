package cn.edu.tju.core.config;

import cn.edu.tju.core.security.controller.AuthenticationRestController;
import cn.edu.tju.core.security.controller.UserRestController;
import cn.edu.tju.elm.controller.AddressController;
import cn.edu.tju.elm.controller.BusinessApplicationController;
import cn.edu.tju.elm.controller.BusinessController;
import cn.edu.tju.elm.controller.CartController;
import cn.edu.tju.elm.controller.FoodController;
import cn.edu.tju.elm.controller.HttpRestController;
import cn.edu.tju.elm.controller.MerchantApplicationController;
import cn.edu.tju.elm.controller.OrderController;
import cn.edu.tju.elm.controller.PointsAdminController;
import cn.edu.tju.elm.controller.PointsController;
import cn.edu.tju.elm.controller.PointsInnerController;
import cn.edu.tju.elm.controller.PrivateVoucherController;
import cn.edu.tju.elm.controller.PublicVoucherController;
import cn.edu.tju.elm.controller.ReviewController;
import cn.edu.tju.elm.controller.TransactionController;
import cn.edu.tju.elm.controller.WalletController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tell the Spring container that this is a configuration class
@Configuration
public class SwaggerConfig {
  // Visit the website:http://127.0.0.1:8080/swagger-ui/index.html
  @Bean
  public OpenAPI springShopOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TJU SE Project")
                .description("Tianjin University Software Engineering Project")
                .version("v1"));
  }

  @Bean
  public GroupedOpenApi authControllerOpenApi() {
    return groupedControllerOpenApi(
        "controller-authentication", AuthenticationRestController.class);
  }

  @Bean
  public GroupedOpenApi userControllerOpenApi() {
    return groupedControllerOpenApi("controller-user", UserRestController.class);
  }

  @Bean
  public GroupedOpenApi addressControllerOpenApi() {
    return groupedControllerOpenApi("controller-address", AddressController.class);
  }

  @Bean
  public GroupedOpenApi businessApplicationControllerOpenApi() {
    return groupedControllerOpenApi(
        "controller-business-application", BusinessApplicationController.class);
  }

  @Bean
  public GroupedOpenApi businessControllerOpenApi() {
    return groupedControllerOpenApi("controller-business", BusinessController.class);
  }

  @Bean
  public GroupedOpenApi cartControllerOpenApi() {
    return groupedControllerOpenApi("controller-cart", CartController.class);
  }

  @Bean
  public GroupedOpenApi foodControllerOpenApi() {
    return groupedControllerOpenApi("controller-food", FoodController.class);
  }

  @Bean
  public GroupedOpenApi healthControllerOpenApi() {
    return groupedControllerOpenApi("controller-http-rest", HttpRestController.class);
  }

  @Bean
  public GroupedOpenApi merchantApplicationControllerOpenApi() {
    return groupedControllerOpenApi(
        "controller-merchant-application", MerchantApplicationController.class);
  }

  @Bean
  public GroupedOpenApi orderControllerOpenApi() {
    return groupedControllerOpenApi("controller-order", OrderController.class);
  }

  @Bean
  public GroupedOpenApi pointsAdminControllerOpenApi() {
    return groupedControllerOpenApi("controller-points-admin", PointsAdminController.class);
  }

  @Bean
  public GroupedOpenApi pointsControllerOpenApi() {
    return groupedControllerOpenApi("controller-points", PointsController.class);
  }

  @Bean
  public GroupedOpenApi pointsInnerControllerOpenApi() {
    return groupedControllerOpenApi("controller-points-inner", PointsInnerController.class);
  }

  @Bean
  public GroupedOpenApi privateVoucherControllerOpenApi() {
    return groupedControllerOpenApi("controller-private-voucher", PrivateVoucherController.class);
  }

  @Bean
  public GroupedOpenApi publicVoucherControllerOpenApi() {
    return groupedControllerOpenApi("controller-public-voucher", PublicVoucherController.class);
  }

  @Bean
  public GroupedOpenApi reviewControllerOpenApi() {
    return groupedControllerOpenApi("controller-review", ReviewController.class);
  }

  @Bean
  public GroupedOpenApi transactionControllerOpenApi() {
    return groupedControllerOpenApi("controller-transaction", TransactionController.class);
  }

  @Bean
  public GroupedOpenApi walletControllerOpenApi() {
    return groupedControllerOpenApi("controller-wallet", WalletController.class);
  }

  private GroupedOpenApi groupedControllerOpenApi(String groupName, Class<?> controllerType) {
    return GroupedOpenApi.builder()
        .group(groupName)
        .addOpenApiMethodFilter(method -> method.getDeclaringClass().equals(controllerType))
        .build();
  }
}
