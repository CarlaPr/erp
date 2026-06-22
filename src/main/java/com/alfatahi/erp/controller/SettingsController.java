package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.repository.ProfileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final ProfileRepository profileRepository;

    public SettingsController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public String getSettings(Model model) {
        // Auto-setup: garante a existência de um registo de perfil de configuração
        Profile profile = profileRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setCompanyName("Minha Vidraçaria ERP");
                    p.setEmail("contacto@empresa.com");
                    return profileRepository.save(p);
                });

        model.addAttribute("currentPage", "settings");
        model.addAttribute("profile", profile);
        return "settings";
    }

    @PostMapping("/save")
    public String saveSettings(@ModelAttribute("profile") Profile profile) {
        profileRepository.save(profile);
        return "redirect:/settings?success";
    }
}