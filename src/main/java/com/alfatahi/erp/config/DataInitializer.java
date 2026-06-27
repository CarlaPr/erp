package com.alfatahi.erp.config;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminPass = System.getenv("ADMIN_PASSWORD");
        String vendorPass = System.getenv("VENDOR_PASSWORD");

        if (adminPass == null || adminPass.isBlank()) {
            throw new IllegalStateException(
                    "Variável de ambiente ADMIN_PASSWORD não definida. " +
                            "Defina antes de iniciar a aplicação.");
        }
        if (vendorPass == null || vendorPass.isBlank()) {
            throw new IllegalStateException(
                    "Variável de ambiente VENDOR_PASSWORD não definida.");
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser gestor = new AppUser();
            gestor.setUsername("admin");
            gestor.setPassword(passwordEncoder.encode(adminPass));
            gestor.setRole("GESTAO");
            userRepository.save(gestor);
        }

        if (userRepository.findByUsername("vendedor").isEmpty()) {
            AppUser vendedor = new AppUser();
            vendedor.setUsername("vendedor");
            vendedor.setPassword(passwordEncoder.encode(vendorPass));
            vendedor.setRole("VENDAS");
            userRepository.save(vendedor);
        }
    }
}