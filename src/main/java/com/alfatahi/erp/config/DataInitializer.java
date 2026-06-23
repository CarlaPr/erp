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
        // Criar usuário administrativo padrão (GESTAO)
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser gestor = new AppUser();
            gestor.setUsername("admin");
            gestor.setPassword(passwordEncoder.encode("admin123"));
            gestor.setRole("GESTAO");
            userRepository.save(gestor);
        }

        // Criar usuário de vendas padrão (VENDAS)
        if (userRepository.findByUsername("vendedor").isEmpty()) {
            AppUser vendedor = new AppUser();
            vendedor.setUsername("vendedor");
            vendedor.setPassword(passwordEncoder.encode("venda123"));
            vendedor.setRole("VENDAS");
            userRepository.save(vendedor);
        }
    }
}