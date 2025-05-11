package com.example.CultureLoop.config;

import com.example.CultureLoop.service.CustomOAuth2UserService;
import com.example.CultureLoop.service.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CorsConfigurationSource corsConfigurationSource;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors((cors) -> {
                    cors.configurationSource(this.corsConfigurationSource);
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**", "/challenge", "/challenge/").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 인증 실패 (ex: JWT 없음 또는 잘못됨)
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Unauthorized - 인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 인가 실패 (ex: 권한 부족)
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Forbidden - 권한이 없습니다.\"}");
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // 사용자 정의 OAuth2UserService 적용
                        )
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
