package cn.edu.tju.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import cn.edu.tju.core.security.JwtAccessDeniedHandler;
import cn.edu.tju.core.security.JwtAuthenticationEntryPoint;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.core.security.jwt.TokenProvider;

import java.util.Collections;

@Configuration  //标明这个类为配置类，spring应用程序一启动，类中的been 就会被初始化在spring容器中
@EnableWebSecurity  //开启spring security 自定义配置
public class WebSecurityConfig {


//    @Bean
//    public UserDetailsService userDetailsService(){
//
//        //1、创建基于内存的用户管理器
//        InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();
//        // 3、将第二步创建的UserDetail对象交给UserDetailsManager 管理
//        inMemoryUserDetailsManager.createUser(
//                //2、创建UserDetail 对象，用于管理用户名、用户密码、用户角色、用户权限
//                //下面代码创建了一个用户名为user,密码为123456，角色为user的用户对象
//                User.withDefaultPasswordEncoder()
//                        .username("user")
//                        .password("123456")
//                        .roles("user")
//                        .build());
//        return inMemoryUserDetailsManager;
//    }
//    @Bean
//    public UserDetailsService userDetailsService(){
//        //创建基于数据库的用户管理器
//        return new UserModelDetailsService();
//    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        // {bcrypt}
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域配置
     */
    CorsConfigurationSource configurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
        corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
        corsConfiguration.setAllowedOrigins(Collections.singletonList("*"));
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    private final String[] permitUrlArr = new String[]{
            "/hello",
            "/api/auth",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/**.jsp",
            "/**.html"
    };
    /**
     * 配置Spring Security安全链。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        //初始化jwt过滤器，并设置jwt公钥
        String a = "ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=";
        var jwtTokenFilter = new JWTFilter(new TokenProvider(a,86400L,108000L));
        //Security6.x关闭默认登录页
        httpSecurity.removeConfigurers(DefaultLoginPageConfigurer.class);
//        logger.info("注册JWT认证SecurityFilterChain");
        var chain = httpSecurity
                // 自定义权限拦截规则
                .authorizeHttpRequests((requests) -> {
                    //requests.anyRequest().permitAll(); //放行所有请求!!!
                    //允许匿名访问
                    requests
                            //自定可匿名访问地址，放到permitAllUrl中即可
                            .requestMatchers(permitUrlArr).permitAll()
                            //除上面声明的可匿名访问地址，其它所有请求全部需要进行认证
//                            .requestMatchers("/api/person").hasAuthority("USER")
//                            .requestMatchers("/api/hiddenmessage").hasAuthority("ADMIN")
                            .anyRequest()
                            .authenticated();
                })
                // 禁用HTTP响应标头
                .headers(headersCustomizer -> {headersCustomizer
                        .cacheControl(cache -> cache.disable())
                        .frameOptions(options -> options.sameOrigin());})
                //会话设为无状态,基于token，所以不需要session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //添加自定义的JWT认证筛选器，验证header中jwt有效性，将插入到UsernamePasswordAuthenticationFilter之前　
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                //禁用表单登录
                .formLogin(formLogin -> formLogin.disable())
                //禁用httpBasic登录
                .httpBasic(httpBasic -> httpBasic.disable())
                //禁用rememberMe
                .rememberMe(rememberMe -> rememberMe.disable())
                // 禁用CSRF，因为不使用session
                .csrf(csrf -> csrf.disable())
                //允许跨域请求
                .cors(Customizer.withDefaults())
                .build();
        return chain;
    }
}