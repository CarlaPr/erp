package com.alfatahi.erp.controller;

import com.alfatahi.erp.cutplan.entity.GlassCutRule;
import com.alfatahi.erp.cutplan.repository.GlassCutRuleRepository;
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

    @Autowired
    private GlassCutRuleRepository glassCutRuleRepository;

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

        // For each category, load its rules grouped
        Map<UUID, List<GlassCutRule>> rulesByCategory = new LinkedHashMap<>();
        for (ServiceCategory cat : categories) {
            List<GlassCutRule> rules = glassCutRuleRepository.findAllByServiceCategoryId(cat.getId());
            rulesByCategory.put(cat.getId(), rules);
        }

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentPage", "settings");
        model.addAttribute("profiles", profiles);
        model.addAttribute("categories", categories);
        model.addAttribute("rulesByCategory", rulesByCategory);
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

    // ──────────────────────────────────────────────────
    // GLASS CUT RULES API
    // ──────────────────────────────────────────────────

    @PostMapping("/cut-rules/save")
    @ResponseBody
    public ResponseEntity<?> saveCutRule(
            @RequestParam UUID categoryId,
            @RequestParam(required = false) UUID ruleId,
            @RequestParam String parameterName,
            @RequestParam String ruleType,
            @RequestParam BigDecimal value,
            @RequestParam String unit,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") Integer applicationOrder) {
        try {
            ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

            GlassCutRule rule;
            if (ruleId != null) {
                rule = glassCutRuleRepository.findById(ruleId)
                        .orElse(GlassCutRule.builder().build());
            } else {
                rule = GlassCutRule.builder().build();
            }

            rule.setServiceCategory(category);
            rule.setParameterName(parameterName);
            rule.setRuleType(ruleType);
            rule.setValue(value);
            rule.setUnit(unit);
            rule.setDescription(description);
            rule.setApplicationOrder(applicationOrder);
            rule.setActive(true);

            glassCutRuleRepository.save(rule);
            return ResponseEntity.ok(Map.of("success", true, "message", "Regra salva com sucesso"));
        } catch (Exception e) {
            log.error("Erro ao salvar regra: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cut-rules/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCutRule(@PathVariable UUID id) {
        try {
            glassCutRuleRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cut-rules/toggle/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleCutRule(@PathVariable UUID id) {
        try {
            GlassCutRule rule = glassCutRuleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Regra não encontrada"));
            rule.setActive(!rule.getActive());
            glassCutRuleRepository.save(rule);
            return ResponseEntity.ok(Map.of("success", true, "active", rule.getActive()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cut-rules/{categoryId}")
    @ResponseBody
    public ResponseEntity<?> getRulesByCategory(@PathVariable UUID categoryId) {
        List<GlassCutRule> rules = glassCutRuleRepository.findAllByServiceCategoryId(categoryId);
        return ResponseEntity.ok(rules);
    }
}
