package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.entity.ServiceCategory;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import com.alfatahi.erp.security.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final LoginAttemptService loginAttemptService;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    public SettingsController(ProfileRepository profileRepository, AppUserRepository userRepository,
                              LoginAttemptService loginAttemptService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
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

        List<AppUser> users = userRepository.findAll();
        Set<UUID> lockedUserIds = users.stream()
                .filter(user -> loginAttemptService.isLocked(user.getUsername()))
                .map(AppUser::getId)
                .collect(Collectors.toSet());
        model.addAttribute("users", users);
        model.addAttribute("lockedUserIds", lockedUserIds);
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

        if (!"GESTAO".equals(role) && !"VENDAS".equals(role) && !"TECNICO".equals(role)) {
            return "redirect:/settings?error=invalidRole";
        }
        if (password == null || password.length() < 10) {
            return "redirect:/settings?error=weakPassword";
        }
        AppUser newUser = new AppUser();
        newUser.setId(UUID.randomUUID());
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);
        userRepository.saveAndFlush(newUser);
        return "redirect:/settings?success";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Optional<AppUser> user = userRepository.findById(id);
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("userUnlockError", "Usuário não encontrado. Atualize a lista e tente novamente.");
        } else {
            loginAttemptService.unlock(user.get().getUsername());
            redirectAttributes.addFlashAttribute("userUnlockSuccess",
                    "Acesso de " + user.get().getUsername() + " desbloqueado. O usuário pode tentar entrar novamente.");
        }
        return "redirect:/settings#registeredUsers";
    }

    @PostMapping("/save")
    public String saveSettings(Profile profile) {
        if (profile.getTaxRate() == null) profile.setTaxRate(new BigDecimal("0.06"));

        if (profile.getId() != null) {
            profileRepository.findById(profile.getId()).ifPresent(existing -> {
                profile.setLogoUrl(existing.getLogoUrl());
                profile.setSignatureUrl(existing.getSignatureUrl());
            });
        }
        profileRepository.save(profile);
        return "redirect:/settings?success";
    }
}
