package cn.edu.tju;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.cloud.config.import-check.enabled=false",
      "eureka.client.enabled=false"
    })
@ActiveProfiles("local")
class MyApplicationTests {

  @Test
  void contextLoads() {}
}
