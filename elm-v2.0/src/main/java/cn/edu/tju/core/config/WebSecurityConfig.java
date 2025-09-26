package cn.edu.tju.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import cn.edu.tju.core.security.jwt.JWTFilter;
import cn.edu.tju.core.security.jwt.TokenProvider;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域配置
     * This bean is picked up by .cors(Customizer.withDefaults())
     */
    @Bean // <-- IMPORTANT: Expose this as a Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173", "http://localhost", "http://127.0.0.1", "http://localhost:4173", "http://127.0.0.1:4173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private final String[] permitUrlArr = new String[]{
            "/hello",
            "/api/auth",
            "/api/persons",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/**.jsp",
            "/**.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        String token = "ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=";
        var jwtTokenFilter = new JWTFilter(new TokenProvider(token, 86400L, 108000L));

        httpSecurity.removeConfigurers(DefaultLoginPageConfigurer.class);

        return httpSecurity
                // Enable CORS using the corsConfigurationSource bean
                .cors(Customizer.withDefaults())
                // Disable CSRF since you are using JWT
                .csrf(AbstractHttpConfigurer::disable)
                // Authorize HTTP requests
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(permitUrlArr).permitAll()
                        .anyRequest().authenticated()
                )
                // Set session management to stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Disable unnecessary headers
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // Add your custom JWT filter
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // Disable default login mechanisms
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .build();
    }
}
