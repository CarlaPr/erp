package com.alfatahi.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .csrf(csrf -> csrf.disable()) // Desabilitado para permitir requisições assíncronas/fetch sem travas
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // Permite renderizar o H2 console
                .authorizeHttpRequests(auth -> auth
                        // Rotas Públicas estáticas e de login
                        .requestMatchers("/login", "/css/**", "/js/**", "/h2-console/**").permitAll()

                        // REQUISITO: Bloqueios Exclusivos do Perfil GESTAO
                        .requestMatchers("/dashboard", "/work-orders/**", "/payables/**", "/receivables/**", "/losses/**", "/dre/**", "/conciliation/**", "/suppliers/**", "/settings/**").hasRole("GESTAO")

                        // REQUISITO: Módulos acessíveis por ambos ou pelo perfil VENDAS
                        .requestMatchers("/commercial/**", "/quotes/**", "/clients/**", "/login-success").hasAnyRole("GESTAO", "VENDAS")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/login-success", true) // Rota neutra inteligente para direcionar cada perfil
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}