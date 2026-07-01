package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.security.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return new CustomUserDetails(
                appUser.getUsername(),
                appUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole())),
                appUser.getRole()
        );
    }
}
