package com.alfatahi.erp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equals(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Sua sessão expirou por inatividade.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Sua sessão expirou ou o token de segurança é inválido.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/acesso-negado");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/public/**"))

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/login", "/css/**", "/js/**", "/public/**").permitAll()
                        .requestMatchers("/admin/users/**").hasAuthority("GESTAO")

                        .requestMatchers("/dashboard", "/payables/**", "/receivables/**",
                                "/losses/**", "/dre/**", "/suppliers/**",
                                "/settings/**", "/settings/users/**", "/cut-plans/**").hasAuthority("GESTAO")

                        .requestMatchers("/work-orders/**").hasAnyAuthority("GESTAO", "TECNICO")

                        .requestMatchers("/commercial/**", "/quotes/**", "/clients/**",
                                "/agenda/**", "/login-success").hasAnyAuthority("GESTAO", "VENDAS", "TECNICO")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/login-success", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );
        return http.build();
    }
}