package com.alfatahi.erp.config;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            if (adminPassword == null || adminPassword.isBlank() || adminPassword.length() < 10) {
                throw new IllegalStateException("Defina ADMIN_PASSWORD com pelo menos 10 caracteres para criar o administrador inicial. Nenhum usuário admin foi criado.");
            }
            AppUser gestor = new AppUser();
            gestor.setUsername("admin");
            gestor.setPassword(passwordEncoder.encode(adminPassword));
            gestor.setRole("GESTAO");
            userRepository.save(gestor);
        }
    }
}
