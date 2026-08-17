package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.planocorte.entity.PlanoCorte;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    private final TemplateEngine templateEngine;
    private final CroquiService croquiService;
    private final ProfileRepository profileRepository;

    public PdfService(
            TemplateEngine templateEngine,
            CroquiService croquiService,
            ProfileRepository profileRepository) {

        this.templateEngine = templateEngine;
        this.croquiService = croquiService;
        this.profileRepository = profileRepository;
    }

    public byte[] gerarPdfPlanoCorte(
            PlanoCorte plano,
            List<PlanoCorteItem> itens,
            Map<TipoFuracao, Integer> resumoFuracoes) {

        Map<Long, String> croquis = new LinkedHashMap<>();

        for (PlanoCorteItem item : itens) {
            croquis.put(
                    item.getId(),
                    croquiService.gerarSvg(item)
            );
        }




        Map<Integer, List<PlanoCorteItem>> itensPorGrupo = new LinkedHashMap<>();
        for (PlanoCorteItem item : itens) {
            if (item.getGrupoVao() != null) {
                itensPorGrupo.computeIfAbsent(item.getGrupoVao(), grupo -> new ArrayList<>()).add(item);
            }
        }

        Map<Integer, String> croquisVao = new LinkedHashMap<>();
        Map<Integer, Long> primeiroItemIdDoGrupo = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<PlanoCorteItem>> entrada : itensPorGrupo.entrySet()) {
            List<PlanoCorteItem> folhas = entrada.getValue();
            primeiroItemIdDoGrupo.put(entrada.getKey(), folhas.get(0).getId());



            if (folhas.size() > 1) {
                croquisVao.put(entrada.getKey(), croquiService.gerarSvgVao(folhas));
            }
        }

        Profile profile = buscarProfileTahiGlass();

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

        String logoBase64 = toBase64Uri(
                profile.getLogoUrl()
        );


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
            empresa = nvlStr(buscarProfileTahiGlass().getCompanyName(), "Plano de Corte");
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

    private Profile buscarProfileTahiGlass() {

        return profileRepository.findAll()
                .stream()
                .filter(profile ->
                        profile.getCompanyName() != null
                                && profile.getCompanyName()
                                .toUpperCase()
                                .contains("TAHI GLASS")
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Company Profile da TAHI GLASS não encontrado."
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


    private String toBase64Uri(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        if (url.startsWith("data:")) {
            return url;
        }

        HttpURLConnection connection = null;

        try {

            connection =
                    (HttpURLConnection)
                            new URL(url).openConnection();

            connection.setConnectTimeout(6000);
            connection.setReadTimeout(12000);

            connection.setRequestProperty(
                    "User-Agent",
                    "ERP-PDF-Generator/1.0"
            );

            byte[] bytes =
                    connection
                            .getInputStream()
                            .readAllBytes();

            String mime =
                    connection.getContentType();

            if (mime == null) {

                mime = url
                        .toLowerCase()
                        .endsWith(".png")
                        ? "image/png"
                        : "image/jpeg";
            }

            mime = mime
                    .split(";")[0]
                    .trim();

            return "data:"
                    + mime
                    + ";base64,"
                    + Base64
                    .getEncoder()
                    .encodeToString(bytes);

        } catch (Exception e) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}