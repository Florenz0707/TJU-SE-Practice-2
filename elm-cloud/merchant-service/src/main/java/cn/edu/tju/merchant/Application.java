package cn.edu.tju.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.edu.tju.merchant", "cn.edu.tju"})
public class Application {
    public static void main(String[] args) {
        System.out.println("[merchant-service] booting with build marker: 2026-04-06-reqlog-v1");
        SpringApplication.run(Application.class, args);
    }
}
