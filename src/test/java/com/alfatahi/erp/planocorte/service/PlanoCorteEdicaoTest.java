package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.calculo.*;
import com.alfatahi.erp.planocorte.dto.PlanoCorteVaoForm;
import com.alfatahi.erp.planocorte.entity.*;
import com.alfatahi.erp.planocorte.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlanoCorteEdicaoTest {
    PlanoCorteRepository planos = mock(PlanoCorteRepository.class);
    PlanoCorteItemRepository itens = mock(PlanoCorteItemRepository.class);
    VidroRepository vidros = mock(VidroRepository.class);
    List<PlanoCorteItem> banco = new ArrayList<>();
    PlanoCorte plano = new PlanoCorte();
    PlanoCorteService service;

    @BeforeEach void setup() {
        plano.setId(1L);
        plano.setCategoria(CategoriaServico.PORTA_CORRER);
        plano.setStatus(StatusPlano.RASCUNHO);
        when(planos.findById(1L)).thenReturn(Optional.of(plano));
        when(itens.findByPlanoCorteIdOrderByIdAsc(1L)).thenAnswer(i -> new ArrayList<>(banco));
        when(itens.findByIdAndPlanoCorteId(anyLong(), eq(1L))).thenAnswer(i ->
                banco.stream().filter(f -> f.getId().equals(i.getArgument(0))).findFirst());
        when(itens.save(any())).thenAnswer(i -> {
            PlanoCorteItem item = i.getArgument(0);
            if (item.getId() == null) { item.setId((long) banco.size() + 1); banco.add(item); }
            return item;
        });
        doAnswer(i -> { ((Iterable<PlanoCorteItem>) i.getArgument(0)).forEach(banco::remove); return null; })
                .when(itens).deleteAll(any());
        for (long id = 1; id <= 2; id++) {
            Vidro v = new Vidro();
            v.setId(id); v.setNome("Vidro " + id); v.setTipo(TipoVidro.TEMPERADO);
            v.setValorPorM2(BigDecimal.valueOf(id * 100)); v.setEspessura(BigDecimal.TEN);
            when(vidros.findById(id)).thenReturn(Optional.of(v));
        }
        ParametroServicoService parametros = mock(ParametroServicoService.class);
        when(parametros.valor(any(), anyString(), nullable(BigDecimal.class))).thenAnswer(i -> i.getArgument(2));
        service = new PlanoCorteService(planos, itens, null, vidros, null, null,
                new ServicoCalculadoraRegistry(List.of(new PortaCorrerCalculadora(parametros), new SacadaCalculadora(parametros), new BoxBanheiroCalculadora(parametros))), null, null);
    }

    PlanoCorteVaoForm form() {
        PlanoCorteVaoForm f = new PlanoCorteVaoForm();
        f.setCategoria(CategoriaServico.PORTA_CORRER); f.setVidroId(1L); f.setTipoBorda(TipoBorda.LISO);
        f.setLarguraVaoMm(new BigDecimal("2000")); f.setAlturaVaoMm(new BigDecimal("2200"));
        f.setQuantidadeFolhasFixas(1); f.setQuantidadeFolhasMoveis(1); f.setObservacoes("Observação do cliente");
        return f;
    }

    @Test void editaVidroAcabamentoDimensoesEFolhasPreservandoDetalhes() {
        service.adicionarVao(1L, form());
        PlanoCorteItem movel = banco.get(1);
        service.adicionarPuxadorH(1L, movel.getId(), "DIREITO", new BigDecimal("400"), new BigDecimal("100"), new BigDecimal("12.5"));
        PlanoCorteVaoForm novo = service.formularioVao(banco);
        novo.setVidroId(2L); novo.setTipoBorda(TipoBorda.LAPIDADO);
        novo.setLarguraVaoMm(new BigDecimal("3000")); novo.setQuantidadeFolhasFixas(2);
        service.editarVao(1L, 1, novo);
        assertEquals(3, banco.size());
        assertSame(movel, banco.get(1));
        assertEquals(2L, movel.getVidroId());
        assertEquals(TipoBorda.LAPIDADO, movel.getTipoBorda());
        assertEquals(0, movel.getLarguraFinalMm().compareTo(new BigDecimal("1050")));
        assertEquals(2, movel.getFuracoes().stream().filter(f -> f.getTipo() == TipoFuracao.PUXADOR).count());
        assertEquals(2, movel.getFuracoes().stream().filter(f -> f.getTipo() == TipoFuracao.ROLDANA).count());
        assertEquals("Observação do cliente", service.formularioVao(banco).getObservacoes());
        novo.setQuantidadeFolhasFixas(0);
        service.editarVao(1L, 1, novo);
        assertEquals(List.of(movel), banco);
        String croqui = new CroquiService().gerarSvg(movel);
        assertTrue(croqui.contains("100 mm"));
        assertTrue(croqui.contains("400 mm"));
        assertEquals(0, movel.getLarguraFinalMm().compareTo(new BigDecimal("3050")));
    }

    @Test void croquiExibeTodasAsMedidasDoPuxador() {
        service.adicionarVao(1L, form());
        PlanoCorteItem movel = banco.get(1);
        service.adicionarPuxadorH(1L, movel.getId(), "DIREITO", new BigDecimal("400"), new BigDecimal("100"), new BigDecimal("12.5"));
        String svg = new CroquiService().gerarSvg(movel);
        assertTrue(svg.contains("400 mm"), svg);
        assertTrue(svg.contains("100 mm"), svg);
        assertTrue(svg.contains("12,5") || svg.contains("12.5"), svg);
        assertTrue(svg.contains("PUXADOR H"));
    }

    @Test void formularioRenderizaCamposDoVaoExistente() throws Exception {
        service.adicionarVao(1L, form());
        String pagina = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/planocorte/fragments/edicao-vao.html"));
        int inicio = pagina.indexOf("<form th:action=\"@{/cut-plans/{id}/vaos/{grupoVao}/dimensoes");
        String fragmento = pagina.substring(inicio, pagina.indexOf("</form>", inicio) + 7)
                .replaceAll("th:action=\"[^\"]*\"", "");
        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("item", banco.get(0));
        context.setVariable("formulariosVao", Map.of(1, service.formularioVao(banco)));
        context.setVariable("itensPorGrupo", Map.of(1, banco));
        context.setVariable("categorias", CategoriaServico.values());
        context.setVariable("tiposBorda", TipoBorda.values());
        context.setVariable("vidros", List.of(vidros.findById(1L).orElseThrow(), vidros.findById(2L).orElseThrow()));
        String html = engine.process(fragmento, context);
        assertTrue(html.contains("name=\"vidroId\""));
        assertTrue(html.contains("name=\"quantidadeFolhasFixas\""));
        assertTrue(html.contains("Observação do cliente"));
        assertFalse(html.contains("th:value"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/edicao-vao-preview.html"), html);
    }

    @Test void alternaDimensoesPorLadoERetangulares() {
        service.adicionarVao(1L, form());
        PlanoCorteVaoForm edicao = service.formularioVao(banco);
        edicao.setDimensoesPersonalizadas(true);
        edicao.setAlturaBrutaEsquerdaMm(new BigDecimal("2200"));
        edicao.setAlturaBrutaDireitaMm(new BigDecimal("2100"));
        edicao.setLarguraBrutaSuperiorMm(new BigDecimal("2000"));
        edicao.setLarguraBrutaInferiorMm(new BigDecimal("1900"));
        service.editarVao(1L, 1, edicao);
        assertTrue(banco.get(0).isDimensoesPersonalizadas());
        edicao.setDimensoesPersonalizadas(false);
        service.editarVao(1L, 1, edicao);
        assertFalse(banco.get(0).isDimensoesPersonalizadas());
        assertNull(banco.get(0).getAlturaFinalEsquerdaMm());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = CategoriaServico.class,
            names = {"PORTA_CORRER", "SACADA", "BOX_BANHEIRO_PADRAO"})
    void editaFolhaEDepoisVaoAgrupado(CategoriaServico categoria) {
        PlanoCorteVaoForm form = form();
        form.setCategoria(categoria);
        form.setQuantidadeFolhas(3);
        service.adicionarVao(1L, form);
        PlanoCorteItem folha = banco.get(0);
        PlanoCorteItem vizinha = banco.get(1);
        BigDecimal larguraVizinha = vizinha.getLarguraFinalMm();
        BigDecimal alturaVizinha = vizinha.getAlturaFinalMm();
        BigDecimal valorAnterior = plano.getValorTotalPlano();
        service.editarDimensoesFolha(1L, folha.getId(), new BigDecimal("777"), new BigDecimal("1999"));
        assertEquals(0, folha.getLarguraFinalMm().compareTo(new BigDecimal("777")));
        assertEquals(0, folha.getAlturaFinalMm().compareTo(new BigDecimal("1999")));
        assertEquals(larguraVizinha, vizinha.getLarguraFinalMm());
        assertEquals(alturaVizinha, vizinha.getAlturaFinalMm());
        assertEquals(0, folha.getLarguraBrutaMm().compareTo(new BigDecimal("2000")));
        assertNotEquals(valorAnterior, plano.getValorTotalPlano());
        String svg = new CroquiService().gerarSvgVao(banco);
        assertTrue(svg.contains("777 mm"), svg);
        assertTrue(svg.contains("1999 mm"), svg);
        PlanoCorteVaoForm total = service.formularioVao(banco);
        total.setLarguraVaoMm(new BigDecimal("3000"));
        total.setAlturaVaoMm(new BigDecimal("2500"));
        service.editarVao(1L, 1, total);
        assertNotEquals(0, folha.getLarguraFinalMm().compareTo(new BigDecimal("777")));
        for (PlanoCorteItem f : banco) {
            assertEquals(0, f.getLarguraBrutaMm().compareTo(new BigDecimal("3000")));
            assertEquals(0, f.getAlturaBrutaMm().compareTo(new BigDecimal("2500")));
        }
    }

    @Test void editarFolhaPreservaPuxadorENaoDuplicaRoldanasAoRecalcularVao() {
        service.adicionarVao(1L, form());
        PlanoCorteItem folha = banco.get(1);
        service.adicionarPuxadorH(1L, folha.getId(), "DIREITO", new BigDecimal("400"), new BigDecimal("100"), new BigDecimal("12"));
        service.editarDimensoesFolha(1L, folha.getId(), new BigDecimal("800"), new BigDecimal("2000"));
        String svg = new CroquiService().gerarSvgVao(banco);
        assertTrue(svg.contains("400 mm"));
        assertTrue(svg.contains("100 mm"));
        service.editarVao(1L, 1, service.formularioVao(banco));
        assertEquals(2, folha.getFuracoes().stream().filter(f -> f.getTipo() == TipoFuracao.ROLDANA).count());
        assertEquals(2, folha.getFuracoes().stream().filter(f -> f.getTipo() == TipoFuracao.PUXADOR).count());
    }

    @Test void rejeitaMedidasInvalidasFolhaExternaEPlanoFinalizado() {
        service.adicionarVao(1L, form());
        PlanoCorteItem folha = banco.get(0);
        BigDecimal original = folha.getLarguraFinalMm();
        assertThrows(IllegalStateException.class, () -> service.editarDimensoesFolha(1L, folha.getId(), BigDecimal.ZERO, BigDecimal.TEN));
        assertThrows(NoSuchElementException.class, () -> service.editarDimensoesFolha(1L, 999L, BigDecimal.TEN, BigDecimal.TEN));
        assertEquals(original, folha.getLarguraFinalMm());
        plano.setStatus(StatusPlano.FINALIZADO);
        assertThrows(IllegalStateException.class, () -> service.editarDimensoesFolha(1L, folha.getId(), BigDecimal.TEN, BigDecimal.TEN));
    }

    @Test void formularioFolhaRenderizaMedidasAtuais() throws Exception {
        service.adicionarVao(1L, form());
        String fragmento = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/templates/planocorte/fragments/edicao-folha.html"))
                .replaceAll("th:action=\"[^\"]*\"", "");
        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("folha", banco.get(0));
        context.setVariable("numero", 1);
        String html = engine.process(fragmento, context);
        assertTrue(html.contains("name=\"larguraMm\""));
        assertTrue(html.contains("name=\"alturaMm\""));
        assertTrue(html.contains("Salvar folha 1"));
        assertFalse(html.contains("th:value"));
    }
    @Test void painelAgrupadoIncluiFormularioTotalEUmPorFolha() throws Exception {
        service.adicionarVao(1L, form());
        String pagina = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/planocorte/detalhe.html"));
        int inicio = pagina.indexOf("<div th:if=\"${item.grupoVao != null and croquisVao.get(item.grupoVao) != null");
        String painel = pagina.substring(inicio, pagina.indexOf("<article", inicio));
        org.thymeleaf.templateresolver.ITemplateResolver resolver = mock(org.thymeleaf.templateresolver.ITemplateResolver.class);
        when(resolver.getName()).thenReturn("teste");
        when(resolver.resolveTemplate(any(), nullable(String.class), anyString(), nullable(Map.class))).thenAnswer(i -> {
            String nome = i.getArgument(2);
            String conteudo = nome.equals("painel") ? painel : java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/" + nome + ".html"));
            return new org.thymeleaf.templateresolver.TemplateResolution(
                    new org.thymeleaf.templateresource.StringTemplateResource(conteudo),
                    org.thymeleaf.templatemode.TemplateMode.HTML,
                    org.thymeleaf.cache.NonCacheableCacheEntryValidity.INSTANCE);
        });
        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setLinkBuilder(new org.thymeleaf.linkbuilder.StandardLinkBuilder() {
            @Override protected String computeContextPath(org.thymeleaf.context.IExpressionContext context, String base, Map<String,Object> parameters) {
                return "";
            }
        });
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("item", banco.get(0));
        context.setVariable("plano", plano);
        context.setVariable("formulariosVao", Map.of(1, service.formularioVao(banco)));
        context.setVariable("itensPorGrupo", Map.of(1, banco));
        context.setVariable("primeiroItemIdDoGrupo", Map.of(1, banco.get(0).getId()));
        context.setVariable("croquisVao", Map.of(1, new CroquiService().gerarSvgsVaoAgrupados(banco, 6)));
        context.setVariable("categorias", CategoriaServico.values());
        context.setVariable("tiposBorda", TipoBorda.values());
        context.setVariable("vidros", List.of(vidros.findById(1L).orElseThrow()));
        String html = engine.process("painel", context);
        assertEquals(3, html.split("</form>").length - 1);
        assertTrue(html.contains("/cut-plans/1/vaos/1/dimensoes"));
        assertTrue(html.contains("/cut-plans/1/itens/1/dimensoes"));
        assertTrue(html.contains("/cut-plans/1/itens/2/dimensoes"));
        assertFalse(html.contains("absolute z-30"));
        plano.setStatus(StatusPlano.FINALIZADO);
        assertFalse(engine.process("painel", context).contains("</form>"));
    }
    @Test void diametroPadraoEValidacao() {
        service.adicionarVao(1L, form());
        PlanoCorteItem movel = banco.get(1);
        service.adicionarPuxadorH(1L, movel.getId(), "ESQUERDO", null, null);
        assertEquals(0, movel.getFuracoes().get(2).getDiametroMm().compareTo(new BigDecimal("30")));
        assertThrows(IllegalStateException.class, () ->
                service.adicionarPuxadorH(1L, movel.getId(), "ESQUERDO", null, null, BigDecimal.ZERO));
        plano.setStatus(StatusPlano.FINALIZADO);
        assertThrows(IllegalStateException.class, () -> service.editarVao(1L, 1, form()));
    }
}