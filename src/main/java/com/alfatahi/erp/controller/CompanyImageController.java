package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.service.CompanyImageService;
import com.alfatahi.erp.service.CompanyImageService.Kind;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Serve ao navegador a logo/assinatura da empresa lidas da pasta {@code images/} do projeto.
 *
 * <p>Fica sob {@code /quotes/**} de propósito: essa rota já exige perfil GESTAO ou VENDAS no
 * {@code SecurityConfig}, então as imagens herdam a mesma proteção sem nenhuma regra nova.
 */
@RestController
@RequestMapping("/quotes/company-image")
public class CompanyImageController {

    private final ProfileRepository profileRepo;
    private final CompanyImageService imageService;

    public CompanyImageController(ProfileRepository profileRepo, CompanyImageService imageService) {
        this.profileRepo = profileRepo;
        this.imageService = imageService;
    }

    @GetMapping("/{profileId}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable UUID profileId) {
        return serve(profileId, Kind.LOGO);
    }

    @GetMapping("/{profileId}/signature")
    public ResponseEntity<byte[]> signature(@PathVariable UUID profileId) {
        return serve(profileId, Kind.SIGNATURE);
    }

    private ResponseEntity<byte[]> serve(UUID profileId, Kind kind) {
        Optional<Profile> profile = profileRepo.findById(profileId);
        if (profile.isEmpty()) return ResponseEntity.notFound().build();

        Optional<byte[]> bytes = imageService.imageBytes(profile.get(), kind);
        Optional<String> mime = imageService.imageMimeType(profile.get(), kind);
        if (bytes.isEmpty() || mime.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime.get()))
                // Cache curto no navegador: o arquivo só muda com um novo deploy.
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(bytes.get());
    }
}
