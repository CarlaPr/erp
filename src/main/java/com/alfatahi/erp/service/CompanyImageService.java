package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CompanyImageService {

    private static final Logger log = LoggerFactory.getLogger(CompanyImageService.class);
    static final List<String> COMPANY_KEYS = List.of("grupoglass", "tahiglass", "oneglass", "ruglass");

    private static final String IMAGES_LOCATION = "classpath*:images/*";
    private static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif", "svg");

    public enum Kind {
        LOGO(List.of("logo")),
        SIGNATURE(List.of("assinatura", "signature"));

        private final List<String> markers;

        Kind(List<String> markers) {
            this.markers = markers;
        }

        boolean matches(String normalizedFileName) {
            return markers.stream().anyMatch(normalizedFileName::contains);
        }
    }

    private record ImageFile(String fileName, String normalizedName, Resource resource) { }

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final Map<String, String> dataUriCache = new ConcurrentHashMap<>();

    private volatile List<ImageFile> imageFiles;

    public String logoDataUri(Profile profile) {
        return dataUriFor(profile, Kind.LOGO);
    }

    public String signatureDataUri(Profile profile) {
        return dataUriFor(profile, Kind.SIGNATURE);
    }

   public boolean hasImage(Profile profile, Kind kind) {
        return findImage(profile, kind).isPresent();
    }

    public Optional<byte[]> imageBytes(Profile profile, Kind kind) {
        return findImage(profile, kind).flatMap(this::readBytes);
    }

    public Optional<String> imageMimeType(Profile profile, Kind kind) {
        return findImage(profile, kind).map(f -> mimeFor(f.fileName()));
    }

    public Optional<String> companyKey(Profile profile) {
        if (profile == null) return Optional.empty();
        return companyKeyFromName(profile.getCompanyName());
    }
    static Optional<String> companyKeyFromName(String companyName) {
        String normalized = normalize(companyName);
        if (normalized.isEmpty()) return Optional.empty();
        return COMPANY_KEYS.stream().filter(normalized::contains).findFirst();
    }

    private Optional<ImageFile> findImage(Profile profile, Kind kind) {
        Optional<String> key = companyKey(profile);
        if (key.isEmpty()) {
            if (profile != null) {
                log.warn("Perfil '{}' não corresponde a nenhuma empresa conhecida {}; sem {}.",
                        profile.getCompanyName(), COMPANY_KEYS, kind);
            }
            return Optional.empty();
        }
        return loadImageFiles().stream()
                .filter(f -> f.normalizedName().contains(key.get()))
                .filter(f -> kind.matches(f.normalizedName()))
                .findFirst();
    }

    private String dataUriFor(Profile profile, Kind kind) {
        Optional<ImageFile> image = findImage(profile, kind);
        if (image.isEmpty()) {
           if (companyKey(profile).isPresent()) {
                log.warn("Nenhum arquivo de {} encontrado em 'images/' para a empresa '{}'.",
                        kind, profile.getCompanyName());
            }
            return null;
        }
        ImageFile file = image.get();
        String cached = dataUriCache.get(file.fileName());
        if (cached != null) return cached;

        Optional<byte[]> bytes = readBytes(file);
        if (bytes.isEmpty()) return null;

        String uri = "data:" + mimeFor(file.fileName()) + ";base64,"
                + Base64.getEncoder().encodeToString(bytes.get());
        dataUriCache.put(file.fileName(), uri);
        return uri;
    }

    private Optional<byte[]> readBytes(ImageFile file) {
        try (InputStream in = file.resource().getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return bytes.length == 0 ? Optional.empty() : Optional.of(bytes);
        } catch (IOException e) {
            log.warn("Falha ao ler a imagem '{}': {}", file.fileName(), e.getMessage());
            return Optional.empty();
        }
    }

    private List<ImageFile> loadImageFiles() {
        List<ImageFile> current = imageFiles;
        if (current != null) return current;
        synchronized (this) {
            if (imageFiles != null) return imageFiles;
            List<ImageFile> found = new ArrayList<>();
            try {
                for (Resource resource : resolver.getResources(IMAGES_LOCATION)) {
                    String name = resource.getFilename();
                    if (name == null || !hasImageExtension(name)) continue;
                    found.add(new ImageFile(name, normalize(name), resource));
                }
            } catch (IOException e) {
                log.warn("Não foi possível listar a pasta 'images/': {}", e.getMessage());
            }
           found.sort((a, b) -> a.fileName().compareToIgnoreCase(b.fileName()));
            log.info("Imagens de empresa disponíveis em 'images/': {}",
                    found.stream().map(ImageFile::fileName).toList());
            imageFiles = List.copyOf(found);
            return imageFiles;
        }
    }
    private static boolean hasImageExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().anyMatch(ext -> lower.endsWith("." + ext));
    }

    private static String mimeFor(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }
    static String normalize(String text) {
        if (text == null) return "";
        String semAcento = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
