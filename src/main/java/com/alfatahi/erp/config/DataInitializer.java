package com.alfatahi.erp.config;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {

            String adminPass = System.getenv("ADMIN_PASSWORD");
            boolean isRandomPassword = false;

            if (adminPass == null || adminPass.isBlank()) {
                adminPass = UUID.randomUUID().toString().substring(0, 8);
                isRandomPassword = true;
            }

            AppUser gestor = new AppUser();
            gestor.setUsername("admin");
            gestor.setId(UUID.randomUUID());
            gestor.setPassword(passwordEncoder.encode(adminPass));
            gestor.setRole("GESTAO");
            userRepository.save(gestor);

        }
    }
}