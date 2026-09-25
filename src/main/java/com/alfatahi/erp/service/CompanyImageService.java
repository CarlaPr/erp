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

/**
 * Resolve a logo e a assinatura de cada empresa (perfil) a partir de arquivos que ficam
 * dentro do próprio projeto, na pasta {@code src/main/resources/images}, em vez de buscá-las
 * na Internet.
 *
 * <p>A associação é feita pelo nome do arquivo: o perfil "Tahi Glass" usa os arquivos cujo nome
 * contém {@code tahiglass}; "One Glass" usa {@code oneglass}; "Ru Glass" usa {@code ruglass}.
 * Dentro de cada empresa, o arquivo que contém {@code logo} é a logo e o que contém
 * {@code assinatura} (ou {@code signature}) é a assinatura. Exemplos válidos:
 * <pre>
 *   images/logo-tahiglass.png        images/assinatura-tahiglass.png
 *   images/oneglass-logo.png         images/oneglass-assinatura.png
 *   images/logo_ruglass.jpg          images/ruglass_assinatura.jpg
 * </pre>
 *
 * <p>Nomes de empresa são comparados sem acento, sem espaços e sem pontuação, então "Tahi Glass",
 * "TAHI GLASS", "TahiGlass" e "Tahi-Glass Ltda" resolvem todos para {@code tahiglass}.
 */
@Service
public class CompanyImageService {

    private static final Logger log = LoggerFactory.getLogger(CompanyImageService.class);

    /** Chaves das empresas suportadas; o nome do arquivo precisa conter uma delas. */
    static final List<String> COMPANY_KEYS = List.of("tahiglass", "oneglass", "ruglass");

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

    /** Cache do data URI já montado, por nome de arquivo (evita reler/re-codificar a cada PDF). */
    private final Map<String, String> dataUriCache = new ConcurrentHashMap<>();

    /** Lista de imagens da pasta; carregada sob demanda e mantida em memória. */
    private volatile List<ImageFile> imageFiles;

    // ------------------------------------------------------------------ API pública

    /** Data URI (base64) da logo da empresa do perfil, ou {@code null} se não houver arquivo. */
    public String logoDataUri(Profile profile) {
        return dataUriFor(profile, Kind.LOGO);
    }

    /** Data URI (base64) da assinatura da empresa do perfil, ou {@code null} se não houver arquivo. */
    public String signatureDataUri(Profile profile) {
        return dataUriFor(profile, Kind.SIGNATURE);
    }

    /** Indica se existe um arquivo de logo/assinatura em {@code images/} para a empresa do perfil. */
    public boolean hasImage(Profile profile, Kind kind) {
        return findImage(profile, kind).isPresent();
    }

    public Optional<byte[]> imageBytes(Profile profile, Kind kind) {
        return findImage(profile, kind).flatMap(this::readBytes);
    }

    public Optional<String> imageMimeType(Profile profile, Kind kind) {
        return findImage(profile, kind).map(f -> mimeFor(f.fileName()));
    }

    /**
     * Chave da empresa ({@code tahiglass}, {@code oneglass} ou {@code ruglass}) para o perfil
     * informado, ou vazio se o nome da empresa não corresponder a nenhuma conhecida.
     */
    public Optional<String> companyKey(Profile profile) {
        if (profile == null) return Optional.empty();
        return companyKeyFromName(profile.getCompanyName());
    }

    // ------------------------------------------------------------------ resolução

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
            // Só avisa "arquivo não encontrado" quando a empresa é conhecida; o caso de empresa
            // desconhecida já foi registrado em findImage.
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
            // Ordem estável: se houver mais de um candidato, o primeiro em ordem alfabética vence.
            found.sort((a, b) -> a.fileName().compareToIgnoreCase(b.fileName()));
            log.info("Imagens de empresa disponíveis em 'images/': {}",
                    found.stream().map(ImageFile::fileName).toList());
            imageFiles = List.copyOf(found);
            return imageFiles;
        }
    }

    // ------------------------------------------------------------------ utilidades

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

    /** minúsculas, sem acento e apenas letras/dígitos: "Tahi-Glass Ltda" -> "tahiglassltda". */
    static String normalize(String text) {
        if (text == null) return "";
        String semAcento = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
