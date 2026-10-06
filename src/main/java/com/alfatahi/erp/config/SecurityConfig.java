package com.alfatahi.erp.config;

import com.alfatahi.erp.security.LoginAttemptService;
import com.alfatahi.erp.security.PageAccessService;
import com.alfatahi.erp.security.PageModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;
    private final PageAccessService pageAccess;

    public SecurityConfig(LoginAttemptService loginAttemptService, PageAccessService pageAccess) {
        this.loginAttemptService = loginAttemptService;
        this.pageAccess = pageAccess;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();

        return "XMLHttpRequest".equals(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Sua sessão expirou por inatividade.\"}");
            } else {

                response.sendRedirect(request.getContextPath() + "/login?expired");
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {

            loginAttemptService.recordSuccess(authentication.getName());

            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":\"ok\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login-success");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {

            String username = request.getParameter("username");
            if (!loginAttemptService.isLocked(username)) {
                loginAttemptService.recordFailure(username);
            }
            boolean locked = loginAttemptService.isLocked(username);

            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(locked
                        ? "{\"status\":\"error\",\"error\":\"ACCOUNT_LOCKED\",\"message\":\"Conta temporariamente bloqueada por excesso de tentativas de login. Aguarde 10 minutos após o bloqueio e tente novamente.\"}"
                        : "{\"status\":\"error\",\"message\":\"Usuário ou senha inválidos.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + (locked ? "/login?locked" : "/login?error"));
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            boolean sessionLikelyExpired = accessDeniedException instanceof CsrfException;

            if (isAjaxRequest(request)) {
                if (sessionLikelyExpired) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Sua sessão expirou por inatividade.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"ACCESS_DENIED\",\"message\":\"Você não tem permissão para executar esta ação.\"}");
                }
            } else {
                if (sessionLikelyExpired) {
                    response.sendRedirect(request.getContextPath() + "/login?expired");
                } else {
                    response.sendRedirect(request.getContextPath() + "/acesso-negado");
                }
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

                                                .requestMatchers("/login", "/css/**", "/js/**", "/public/**", "/ping").permitAll()
                        .requestMatchers("/AppAssets/**", "/favicon.ico",
                                "/apple-touch-icon.png", "/web-app-manifest-192x192.png",
                                "/web-app-manifest-512x512.png", "/site.webmanifest").permitAll()
                        .requestMatchers(PageAccessService::isManagementRequest)
                        .access((authentication, context) -> new AuthorizationDecision(
                                pageAccess.isManager(authentication.get(), context.getRequest())))
                        .requestMatchers(request -> PageModule.fromPath(PageAccessService.requestPath(request)).isPresent())
                        .access((authentication, context) -> new AuthorizationDecision(
                                pageAccess.authorize(authentication.get(), context.getRequest())))
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );
        return http.build();
    }
}
