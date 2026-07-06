package com.alfatahi.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/public/**"))

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Libera rotas públicas
                        .requestMatchers("/login", "/css/**", "/js/**", "/public/**").permitAll()
                        .requestMatchers("/admin/users/**").hasAuthority("GESTAO")

                        .requestMatchers("/dashboard", "/payables/**", "/receivables/**",
                                "/losses/**", "/dre/**", "/conciliation/**", "/suppliers/**",
                                "/settings/**", "/settings/users/**").hasAuthority("GESTAO")

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
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}