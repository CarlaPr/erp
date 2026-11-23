package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.TechnicianWorkOrderView;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class WorkOrderMaterialSuggestions {
    record Material(String name, String observation) {
        Material(String name) { this(name, null); }
    }

    private static final String COLORS = "preto|preta|branco|branca|bronze|fosco|fosca|natural";
    private static final Pattern KIT_COLOR = Pattern.compile(
            "\\b(?:kit|perfil|perfis|aluminio)(?:\\s+(?:box|sacada|de|em|aluminio|na|cor|com|pintura|acabamento))*\\s+("
                    + COLORS + ")\\b");
    private static final Pattern GLASS_COLOR = Pattern.compile("\\b(incolor|fume|extra\\s+clear)\\b"
            + "|\\b(?:vidros?|espelhos?)\\s+(?:(?:laminad[oa]|temperad[oa]|na|cor)\\s+){0,3}"
            + "(bronze|cinza|preto|branco|azul|verde|prata)\\b");
    private static final Pattern FIXING_DIMENSIONS = Pattern.compile(
            "\\b(?:parafusos?|buchas?|furos?|chumbadores?)\\s+(?:de\\s+)?\\d+(?:[.,]\\d+)?\\s*mm\\b");
    private static final Pattern SLIDING_MODEL = Pattern.compile(
            "\\b(?:porta|abertura|modelo|sistema)\\s+(?:de\\s+)?(?:correr|deslizante)\\b");
    private static final Pattern GLASS_TYPE = Pattern.compile("\\b(temperad[oa]\\s+laminad[oa]|laminad[oa]\\s+temperad[oa]"
            + "|temperad[oa]|laminad[oa]|aramad[oa]|canelad[oa]|jatead[oa]|serigrafad[oa])\\b");
    private static final Pattern THICKNESS = Pattern.compile(
            "(?<![\\d.,x])\\b(3|4|5|6|8|10|12|15|16|18|19|20|22|24)(?:[.,]0+)?\\s*mm\\b(?!\\s*[x×]\\s*\\d)");

    List<Material> forService(TechnicianWorkOrderView.ContractedService service) {
        String details = normalize(join(service.description(), service.fieldNotes()));
        Kind kind = identify(service.name(), service.category(), details);
        if (kind == null) return List.of();

        String specs = normalize(join(service.name(), service.description(), service.fieldNotes()));
        List<Material> materials = new ArrayList<>(kind.materials);
        boolean mirror = kind == Kind.MIRROR;
        String color = unique(GLASS_COLOR, specs, match -> {
            String colorMatch = match.group(1) != null ? match.group(1) : match.group(2);
            return "fume".equals(colorMatch) ? "fumê" : colorMatch;
        });
        String type = unique(GLASS_TYPE, specs, match -> match.group(1)
                .replaceAll("\\b(temperad|laminad|aramad|canelad|jatead|serigrafad)a\\b", "$1o"));
        String thickness = unique(THICKNESS, FIXING_DIMENSIONS.matcher(specs).replaceAll(""), match -> match.group(1) + "mm");
        String glass = join(mirror ? "Espelho" : "Vidro", color, mirror ? null : type, thickness);
        materials.add(1, new Material(glass.equals("Vidro") ? "Vidros" : glass));

        if (kind == Kind.BOX || kind == Kind.BALCONY) {
            String kitColor = unique(KIT_COLOR, specs, match -> switch (match.group(1)) {
                case "preta" -> "preto";
                case "branca" -> "branco";
                case "fosca" -> "fosco";
                default -> match.group(1);
            });
            if (kitColor != null) materials.set(0, new Material(materials.getFirst().name() + " " + kitColor));
        }
        return materials;
    }

    private static Kind identify(String title, String category, String details) {
        List<Kind> candidates = matches(normalize(title));
        if (candidates.isEmpty()) candidates = matches(normalize(join(category, title)));
        if (candidates.isEmpty()) {
            candidates = switch (normalize(category)) {
                case "sacada", "sacadas" -> List.of(Kind.BALCONY);
                case "cobertura", "coberturas" -> List.of(Kind.ROOF);
                default -> matches(details);
            };
        }
        if (candidates.size() != 1) return null;
        Kind kind = candidates.getFirst();
        if (kind == Kind.GLASS_DOOR && SLIDING_MODEL.matcher(details).find()) return Kind.SLIDING_DOOR;
        return kind;
    }

    private static List<Kind> matches(String text) {
        List<Kind> matches = new ArrayList<>(Arrays.stream(Kind.values())
                .filter(kind -> kind.keyword.matcher(text).find()).toList());
        if (matches.contains(Kind.SLIDING_DOOR) || matches.contains(Kind.BOX) || matches.contains(Kind.BALCONY)) {
            matches.remove(Kind.GLASS_DOOR);
        }
        return matches;
    }

    private static String unique(Pattern pattern, String text, Function<MatchResult, String> value) {
        List<String> matches = pattern.matcher(text).results().map(value).distinct().limit(2).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static String join(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private enum Kind {
        BOX("\\bbox(?:es)?\\b",
                new Material("Kit box"), new Material("Silicone"), new Material("Buchas e parafusos"),
                new Material("Ferragens e acessórios"), new Material("Ferramentas de instalação")),
        BALCONY("\\b(?:fechamento\\s+(?:de|da)\\s+sacada|envidracamento\\s+(?:de|da)\\s+sacada"
                + "|sacada\\s+(?:de\\s+vidro|reiki)|kit\\s+(?:de\\s+)?sacada|sistema\\s+reiki)\\b",
                new Material("Kit de sacada"), new Material("Fix All"), new Material("Escovinhas"),
                new Material("Borrachas e vedações"), new Material("Componentes de fixação"),
                new Material("Acessórios do sistema"), new Material("Ferramentas de instalação")),
        SLIDING_DOOR("\\bportas?\\b(?:\\s+(?:de|em)?\\s*(?:vidro|aluminio))?\\s+(?:de\\s+)?(?:correr|deslizante[s]?)\\b",
                new Material("Perfis"), new Material("Trilhos"), new Material("Roldanas"),
                new Material("Puxadores"), new Material("Guias"), new Material("Batentes e acessórios de fixação")),
        GLASS_DOOR("\\bportas?\\s+(?:(?:de|em)\\s+)?(?:abrir\\s+(?:de|em)\\s+)?(?:vidro|temperad[oa]s?|blindex)\\b",
                new Material("Perfis"), new Material("Puxador"), new Material("Fechadura"),
                new Material("Dobradiças ou pivôs", "Selecione conforme o modelo."),
                new Material("Buchas e parafusos"), new Material("Acessórios de instalação")),
        WINDOW("\\bjanelas?\\b",
                new Material("Perfis"), new Material("Trilhos", "Quando aplicável."),
                new Material("Roldanas ou acessórios de movimentação", "Quando aplicável."),
                new Material("Borrachas de vedação"), new Material("Silicone"),
                new Material("Parafusos e componentes de fixação")),
        MIRROR("\\bespelhos?\\b",
                new Material("Cola ou fita para espelho", "Confira a especificação."),
                new Material("Suportes ou acessórios de fixação"), new Material("Silicone", "Quando necessário."),
                new Material("Materiais de acabamento")),
        RAILING("\\bguarda[\\s-]?corpos?\\b",
                new Material("Perfis ou suportes para guarda-corpo"), new Material("Chumbadores e fixações"),
                new Material("Calços e juntas"), new Material("Tampas e acabamentos"),
                new Material("Corrimão", "Quando previsto no serviço.")),
        ROOF("\\b(?:cobertura\\s+(?:de|em)\\s+vidro|telhado\\s+(?:de|em)\\s+vidro|claraboia)\\b",
                new Material("Estrutura e perfis de apoio"), new Material("Gaxetas de apoio"),
                new Material("Vedação para cobertura"), new Material("Elementos de fixação"),
                new Material("Rufos e arremates", "Quando previstos no serviço.")),
        PARTITION("\\b(?:divisorias?|fechamento\\s+(?:de|para)\\s+salas?\\s+comercia(?:l|is))\\b",
                new Material("Perfis de divisória"), new Material("Conectores e arremates"),
                new Material("Calços e vedações"), new Material("Fixações"),
                new Material("Ferragens da passagem", "Quando houver porta."));

        private final Pattern keyword;
        private final List<Material> materials;

        Kind(String keyword, Material... materials) {
            this.keyword = Pattern.compile(keyword);
            this.materials = List.of(materials);
        }
    }
}
