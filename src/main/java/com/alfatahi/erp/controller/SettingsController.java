package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.entity.ServiceCategory;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AppUserRepository userRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    public SettingsController(ProfileRepository profileRepository, AppUserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String getSettings(Model model) {
        List<Profile> profiles = profileRepository.findAll();
        if (profiles.isEmpty()) {
            Profile p = new Profile();
            p.setCompanyName("Minha Vidraçaria ERP");
            p.setEmail("contato@empresa.com");
            profileRepository.save(p);
            profiles.add(p);
        }

        List<ServiceCategory> categories = serviceCategoryRepository.findAll()
                .stream().sorted(Comparator.comparing(ServiceCategory::getName)).collect(Collectors.toList());

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentPage", "settings");
        model.addAttribute("profiles", profiles);
        model.addAttribute("categories", categories);
        return "settings";
    }

    @PostMapping("/profile/delete/{id}")
    public String deleteProfile(@PathVariable UUID id) {
        profileRepository.deleteById(id);
        return "redirect:/settings?success";
    }

    @PostMapping("/users/save")
    public String saveUser(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return "redirect:/settings?error=userExists";
        }
        AppUser newUser = new AppUser();
        newUser.setId(UUID.randomUUID());
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);
        userRepository.saveAndFlush(newUser);
        return "redirect:/settings?success";
    }

    @PostMapping("/save")
    public String saveSettings(Profile profile) {
        if (profile.getTaxRate() == null) profile.setTaxRate(new BigDecimal("0.06"));
        profileRepository.save(profile);
        return "redirect:/settings?success";
    }
}
