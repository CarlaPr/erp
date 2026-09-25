package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.service.CompanyImageService;
import com.alfatahi.erp.planocorte.dto.CroquiVaoChunkDto;
import com.alfatahi.erp.planocorte.entity.PlanoCorte;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PdfService {

     private static final int MAX_FOLHAS_POR_CROQUI_PDF = 3;

    private final TemplateEngine templateEngine;
    private final CroquiService croquiService;
    private final ProfileRepository profileRepository;
    private final CompanyImageService companyImageService;

    public PdfService(
            TemplateEngine templateEngine,
            CroquiService croquiService,
            ProfileRepository profileRepository,
            CompanyImageService companyImageService) {

        this.templateEngine = templateEngine;
        this.croquiService = croquiService;
        this.profileRepository = profileRepository;
        this.companyImageService = companyImageService;
    }

    public byte[] gerarPdfPlanoCorte(
            PlanoCorte plano,
            List<PlanoCorteItem> itens,
            Map<TipoFuracao, Integer> resumoFuracoes) {

        Map<Long, String> croquis = new LinkedHashMap<>();

        for (PlanoCorteItem item : itens) {
            croquis.put(
                    item.getId(),
                    prepararSvgParaPdf(croquiService.gerarSvg(item))
            );
        }




        Map<Integer, List<PlanoCorteItem>> itensPorGrupo = new LinkedHashMap<>();
        for (PlanoCorteItem item : itens) {
            if (item.getGrupoVao() != null) {
                itensPorGrupo.computeIfAbsent(item.getGrupoVao(), grupo -> new ArrayList<>()).add(item);
            }
        }

        Map<Integer, List<CroquiVaoChunkDto>> croquisVao = new LinkedHashMap<>();
        Map<Integer, Long> primeiroItemIdDoGrupo = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<PlanoCorteItem>> entrada : itensPorGrupo.entrySet()) {
            List<PlanoCorteItem> folhas = entrada.getValue();
            primeiroItemIdDoGrupo.put(entrada.getKey(), folhas.get(0).getId());



            if (folhas.size() > 1) {
                List<CroquiVaoChunkDto> chunksPdf = croquiService
                        .gerarSvgsVaoAgrupados(folhas, MAX_FOLHAS_POR_CROQUI_PDF)
                        .stream()
                        .map(chunk -> new CroquiVaoChunkDto(
                                prepararSvgParaPdf(chunk.getSvg()),
                                chunk.getFolhaInicio(),
                                chunk.getFolhaFim(),
                                chunk.getTotalFolhas()))
                        .toList();
                croquisVao.put(entrada.getKey(), chunksPdf);
            }
        }

        Profile profile = resolverProfile(plano);

        String companyName = nvlStr(
                profile.getCompanyName(),
                "TAHI GLASS"
        );

        String companyDoc = nvlStr(
                profile.getDocument(),
                "--"
        );

        String companyAddress = nvlStr(
                profile.getAddress(),
                ""
        );

        String companyEmail = nvlStr(
                profile.getEmail(),
                ""
        );

        String companyPhone = nvlStr(
                profile.getPhone(),
                ""
        );

        String logoBase64 = companyImageService.logoDataUri(profile);


        Context context = new Context();

        context.setVariable(
                "plano",
                plano
        );

        context.setVariable(
                "itens",
                itens
        );

        context.setVariable(
                "croquis",
                croquis
        );

        context.setVariable(
                "croquisVao",
                croquisVao
        );

        context.setVariable(
                "itensPorGrupo",
                itensPorGrupo
        );

        context.setVariable(
                "primeiroItemIdDoGrupo",
                primeiroItemIdDoGrupo
        );

        context.setVariable(
                "resumoFuracoes",
                resumoFuracoes
        );

        context.setVariable(
                "dataEmissao",
                LocalDateTime.now()
        );

        context.setVariable(
                "companyName",
                companyName
        );

        context.setVariable(
                "companyDoc",
                companyDoc
        );

        context.setVariable(
                "companyAddress",
                companyAddress
        );

        context.setVariable(
                "companyEmail",
                companyEmail
        );

        context.setVariable(
                "companyPhone",
                companyPhone
        );

        context.setVariable(
                "logoBase64",
                logoBase64
        );


        String html = templateEngine.process(
                "pdf/plano-corte-pdf",
                context
        );


        try {

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.useSVGDrawer(
                    new BatikSVGDrawer()
            );

            builder.withHtmlContent(
                    html,
                    null
            );

            builder.toStream(out);

            builder.run();

            return out.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Falha ao gerar PDF do plano de corte "
                            + plano.getId(),
                    e
            );
        }
    }


    public String nomeArquivoPdf(PlanoCorte plano) {
        String empresa;
        try {
            empresa = nvlStr(resolverProfile(plano).getCompanyName(), "Plano de Corte");
        } catch (RuntimeException e) {
            empresa = "Plano de Corte";
        }
        String base = empresa + " - " + plano.getNumeroFormatado();
        return sanitizarNomeArquivo(base) + ".pdf";
    }

    private String sanitizarNomeArquivo(String texto) {
        String semAcento = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpo = semAcento.replaceAll("[^a-zA-Z0-9 _-]", "").trim();
        limpo = limpo.replaceAll("\\s+", "_");
        return limpo.isEmpty() ? "plano-de-corte" : limpo;
    }

    private String prepararSvgParaPdf(String svg) {
        if (svg == null || svg.isBlank()) {
            return svg;
        }

        String preparado = svg
                .replaceAll(
                        "(?i)(<text\\b[^>]*?)\\s+fill=\"[^\"]+\"",
                        "$1 fill=\"#000000\"")
                .replaceAll(
                        "(?i)(<text\\b[^>]*?)\\s+stroke=\"[^\"]+\"",
                        "$1 stroke=\"none\"")
                .replaceAll(
                        "(?i)(<(?:line|path|circle|rect|polygon|polyline|ellipse)\\b[^>]*?)\\s+stroke=\"[^\"]+\"",
                        "$1 stroke=\"#000000\"");

        return preparado.replaceAll(
                "(?i)(<marker\\b[^>]*>\\s*<path\\b[^>]*?)\\s+fill=\"[^\"]+\"",
                "$1 fill=\"#000000\"");
    }

    private Profile resolverProfile(PlanoCorte plano) {
        Optional<Profile> doOrcamento = Optional.ofNullable(plano)
                .map(PlanoCorte::getWorkOrder)
                .map(os -> os.getQuote())
                .map(q -> q.getProfile());
        if (doOrcamento.isPresent()) {
            return doOrcamento.get();
        }
        return buscarProfilePadrao();
    }

    private Profile buscarProfilePadrao() {

        return profileRepository.findAll()
                .stream()
                .filter(profile ->
                        profile.getCompanyName() != null
                                && companyImageService.companyKey(profile)
                                .filter(key -> "grupoglass".equals(key) || "tahiglass".equals(key))
                                .isPresent()
                )
                .sorted(java.util.Comparator.comparingInt(profile ->
                        companyImageService.companyKey(profile).filter("grupoglass"::equals).isPresent() ? 0 : 1))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Perfil empresarial do GRUPO GLASS não encontrado."
                        )
                );
    }


    private String nvlStr(
            String value,
            String defaultValue) {

        return value != null && !value.isBlank()
                ? value
                : defaultValue;
    }
}
