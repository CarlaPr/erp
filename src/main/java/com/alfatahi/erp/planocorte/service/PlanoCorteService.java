package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.calculo.EntradaCalculoServico;
import com.alfatahi.erp.planocorte.calculo.FolhaCalculada;
import com.alfatahi.erp.planocorte.calculo.FuracaoCalculada;
import com.alfatahi.erp.planocorte.calculo.ResultadoCalculoServico;
import com.alfatahi.erp.planocorte.calculo.ServicoCalculadora;
import com.alfatahi.erp.planocorte.calculo.ServicoCalculadoraRegistry;
import com.alfatahi.erp.planocorte.dto.ElementoTecnicoForm;
import com.alfatahi.erp.planocorte.dto.FuracaoForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteItemForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteVaoForm;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.TechnicalVisitFeature;
import com.alfatahi.erp.entity.TechnicalVisitOpening;
import com.alfatahi.erp.service.TechnicalVisitDataService;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.Anotacao;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.Ferragem;
import com.alfatahi.erp.planocorte.entity.Furacao;
import com.alfatahi.erp.planocorte.entity.PlanoCorte;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.StatusPlano;
import com.alfatahi.erp.planocorte.entity.TipoAncoragem;
import com.alfatahi.erp.planocorte.entity.TipoAnotacao;
import com.alfatahi.erp.planocorte.entity.TipoBorda;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.alfatahi.erp.planocorte.entity.Vidro;
import com.alfatahi.erp.planocorte.repository.FerragemRepository;
import com.alfatahi.erp.planocorte.repository.PlanoCorteItemRepository;
import com.alfatahi.erp.planocorte.repository.PlanoCorteRepository;
import com.alfatahi.erp.planocorte.repository.VidroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;



@Service
@Transactional
public class PlanoCorteService {

    private static final BigDecimal UM_MILHAO = BigDecimal.valueOf(1_000_000);

    private final PlanoCorteRepository planoCorteRepository;
    private final PlanoCorteItemRepository itemRepository;
    private final WorkOrderRepository workOrderRepository;
    private final VidroRepository vidroRepository;
    private final FerragemRepository ferragemRepository;
    private final RegraTecnicaService regraTecnicaService;
    private final ServicoCalculadoraRegistry servicoCalculadoraRegistry;
    private final ValidacaoElementoService validacaoElementoService;
    private final TechnicalVisitDataService technicalVisitDataService;

    public PlanoCorteService(PlanoCorteRepository planoCorteRepository,
                              PlanoCorteItemRepository itemRepository,
                              WorkOrderRepository workOrderRepository,
                              VidroRepository vidroRepository,
                              FerragemRepository ferragemRepository,
                              RegraTecnicaService regraTecnicaService,
                              ServicoCalculadoraRegistry servicoCalculadoraRegistry,
                              ValidacaoElementoService validacaoElementoService,
                              TechnicalVisitDataService technicalVisitDataService) {
        this.planoCorteRepository = planoCorteRepository;
        this.itemRepository = itemRepository;
        this.workOrderRepository = workOrderRepository;
        this.vidroRepository = vidroRepository;
        this.ferragemRepository = ferragemRepository;
        this.regraTecnicaService = regraTecnicaService;
        this.servicoCalculadoraRegistry = servicoCalculadoraRegistry;
        this.validacaoElementoService = validacaoElementoService;
        this.technicalVisitDataService = technicalVisitDataService;
    }

    @Transactional(readOnly = true)
    public List<TechnicalVisitOpening> listarMedicoesCliente(Long planoCorteId) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        if (plano.getWorkOrder() == null || plano.getWorkOrder().getClient() == null) return List.of();
        return technicalVisitDataService.findOpeningsForClient(plano.getWorkOrder().getClient().getId());
    }


    @Transactional(readOnly = true)
    public List<WorkOrder> listarOsDisponiveis() {
        return workOrderRepository.findAll().stream()
                .filter(os -> !"cancelled".equals(os.getStatus()) && !"canceled".equals(os.getStatus()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }



    @Transactional(readOnly = true)
    public boolean usaCalculoAutomatico(CategoriaServico categoria) {
        return servicoCalculadoraRegistry.possuiCalculadora(categoria);
    }

    @Transactional(readOnly = true)
    public List<PlanoCorte> listar() {
        return planoCorteRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public PlanoCorte buscarPorId(Long id) {
        return planoCorteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Plano de corte não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<PlanoCorteItem> listarItens(Long planoCorteId) {
        return itemRepository.findByPlanoCorteIdOrderByIdAsc(planoCorteId);
    }

    @Transactional(readOnly = true)
    public PlanoCorteItem buscarItem(Long planoCorteId, Long itemId) {
        return itemRepository.findByIdAndPlanoCorteId(itemId, planoCorteId)
                .orElseThrow(() -> new NoSuchElementException("Peça não encontrada no plano informado."));
    }

    public PlanoCorte criar(PlanoCorteForm form) {
        WorkOrder workOrder = workOrderRepository.findById(form.getWorkOrderId())
                .orElseThrow(() -> new NoSuchElementException("OS não encontrada: " + form.getWorkOrderId()));

        PlanoCorte plano = new PlanoCorte();
        plano.setWorkOrder(workOrder);
        plano.setCategoria(form.getCategoria());
        plano.setDescricao(form.getDescricao());
        plano.setStatus(StatusPlano.RASCUNHO);
        plano.setAreaTotalM2(BigDecimal.ZERO);
        plano.setPesoTotalKg(BigDecimal.ZERO);
        plano.setValorTotalPlano(BigDecimal.ZERO);

        return planoCorteRepository.save(plano);
    }


    public PlanoCorte editar(Long id, PlanoCorteForm form) {
        PlanoCorte plano = buscarPorId(id);

        WorkOrder workOrder = workOrderRepository.findById(form.getWorkOrderId())
                .orElseThrow(() -> new NoSuchElementException("OS não encontrada: " + form.getWorkOrderId()));
        plano.setWorkOrder(workOrder);
        plano.setDescricao(form.getDescricao());

        plano.setCategoria(form.getCategoria());

        return planoCorteRepository.save(plano);
    }


    public void excluir(Long id) {
        if (!planoCorteRepository.existsById(id)) {
            throw new NoSuchElementException("Plano de corte não encontrado: " + id);
        }
        planoCorteRepository.deleteById(id);
    }


    private BigDecimal semNegativo(BigDecimal valor) {
        return valor.signum() < 0 ? BigDecimal.ZERO : valor;
    }

    public PlanoCorteItem adicionarItem(Long planoCorteId, PlanoCorteItemForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);

        Vidro vidro = vidroRepository.findById(form.getVidroId())
                .orElseThrow(() -> new NoSuchElementException("Vidro não encontrado: " + form.getVidroId()));

        DimensoesBrutas dimensoes = resolverDimensoesPersonalizadas(
                form.isDimensoesPersonalizadas(),
                form.getLarguraBrutaMm(), form.getAlturaBrutaMm(),
                form.getAlturaBrutaEsquerdaMm(), form.getAlturaBrutaDireitaMm(),
                form.getLarguraBrutaSuperiorMm(), form.getLarguraBrutaInferiorMm());

        CategoriaServico categoriaItem = form.getCategoria() != null ? form.getCategoria() : plano.getCategoria();
        RegraTecnicaService.AjusteCalculado ajuste = regraTecnicaService.calcularAjuste(categoriaItem);

        BigDecimal larguraFinal = semNegativo(dimensoes.larguraMm().subtract(ajuste.largura()));
        BigDecimal alturaFinal = semNegativo(dimensoes.alturaMm().subtract(ajuste.altura()));

        PlanoCorteItem item = new PlanoCorteItem();
        item.setPlanoCorte(plano);
        item.setCategoria(categoriaItem);
        item.setVidroId(vidro.getId());
        item.setVidroNomeSnapshot(vidro.getNome());
        item.setEspessuraSnapshot(vidro.getEspessura());
        item.setCorSnapshot(vidro.getCor());
        item.setValorM2Snapshot(vidro.getValorPorM2());
        item.setValorMinimoSnapshot(vidro.getValorMinimo());
        item.setPesoM2Snapshot(vidro.getPesoPorM2());
        item.setLarguraBrutaMm(dimensoes.larguraMm());
        item.setAlturaBrutaMm(dimensoes.alturaMm());
        item.setQuantidade(form.getQuantidade());
        item.setTipoBorda(form.getTipoBorda());
        item.setObservacoes(form.getObservacoes());
        item.setLarguraFinalMm(larguraFinal);
        item.setAlturaFinalMm(alturaFinal);





        if (dimensoes.alturaEsquerdaMm() != null) {
            aplicarDimensoesPersonalizadas(item, dimensoes,
                    semNegativo(dimensoes.alturaEsquerdaMm().subtract(ajuste.altura())),
                    semNegativo(dimensoes.alturaDireitaMm().subtract(ajuste.altura())),
                    semNegativo(dimensoes.larguraSuperiorMm().subtract(ajuste.largura())),
                    semNegativo(dimensoes.larguraInferiorMm().subtract(ajuste.largura())));
        }
        aplicarCantosMoeda(item, form.getTipoBorda(),
                form.isCantoSuperiorEsquerdo(), form.isCantoSuperiorDireito(),
                form.isCantoInferiorEsquerdo(), form.isCantoInferiorDireito());

        calcularCustoItem(item);

        PlanoCorteItem salvo = itemRepository.save(item);
        recalcularTotaisPlano(plano);
        return salvo;
    }


    private record DimensoesBrutas(BigDecimal larguraMm, BigDecimal alturaMm,
                                    BigDecimal alturaEsquerdaMm, BigDecimal alturaDireitaMm,
                                    BigDecimal larguraSuperiorMm, BigDecimal larguraInferiorMm) {
    }


    private DimensoesBrutas resolverDimensoesPersonalizadas(
            boolean personalizadas, BigDecimal larguraMm, BigDecimal alturaMm,
            BigDecimal alturaEsquerdaMm, BigDecimal alturaDireitaMm,
            BigDecimal larguraSuperiorMm, BigDecimal larguraInferiorMm) {
        if (!personalizadas) {
            if (larguraMm == null || larguraMm.signum() <= 0) {
                throw new IllegalStateException("Informe a largura.");
            }
            if (alturaMm == null || alturaMm.signum() <= 0) {
                throw new IllegalStateException("Informe a altura.");
            }
            return new DimensoesBrutas(larguraMm, alturaMm, null, null, null, null);
        }

        if (alturaEsquerdaMm == null || alturaDireitaMm == null
                || larguraSuperiorMm == null || larguraInferiorMm == null) {
            throw new IllegalStateException(
                    "Informe a altura bruta dos dois lados (esquerdo e direito) e a largura bruta de cima e de baixo.");
        }
        if (alturaEsquerdaMm.signum() < 0 || alturaDireitaMm.signum() < 0
                || larguraSuperiorMm.signum() < 0 || larguraInferiorMm.signum() < 0) {
            throw new IllegalStateException("As dimensoes personalizadas nao podem ser negativas.");
        }

        BigDecimal larguraEfetiva = larguraSuperiorMm.max(larguraInferiorMm);
        BigDecimal alturaEfetiva = alturaEsquerdaMm.max(alturaDireitaMm);
        if (larguraEfetiva.signum() <= 0 || alturaEfetiva.signum() <= 0) {
            throw new IllegalStateException("Ao menos uma largura e uma altura devem ser maiores que zero.");
        }
        return new DimensoesBrutas(larguraEfetiva, alturaEfetiva,
                alturaEsquerdaMm, alturaDireitaMm, larguraSuperiorMm, larguraInferiorMm);
    }


    private void aplicarDimensoesPersonalizadas(PlanoCorteItem item, DimensoesBrutas dimensoes,
                                                  BigDecimal alturaEsquerdaFinalMm, BigDecimal alturaDireitaFinalMm,
                                                  BigDecimal larguraSuperiorFinalMm, BigDecimal larguraInferiorFinalMm) {
        item.setAlturaBrutaEsquerdaMm(dimensoes.alturaEsquerdaMm());
        item.setAlturaBrutaDireitaMm(dimensoes.alturaDireitaMm());
        item.setLarguraBrutaSuperiorMm(dimensoes.larguraSuperiorMm());
        item.setLarguraBrutaInferiorMm(dimensoes.larguraInferiorMm());
        item.setAlturaFinalEsquerdaMm(alturaEsquerdaFinalMm);
        item.setAlturaFinalDireitaMm(alturaDireitaFinalMm);
        item.setLarguraFinalSuperiorMm(larguraSuperiorFinalMm);
        item.setLarguraFinalInferiorMm(larguraInferiorFinalMm);
    }




    public List<PlanoCorteItem> adicionarVao(Long planoCorteId, PlanoCorteVaoForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        TechnicalVisitOpening medicaoVisita = aplicarMedicaoVisita(plano, form);
        CategoriaServico categoriaVao = form.getCategoria() != null ? form.getCategoria() : plano.getCategoria();

        ServicoCalculadora calculadora = servicoCalculadoraRegistry.buscar(categoriaVao)
                .orElseThrow(() -> new IllegalStateException(
                        "A categoria " + categoriaVao.getDescricao() + " não possui cálculo automático de vão."));

        Vidro vidro = vidroRepository.findById(form.getVidroId())
                .orElseThrow(() -> new NoSuchElementException("Vidro não encontrado: " + form.getVidroId()));

        boolean espelhoRedondo = categoriaVao == CategoriaServico.ESPELHO && form.isEspelhoRedondo();
        if (espelhoRedondo) {
            // Espelho redondo: o usuário informa apenas a altura (diâmetro) — a largura
            // acompanha automaticamente o mesmo valor, e dimensões personalizadas não se aplicam.
            form.setLarguraVaoMm(form.getAlturaVaoMm());
            form.setDimensoesPersonalizadas(false);
        }

        DimensoesBrutas dimensoes = resolverDimensoesPersonalizadas(
                form.isDimensoesPersonalizadas(),
                form.getLarguraVaoMm(), form.getAlturaVaoMm(),
                form.getAlturaBrutaEsquerdaMm(), form.getAlturaBrutaDireitaMm(),
                form.getLarguraBrutaSuperiorMm(), form.getLarguraBrutaInferiorMm());

        EntradaCalculoServico entrada = new EntradaCalculoServico(
                dimensoes.larguraMm(), dimensoes.alturaMm(),
                form.getQuantidadeFolhas(), form.getQuantidadeFolhasFixas(), form.getQuantidadeFolhasMoveis(),
                form.getLadoRecorte(),
                vidro.getTipo(),
                form.getDescontoLateralPersonalizadoMm(), form.getDescontoAlturaPersonalizadoMm(),
                form.getEspessuraBisoteMm(),
                form.getComFechadura(),
                espelhoRedondo,
                form.getAlturaBateFechaMm());
        ResultadoCalculoServico resultado = calculadora.calcular(entrada);







        BigDecimal alturaEsquerdaFinalMm = null;
        BigDecimal alturaDireitaFinalMm = null;
        BigDecimal larguraSuperiorFinalMm = null;
        BigDecimal larguraInferiorFinalMm = null;
        if (dimensoes.alturaEsquerdaMm() != null) {
            BigDecimal larguraFinalEfetivaMm = resultado.folhas().stream()
                    .map(FolhaCalculada::larguraMm)
                    .max(BigDecimal::compareTo)
                    .orElse(dimensoes.larguraMm());
            BigDecimal alturaFinalEfetivaMm = resultado.folhas().stream()
                    .map(FolhaCalculada::alturaMm)
                    .max(BigDecimal::compareTo)
                    .orElse(dimensoes.alturaMm());
            BigDecimal descontoLarguraMm = dimensoes.larguraMm().subtract(larguraFinalEfetivaMm);
            BigDecimal descontoAlturaMm = dimensoes.alturaMm().subtract(alturaFinalEfetivaMm);

            alturaEsquerdaFinalMm = semNegativo(dimensoes.alturaEsquerdaMm().subtract(descontoAlturaMm));
            alturaDireitaFinalMm = semNegativo(dimensoes.alturaDireitaMm().subtract(descontoAlturaMm));
            larguraSuperiorFinalMm = semNegativo(dimensoes.larguraSuperiorMm().subtract(descontoLarguraMm));
            larguraInferiorFinalMm = semNegativo(dimensoes.larguraInferiorMm().subtract(descontoLarguraMm));
        }

        Integer grupoVao = proximoGrupoVao(planoCorteId);
        List<PlanoCorteItem> criados = new ArrayList<>();

        for (FolhaCalculada folha : resultado.folhas()) {
            PlanoCorteItem item = new PlanoCorteItem();
            item.setPlanoCorte(plano);
            item.setCategoria(categoriaVao);
            item.setVidroId(vidro.getId());
            item.setVidroNomeSnapshot(vidro.getNome());
            item.setEspessuraSnapshot(vidro.getEspessura());
            item.setCorSnapshot(vidro.getCor());
            item.setValorM2Snapshot(vidro.getValorPorM2());
            item.setValorMinimoSnapshot(vidro.getValorMinimo());
            item.setPesoM2Snapshot(vidro.getPesoPorM2());
            item.setLarguraBrutaMm(dimensoes.larguraMm());
            item.setAlturaBrutaMm(dimensoes.alturaMm());
            item.setQuantidade(form.getQuantidadeVaos());
            item.setTipoBorda(form.getTipoBorda());
            item.setObservacoes(combinarObservacoes(folha.observacao(), form.getObservacoes()));
            item.setLarguraFinalMm(folha.larguraMm());
            item.setAlturaFinalMm(folha.alturaMm());
            item.setTipoFolha(folha.tipoFolha());
            item.setGrupoVao(grupoVao);
            item.setFuracoes(mapearFuracoes(folha.furacoes()));
            item.getElementos().addAll(folha.elementos());
            item.setEspessuraBisoteMm(folha.espessuraBisoteMm());
            item.setRedondo(folha.redondo());
            if (dimensoes.alturaEsquerdaMm() != null) {
                aplicarDimensoesPersonalizadas(item, dimensoes,
                        alturaEsquerdaFinalMm, alturaDireitaFinalMm, larguraSuperiorFinalMm, larguraInferiorFinalMm);
            }
            aplicarCantosMoeda(item, form.getTipoBorda(),
                    form.isCantoSuperiorEsquerdo(), form.isCantoSuperiorDireito(),
                    form.isCantoInferiorEsquerdo(), form.isCantoInferiorDireito());

            calcularCustoItem(item);
            criados.add(itemRepository.save(item));
        }

        if (medicaoVisita != null && !criados.isEmpty() && !medicaoVisita.getFeatures().isEmpty()) {
            PlanoCorteItem destino = criados.stream().filter(i -> i.getTipoFolha() == TipoFolha.MOVEL)
                    .findFirst().orElse(criados.get(0));
            for (TechnicalVisitFeature feature : medicaoVisita.getFeatures()) {
                destino.getElementos().add(mapearElementoVisita(destino, feature));
            }
            destino.setObservacoes(combinarObservacoes(destino.getObservacoes(),
                    "Medição importada da visita: " + medicaoVisita.getName()));
            itemRepository.save(destino);
        }

        recalcularTotaisPlano(plano);
        return criados;
    }

    public void editarDimensoesVao(Long planoCorteId, Integer grupoVao, PlanoCorteVaoForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        List<PlanoCorteItem> itensVao = listarItens(planoCorteId).stream()
                .filter(item -> Objects.equals(grupoVao, item.getGrupoVao()))
                .toList();
        if (itensVao.isEmpty()) {
            throw new NoSuchElementException("Vao nao encontrado neste plano.");
        }

        DimensoesBrutas novas = resolverDimensoesPersonalizadas(
                form.isDimensoesPersonalizadas(), form.getLarguraVaoMm(), form.getAlturaVaoMm(),
                form.getAlturaBrutaEsquerdaMm(), form.getAlturaBrutaDireitaMm(),
                form.getLarguraBrutaSuperiorMm(), form.getLarguraBrutaInferiorMm());
        PlanoCorteItem primeiro = itensVao.get(0);
        BigDecimal larguraAntiga = primeiro.getLarguraBrutaMm();
        BigDecimal alturaAntiga = primeiro.getAlturaBrutaMm();
        BigDecimal deltaLarguraPorFolha = novas.larguraMm().subtract(larguraAntiga)
                .divide(BigDecimal.valueOf(itensVao.size()), 4, RoundingMode.HALF_UP);
        BigDecimal deltaAltura = novas.alturaMm().subtract(alturaAntiga);

        for (PlanoCorteItem item : itensVao) {
            BigDecimal larguraFinalAntiga = item.getLarguraFinalMm();
            BigDecimal alturaFinalAntiga = item.getAlturaFinalMm();
            BigDecimal larguraFinalNova = semNegativo(larguraFinalAntiga.add(deltaLarguraPorFolha));
            BigDecimal alturaFinalNova = semNegativo(alturaFinalAntiga.add(deltaAltura));
            item.setLarguraBrutaMm(novas.larguraMm());
            item.setAlturaBrutaMm(novas.alturaMm());
            item.setLarguraFinalMm(larguraFinalNova);
            item.setAlturaFinalMm(alturaFinalNova);

            if (novas.alturaEsquerdaMm() != null) {
                aplicarDimensoesPersonalizadas(item, novas,
                        ajustarLadoFinal(novas.alturaEsquerdaMm(), novas.alturaMm(), alturaFinalNova),
                        ajustarLadoFinal(novas.alturaDireitaMm(), novas.alturaMm(), alturaFinalNova),
                        ajustarLadoFinal(novas.larguraSuperiorMm(), novas.larguraMm(), larguraFinalNova),
                        ajustarLadoFinal(novas.larguraInferiorMm(), novas.larguraMm(), larguraFinalNova));
            } else {
                limparDimensoesPersonalizadas(item);
            }

            for (Furacao furacao : item.getFuracoes()) {
                furacao.setPosicaoXMm(escalar(furacao.getPosicaoXMm(), larguraFinalAntiga, larguraFinalNova));
                furacao.setPosicaoYMm(escalar(furacao.getPosicaoYMm(), alturaFinalAntiga, alturaFinalNova));
            }
            for (Anotacao anotacao : item.getAnotacoes()) {
                anotacao.setX1Mm(escalar(anotacao.getX1Mm(), larguraFinalAntiga, larguraFinalNova));
                anotacao.setY1Mm(escalar(anotacao.getY1Mm(), alturaFinalAntiga, alturaFinalNova));
                anotacao.setX2Mm(escalar(anotacao.getX2Mm(), larguraFinalAntiga, larguraFinalNova));
                anotacao.setY2Mm(escalar(anotacao.getY2Mm(), alturaFinalAntiga, alturaFinalNova));
            }
            for (ElementoTecnico elemento : item.getElementos()) {
                if (elemento.getReferenciaElementoIndice() == null) {
                    elemento.setPosicaoXMm(resolverX(larguraFinalNova, elemento.getReferenciaHorizontal(), elemento.getDistanciaHorizontalMm()));
                    elemento.setPosicaoYMm(resolverY(alturaFinalNova, elemento.getReferenciaVertical(), elemento.getDistanciaVerticalMm()));
                }
            }
            recalcularPosicoesRelativas(item);
            calcularCustoItem(item);
            itemRepository.save(item);
        }
        recalcularTotaisPlano(plano);
    }

    private BigDecimal ajustarLadoFinal(BigDecimal ladoBruto, BigDecimal efetivaBruta, BigDecimal efetivaFinal) {
        return semNegativo(efetivaFinal.add(ladoBruto.subtract(efetivaBruta)));
    }

    private BigDecimal escalar(BigDecimal valor, BigDecimal dimensaoAntiga, BigDecimal dimensaoNova) {
        if (valor == null || dimensaoAntiga == null || dimensaoAntiga.signum() == 0) {
            return valor;
        }
        return valor.multiply(dimensaoNova).divide(dimensaoAntiga, 2, RoundingMode.HALF_UP);
    }

    private void limparDimensoesPersonalizadas(PlanoCorteItem item) {
        item.setAlturaBrutaEsquerdaMm(null);
        item.setAlturaBrutaDireitaMm(null);
        item.setLarguraBrutaSuperiorMm(null);
        item.setLarguraBrutaInferiorMm(null);
        item.setAlturaFinalEsquerdaMm(null);
        item.setAlturaFinalDireitaMm(null);
        item.setLarguraFinalSuperiorMm(null);
        item.setLarguraFinalInferiorMm(null);
    }

    private TechnicalVisitOpening aplicarMedicaoVisita(PlanoCorte plano, PlanoCorteVaoForm form) {
        if (form.getSourceOpeningId() == null) return null;
        TechnicalVisitOpening opening = technicalVisitDataService.findOpening(form.getSourceOpeningId());
        UUID clientePlano = plano.getWorkOrder() != null && plano.getWorkOrder().getClient() != null
                ? plano.getWorkOrder().getClient().getId() : null;
        UUID clienteVisita = opening.getTechnicalVisit().getClient() != null
                ? opening.getTechnicalVisit().getClient().getId() : null;
        if (!Objects.equals(clientePlano, clienteVisita)) {
            throw new IllegalStateException("A medição selecionada pertence a outro cliente.");
        }
        boolean detalhada = opening.getGrossHeightLeftMm() != null && opening.getGrossHeightRightMm() != null
                && opening.getGrossWidthTopMm() != null && opening.getGrossWidthBottomMm() != null;
        if (opening.getServiceCategory() != null) {
            form.setCategoria(opening.getServiceCategory());
        }
        form.setDimensoesPersonalizadas(detalhada);
        if (detalhada) {
            form.setAlturaBrutaEsquerdaMm(opening.getGrossHeightLeftMm());
            form.setAlturaBrutaDireitaMm(opening.getGrossHeightRightMm());
            form.setLarguraBrutaSuperiorMm(opening.getGrossWidthTopMm());
            form.setLarguraBrutaInferiorMm(opening.getGrossWidthBottomMm());
        } else {
            form.setLarguraVaoMm(opening.getWidthMm());
            form.setAlturaVaoMm(opening.getHeightMm());
        }
        form.setObservacoes(combinarObservacoes(opening.getNotes(), form.getObservacoes()));
        return opening;
    }

    private ElementoTecnico mapearElementoVisita(PlanoCorteItem item, TechnicalVisitFeature feature) {
        ElementoTecnico e = new ElementoTecnico();
        e.setTipo(feature.getType());
        e.setNome(feature.getName());
        e.setAncoragem(TipoAncoragem.CENTRO);
        e.setReferenciaHorizontal(feature.getReferenceHorizontal());
        e.setDistanciaHorizontalMm(feature.getDistanceHorizontalMm());
        e.setReferenciaVertical(feature.getReferenceVertical());
        e.setDistanciaVerticalMm(feature.getDistanceVerticalMm());
        e.setDiametroMm(feature.getDiameterMm());
        e.setLarguraMm(feature.getWidthMm());
        e.setAlturaMm(feature.getHeightMm());
        e.setProfundidadeMm(feature.getDepthMm());
        e.setRaioMm(feature.getRadiusMm());
        e.setLado(feature.getCorner());
        e.setObservacao(feature.getNotes());
        e.setPosicaoXMm(resolverX(item.getLarguraFinalMm(), e.getReferenciaHorizontal(), e.getDistanciaHorizontalMm()));
        e.setPosicaoYMm(resolverY(item.getAlturaFinalMm(), e.getReferenciaVertical(), e.getDistanciaVerticalMm()));
        return e;
    }




    @Transactional(readOnly = true)
    public Map<TipoFuracao, Integer> resumoFuracoes(Long planoCorteId) {
        Map<TipoFuracao, Integer> resumo = new EnumMap<>(TipoFuracao.class);
        for (PlanoCorteItem item : listarItens(planoCorteId)) {
            for (Furacao furacao : item.getFuracoes()) {
                resumo.merge(furacao.getTipo(), item.getQuantidade(), Integer::sum);
            }
        }
        return resumo;
    }

    private Integer proximoGrupoVao(Long planoCorteId) {
        return itemRepository.findByPlanoCorteIdOrderByIdAsc(planoCorteId).stream()
                .map(PlanoCorteItem::getGrupoVao)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private List<Furacao> mapearFuracoes(List<FuracaoCalculada> calculadas) {
        List<Furacao> furacoes = new ArrayList<>();
        for (FuracaoCalculada calculada : calculadas) {
            furacoes.add(new Furacao(calculada.tipo(), calculada.posicaoXMm(), calculada.posicaoYMm(),
                    calculada.diametroMm(), calculada.descricao()));
        }
        return furacoes;
    }

    private String combinarObservacoes(String daFolha, String doUsuario) {
        boolean semFolha = daFolha == null || daFolha.isBlank();
        boolean semUsuario = doUsuario == null || doUsuario.isBlank();
        if (semFolha && semUsuario) return null;
        if (semUsuario) return daFolha;
        if (semFolha) return doUsuario;
        return daFolha + " — " + doUsuario;
    }


    private void aplicarCantosMoeda(PlanoCorteItem item, TipoBorda tipoBorda,
                                     boolean superiorEsquerdo, boolean superiorDireito,
                                     boolean inferiorEsquerdo, boolean inferiorDireito) {
        boolean cantoMoeda = tipoBorda == TipoBorda.CANTO_MOEDA || tipoBorda == TipoBorda.CANTO_GARRAFA;
        item.setCantoMoedaSuperiorEsquerdo(cantoMoeda && superiorEsquerdo);
        item.setCantoMoedaSuperiorDireito(cantoMoeda && superiorDireito);
        item.setCantoMoedaInferiorEsquerdo(cantoMoeda && inferiorEsquerdo);
        item.setCantoMoedaInferiorDireito(cantoMoeda && inferiorDireito);
    }






    public ResultadoElemento adicionarElemento(Long planoCorteId, Long itemId, ElementoTecnicoForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);

        Ferragem ferragem = form.getFerragemId() != null
                ? ferragemRepository.findById(form.getFerragemId())
                        .orElseThrow(() -> new NoSuchElementException("Ferragem não encontrada: " + form.getFerragemId()))
                : null;

        List<ElementoTecnico> novosElementos = new ArrayList<>();
        int quantidadeFuros = ferragem != null && ferragem.getQuantidadeFuros() != null && ferragem.getQuantidadeFuros() > 0
                ? ferragem.getQuantidadeFuros()
                : 1;

        if (quantidadeFuros <= 1) {
            novosElementos.add(construirElemento(item, form, ferragem, form.getReferenciaHorizontal(), form.getDistanciaHorizontalMm()));
        } else {







            BigDecimal distBorda = form.getDistanciaHorizontalMm() != null ? form.getDistanciaHorizontalMm() : ferragem.getDistanciaBordaMm();
            BigDecimal larguraPeca = item.getLarguraFinalMm();
            for (int i = 0; i < quantidadeFuros; i++) {
                BigDecimal x;
                if (i == 0) {
                    x = distBorda;
                } else if (i == 1) {
                    x = larguraPeca.subtract(distBorda);
                } else {
                    BigDecimal vao = larguraPeca.subtract(distBorda.multiply(BigDecimal.valueOf(2)));
                    int espacos = quantidadeFuros - 1;
                    x = distBorda.add(vao.multiply(BigDecimal.valueOf(i - 1)).divide(BigDecimal.valueOf(espacos), 2, RoundingMode.HALF_UP));
                }
                ElementoTecnico elemento = construirElemento(item, form, ferragem, ReferenciaHorizontal.ESQUERDA, x);
                novosElementos.add(elemento);
            }
        }

        List<String> avisos = new ArrayList<>();
        for (ElementoTecnico elemento : novosElementos) {
            avisos.addAll(validacaoElementoService.validar(item, elemento, item.getCategoria()));
        }

        item.getElementos().addAll(novosElementos);
        itemRepository.save(item);

        return new ResultadoElemento(novosElementos, avisos);
    }

    private ElementoTecnico construirElemento(PlanoCorteItem item, ElementoTecnicoForm form, Ferragem ferragem,
                                               ReferenciaHorizontal referenciaHorizontal, BigDecimal distanciaHorizontalMm) {
        ElementoTecnico elemento = new ElementoTecnico();




        elemento.setTipo(form.getTipo());
        elemento.setNome(form.getNome());
        elemento.setAncoragem(form.getAncoragem() != null ? form.getAncoragem() : TipoAncoragem.CENTRO);
        elemento.setReferenciaHorizontal(referenciaHorizontal);
        elemento.setDistanciaHorizontalMm(distanciaHorizontalMm);
        elemento.setReferenciaVertical(form.getReferenciaVertical());







        elemento.setDistanciaVerticalMm(valorOuPadrao(form.getDistanciaVerticalMm(), ferragem != null ? ferragem.getDistanciaTopoMm() : null));
        elemento.setReferenciaElementoIndice(form.getReferenciaElementoIndice());
        elemento.setDiametroMm(valorOuPadrao(form.getDiametroMm(), ferragem != null ? ferragem.getDiametroFuracaoMm() : null));
        elemento.setLarguraMm(valorOuPadrao(form.getLarguraMm(), ferragem != null ? ferragem.getLarguraMm() : null));
        elemento.setAlturaMm(valorOuPadrao(form.getAlturaMm(), ferragem != null ? ferragem.getAlturaMm() : null));
        elemento.setComprimentoMm(valorOuPadrao(form.getComprimentoMm(), ferragem != null ? ferragem.getComprimentoMm() : null));
        elemento.setProfundidadeMm(valorOuPadrao(form.getProfundidadeMm(), ferragem != null ? ferragem.getProfundidadeMm() : null));
        elemento.setRaioMm(valorOuPadrao(form.getRaioMm(), ferragem != null ? ferragem.getRaioMm() : null));
        elemento.setAnguloGraus(valorOuPadrao(form.getAnguloGraus(), ferragem != null ? ferragem.getAnguloGraus() : null));
        elemento.setOrientacao(form.getOrientacao());
        elemento.setFormato(form.getFormato());
        elemento.setLado(form.getLado());
        elemento.setFerragemNomeSnapshot(ferragem != null ? ferragem.getProduto() : form.getFerragemNomeSnapshot());
        elemento.setObservacao(form.getObservacao());

        ElementoTecnico referencia = buscarElementoReferencia(item, elemento.getReferenciaElementoIndice());
        BigDecimal x = referencia == null
                ? resolverX(item.getLarguraFinalMm(), elemento.getReferenciaHorizontal(), elemento.getDistanciaHorizontalMm())
                : resolverXRelativo(referencia, elemento.getReferenciaHorizontal(), elemento.getDistanciaHorizontalMm());
        BigDecimal y = referencia == null
                ? resolverY(item.getAlturaFinalMm(), elemento.getReferenciaVertical(), elemento.getDistanciaVerticalMm())
                : resolverYRelativo(referencia, elemento.getReferenciaVertical(), elemento.getDistanciaVerticalMm());
        elemento.setPosicaoXMm(x);
        elemento.setPosicaoYMm(y);
        return elemento;
    }

    private BigDecimal valorOuPadrao(BigDecimal valorFormulario, BigDecimal valorPadraoFerragem) {
        return valorFormulario != null ? valorFormulario : valorPadraoFerragem;
    }

    private BigDecimal resolverX(BigDecimal larguraPeca, ReferenciaHorizontal referencia, BigDecimal distancia) {
        BigDecimal dist = distancia != null ? distancia : BigDecimal.ZERO;
        return switch (referencia) {
            case ESQUERDA -> dist;
            case DIREITA -> larguraPeca.subtract(dist);
            case CENTRO -> larguraPeca.divide(BigDecimal.valueOf(2)).add(dist);
        };
    }

    private BigDecimal resolverY(BigDecimal alturaPeca, ReferenciaVertical referencia, BigDecimal distancia) {
        BigDecimal dist = distancia != null ? distancia : BigDecimal.ZERO;
        return switch (referencia) {
            case SUPERIOR -> dist;
            case INFERIOR -> alturaPeca.subtract(dist);
            case CENTRO -> alturaPeca.divide(BigDecimal.valueOf(2)).add(dist);
        };
    }

    private ElementoTecnico buscarElementoReferencia(PlanoCorteItem item, Integer indice) {
        if (indice == null) {
            return null;
        }
        verificarIndice(item.getElementos(), indice, "Elemento de referencia");
        ElementoTecnico referencia = item.getElementos().get(indice);
        if (referencia.getPosicaoXMm() == null || referencia.getPosicaoYMm() == null) {
            throw new IllegalStateException("O elemento selecionado como referencia ainda nao possui posicao valida.");
        }
        return referencia;
    }

    private BigDecimal resolverXRelativo(ElementoTecnico referencia, ReferenciaHorizontal direcao, BigDecimal distancia) {
        BigDecimal dist = distancia != null ? distancia : BigDecimal.ZERO;
        return switch (direcao) {
            case ESQUERDA -> referencia.getPosicaoXMm().subtract(dist);
            case DIREITA -> referencia.getPosicaoXMm().add(dist);
            case CENTRO -> referencia.getPosicaoXMm();
        };
    }

    private BigDecimal resolverYRelativo(ElementoTecnico referencia, ReferenciaVertical direcao, BigDecimal distancia) {
        BigDecimal dist = distancia != null ? distancia : BigDecimal.ZERO;
        return switch (direcao) {
            case SUPERIOR -> referencia.getPosicaoYMm().subtract(dist);
            case INFERIOR -> referencia.getPosicaoYMm().add(dist);
            case CENTRO -> referencia.getPosicaoYMm();
        };
    }

    private void validarReferenciaSemCiclo(PlanoCorteItem item, int indiceAtual, Integer indiceReferencia) {
        Integer cursor = indiceReferencia;
        int passos = 0;
        while (cursor != null) {
            if (cursor == indiceAtual) {
                throw new IllegalStateException("Um elemento nao pode depender dele mesmo, direta ou indiretamente.");
            }
            verificarIndice(item.getElementos(), cursor, "Elemento de referencia");
            cursor = item.getElementos().get(cursor).getReferenciaElementoIndice();
            if (++passos > item.getElementos().size()) {
                throw new IllegalStateException("Foi detectado um ciclo entre as referencias dos elementos.");
            }
        }
    }

    private void recalcularPosicoesRelativas(PlanoCorteItem item) {
        for (int passo = 0; passo < item.getElementos().size(); passo++) {
            for (ElementoTecnico elemento : item.getElementos()) {
                ElementoTecnico referencia = buscarElementoReferencia(item, elemento.getReferenciaElementoIndice());
                if (referencia != null) {
                    elemento.setPosicaoXMm(resolverXRelativo(referencia, elemento.getReferenciaHorizontal(), elemento.getDistanciaHorizontalMm()));
                    elemento.setPosicaoYMm(resolverYRelativo(referencia, elemento.getReferenciaVertical(), elemento.getDistanciaVerticalMm()));
                }
            }
        }
    }
    public record ResultadoElemento(List<ElementoTecnico> elementos, List<String> avisos) {
    }


    public ResultadoElemento editarElemento(Long planoCorteId, Long itemId, int index, ElementoTecnicoForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        verificarIndice(item.getElementos(), index, "Elemento técnico");

        Ferragem ferragem = form.getFerragemId() != null
                ? ferragemRepository.findById(form.getFerragemId())
                        .orElseThrow(() -> new NoSuchElementException("Ferragem não encontrada: " + form.getFerragemId()))
                : null;

        validarReferenciaSemCiclo(item, index, form.getReferenciaElementoIndice());
        ElementoTecnico atualizado = construirElemento(item, form, ferragem, form.getReferenciaHorizontal(), form.getDistanciaHorizontalMm());
        List<String> avisos = validacaoElementoService.validar(item, atualizado, item.getCategoria());

        item.getElementos().set(index, atualizado);
        recalcularPosicoesRelativas(item);
        itemRepository.save(item);
        return new ResultadoElemento(List.of(atualizado), avisos);
    }



    public void removerElemento(Long planoCorteId, Long itemId, int index) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        verificarIndice(item.getElementos(), index, "Elemento técnico");
        for (ElementoTecnico elemento : item.getElementos()) {
            Integer referencia = elemento.getReferenciaElementoIndice();
            if (referencia == null) {
                continue;
            }
            if (referencia == index) {
                elemento.setReferenciaElementoIndice(null);
                elemento.setReferenciaHorizontal(ReferenciaHorizontal.ESQUERDA);
                elemento.setDistanciaHorizontalMm(elemento.getPosicaoXMm());
                elemento.setReferenciaVertical(ReferenciaVertical.SUPERIOR);
                elemento.setDistanciaVerticalMm(elemento.getPosicaoYMm());
            } else if (referencia > index) {
                elemento.setReferenciaElementoIndice(referencia - 1);
            }
        }
        item.getElementos().remove(index);
        itemRepository.save(item);
    }

    public void editarFuracao(Long planoCorteId, Long itemId, int index, FuracaoForm form) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        verificarIndice(item.getFuracoes(), index, "Furação");
        if (form.getTipo() == null || form.getPosicaoXMm() == null || form.getPosicaoYMm() == null) {
            throw new IllegalStateException("Informe tipo, posição X e posição Y da furação.");
        }
        if (form.getPosicaoXMm().signum() < 0 || form.getPosicaoYMm().signum() < 0
                || form.getPosicaoXMm().compareTo(item.getLarguraFinalMm()) > 0
                || form.getPosicaoYMm().compareTo(item.getAlturaFinalMm()) > 0) {
            throw new IllegalStateException("A posição informada precisa ficar dentro do vidro.");
        }
        Furacao f = item.getFuracoes().get(index);
        f.setTipo(form.getTipo());
        f.setPosicaoXMm(form.getPosicaoXMm());
        f.setPosicaoYMm(form.getPosicaoYMm());
        f.setDiametroMm(form.getDiametroMm());
        f.setDescricao(form.getDescricao());
        itemRepository.save(item);
    }



    public void removerFuracao(Long planoCorteId, Long itemId, int index) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        verificarIndice(item.getFuracoes(), index, "Furação");
        item.getFuracoes().remove(index);
        itemRepository.save(item);
    }


    public Anotacao adicionarAnotacao(Long planoCorteId, Long itemId, TipoAnotacao tipo,
                                       BigDecimal x1Mm, BigDecimal y1Mm,
                                       BigDecimal x2Mm, BigDecimal y2Mm, String texto) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);

        if (tipo == null || x1Mm == null || y1Mm == null) {
            throw new IllegalStateException("Informe o tipo e a posição da anotação.");
        }
        if (tipo == TipoAnotacao.TEXTO && (texto == null || texto.isBlank())) {
            throw new IllegalStateException("Informe o texto da anotação.");
        }
        if ((tipo == TipoAnotacao.SETA || tipo == TipoAnotacao.LINHA) && (x2Mm == null || y2Mm == null)) {
            throw new IllegalStateException("Informe o ponto final da " + tipo.getDescricao().toLowerCase() + ".");
        }

        Anotacao anotacao = new Anotacao();
        anotacao.setTipo(tipo);
        anotacao.setX1Mm(x1Mm);
        anotacao.setY1Mm(y1Mm);
        anotacao.setX2Mm(x2Mm);
        anotacao.setY2Mm(y2Mm);
        anotacao.setTexto(texto);

        item.getAnotacoes().add(anotacao);
        itemRepository.save(item);
        return anotacao;
    }

    public void removerAnotacao(Long planoCorteId, Long itemId, int index) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        verificarIndice(item.getAnotacoes(), index, "Anotação");
        item.getAnotacoes().remove(index);
        itemRepository.save(item);
    }

    private void verificarIndice(List<?> lista, int index, String rotulo) {
        if (index < 0 || index >= lista.size()) {
            throw new IllegalStateException(rotulo + " não encontrado(a).");
        }
    }

    private static final BigDecimal PUXADOR_H_DIST_PISO_MM = BigDecimal.valueOf(1000);
    private static final BigDecimal PUXADOR_H_DIST_LATERAL_MM = BigDecimal.valueOf(300);
    private static final BigDecimal PUXADOR_H_DIAMETRO_FURO_MM = BigDecimal.valueOf(300);




    private static final BigDecimal PUXADOR_H_DIST_ENTRE_FUROS_MM = BigDecimal.valueOf(300);

    private static final BigDecimal FECHADURA_DIST_PISO_MM = BigDecimal.valueOf(1000);
    private static final BigDecimal FECHADURA_DIST_LATERAL_MM = BigDecimal.valueOf(50);
    private static final BigDecimal FECHADURA_DIST_ENTRE_FUROS_MM = BigDecimal.valueOf(100);
    private static final BigDecimal FECHADURA_DIAMETRO_FURO_MM = BigDecimal.valueOf(16);
    private static final BigDecimal FECHADURA_DIAMETRO_FURO_MAIOR_MM = BigDecimal.valueOf(19);

    private static final BigDecimal FECHADURA_DESLOCAMENTO_FURO_MAIOR_MM = BigDecimal.valueOf(8);



    private static final BigDecimal PUXADOR_SIMPLES_DIST_PISO_MM = BigDecimal.valueOf(1000);
    private static final BigDecimal PUXADOR_SIMPLES_DIST_LATERAL_MM = BigDecimal.valueOf(50);
    private static final BigDecimal PUXADOR_SIMPLES_DIAMETRO_FURO_MM = BigDecimal.valueOf(7);



    /**
     * Atalhos de ferragens (Puxador H, Puxador simples, Fechadura) não se aplicam a vidro fixo:
     * em folhas fixas, ferragens de abertura (puxador, fechadura etc.) não fazem sentido — se
     * for realmente necessário algum furo/recorte pontual, deve ser adicionado manualmente como
     * Elemento Técnico.
     */
    private void garantirNaoVidroFixo(PlanoCorteItem item, String nomeAtalho) {
        if (item.getTipoFolha() == TipoFolha.FIXA) {
            throw new IllegalStateException(nomeAtalho + " não se aplica a vidro fixo — adicione um furo/recorte manualmente como Elemento Técnico, se necessário.");
        }
    }


    public void adicionarPuxadorH(Long planoCorteId, Long itemId, String lado, BigDecimal tamanhoEntreFurosMm) {
        adicionarPuxadorH(planoCorteId, itemId, lado, tamanhoEntreFurosMm, null);
    }

    public void adicionarPuxadorH(Long planoCorteId, Long itemId, String lado, BigDecimal tamanhoEntreFurosMm,
                                   BigDecimal distanciaBordaMm) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        if (item.getCategoria() == CategoriaServico.ESPELHO) {
            throw new IllegalStateException("Espelho não usa Puxador H automático — adicione um furo/recorte como Elemento Técnico.");
        }
        garantirNaoVidroFixo(item, "Puxador H");
        boolean direito = "DIREITO".equalsIgnoreCase(lado);

        BigDecimal distEntreFuros = tamanhoEntreFurosMm != null && tamanhoEntreFurosMm.signum() > 0
                ? tamanhoEntreFurosMm
                : PUXADOR_H_DIST_ENTRE_FUROS_MM;



        BigDecimal distBorda = distanciaBordaMm != null && distanciaBordaMm.signum() > 0
                ? distanciaBordaMm
                : PUXADOR_H_DIST_LATERAL_MM;

        BigDecimal x = direito
                ? item.getLarguraFinalMm().subtract(distBorda)
                : distBorda;
        BigDecimal yCentro = item.getAlturaFinalMm().subtract(PUXADOR_H_DIST_PISO_MM);
        BigDecimal metadeGap = distEntreFuros.divide(BigDecimal.valueOf(2));

        String rotulo = "Puxador H (" + distEntreFuros.stripTrailingZeros().toPlainString() + "mm entre furos, "
                + distBorda.stripTrailingZeros().toPlainString() + "mm da borda)";
        item.getFuracoes().add(new Furacao(TipoFuracao.PUXADOR, x, yCentro.subtract(metadeGap), PUXADOR_H_DIAMETRO_FURO_MM, rotulo));
        item.getFuracoes().add(new Furacao(TipoFuracao.PUXADOR, x, yCentro.add(metadeGap), PUXADOR_H_DIAMETRO_FURO_MM, ""));
        itemRepository.save(item);
    }



    public void adicionarPuxadorSimples(Long planoCorteId, Long itemId, String lado) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        if (item.getCategoria() == CategoriaServico.ESPELHO) {
            throw new IllegalStateException("Espelho não usa Puxador automático — adicione um furo/recorte como Elemento Técnico.");
        }
        garantirNaoVidroFixo(item, "Puxador");
        boolean direito = "DIREITO".equalsIgnoreCase(lado);

        BigDecimal x = direito
                ? item.getLarguraFinalMm().subtract(PUXADOR_SIMPLES_DIST_LATERAL_MM)
                : PUXADOR_SIMPLES_DIST_LATERAL_MM;
        BigDecimal y = item.getAlturaFinalMm().subtract(PUXADOR_SIMPLES_DIST_PISO_MM);

        item.getFuracoes().add(new Furacao(TipoFuracao.PUXADOR, x, y, PUXADOR_SIMPLES_DIAMETRO_FURO_MM, "Puxador simples"));
        itemRepository.save(item);
    }

    public void adicionarFechadura(Long planoCorteId, Long itemId, String lado) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);
        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        if (item.getCategoria() == CategoriaServico.ESPELHO) {
            throw new IllegalStateException("Espelho não usa Fechadura automática — adicione um furo/recorte como Elemento Técnico.");
        }
        garantirNaoVidroFixo(item, "Fechadura");
        boolean direito = "DIREITO".equalsIgnoreCase(lado);

        BigDecimal xAlinhados = direito
                ? item.getLarguraFinalMm().subtract(FECHADURA_DIST_LATERAL_MM)
                : FECHADURA_DIST_LATERAL_MM;
        BigDecimal xMaior = xAlinhados.add(FECHADURA_DESLOCAMENTO_FURO_MAIOR_MM);



        BigDecimal yCentro = item.getCategoria() == CategoriaServico.JANELA_PADRAO
                ? item.getAlturaFinalMm().divide(BigDecimal.valueOf(2))
                : item.getAlturaFinalMm().subtract(FECHADURA_DIST_PISO_MM);
        BigDecimal metadeGap = FECHADURA_DIST_ENTRE_FUROS_MM.divide(BigDecimal.valueOf(2));

        item.getFuracoes().add(new Furacao(TipoFuracao.FECHADURA, xAlinhados, yCentro.subtract(metadeGap), FECHADURA_DIAMETRO_FURO_MM, "Fechadura"));
        item.getFuracoes().add(new Furacao(TipoFuracao.FECHADURA, xAlinhados, yCentro.add(metadeGap), FECHADURA_DIAMETRO_FURO_MM, ""));
        item.getFuracoes().add(new Furacao(TipoFuracao.FECHADURA, xMaior, yCentro, FECHADURA_DIAMETRO_FURO_MAIOR_MM, ""));
        itemRepository.save(item);
    }

    public void removerItem(Long planoCorteId, Long itemId) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        garantirRascunho(plano);

        PlanoCorteItem item = buscarItem(planoCorteId, itemId);
        itemRepository.delete(item);
        recalcularTotaisPlano(plano);
    }

    public PlanoCorte finalizar(Long planoCorteId) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        plano.setStatus(StatusPlano.FINALIZADO);
        return planoCorteRepository.save(plano);
    }

    public PlanoCorte reabrir(Long planoCorteId) {
        PlanoCorte plano = buscarPorId(planoCorteId);
        plano.setStatus(StatusPlano.RASCUNHO);
        return planoCorteRepository.save(plano);
    }

    private void garantirRascunho(PlanoCorte plano) {
        if (plano.getStatus() == StatusPlano.FINALIZADO) {
            throw new IllegalStateException("Este plano já foi finalizado. Reabra para poder alterar as peças.");
        }
    }

    private void calcularCustoItem(PlanoCorteItem item) {
        BigDecimal areaUnitariaM2 = item.getLarguraFinalMm()
                .multiply(item.getAlturaFinalMm())
                .divide(UM_MILHAO, 6, RoundingMode.HALF_UP);

        BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
        BigDecimal areaTotalM2 = areaUnitariaM2.multiply(quantidade);
        item.setAreaM2(areaTotalM2.setScale(4, RoundingMode.HALF_UP));

        BigDecimal pesoM2 = item.getPesoM2Snapshot() != null ? item.getPesoM2Snapshot() : BigDecimal.ZERO;
        item.setPesoKg(areaTotalM2.multiply(pesoM2).setScale(3, RoundingMode.HALF_UP));

        BigDecimal valorPorPeca = areaUnitariaM2.multiply(item.getValorM2Snapshot());
        if (item.getValorMinimoSnapshot() != null && valorPorPeca.compareTo(item.getValorMinimoSnapshot()) < 0) {
            valorPorPeca = item.getValorMinimoSnapshot();
        }
        item.setValorTotal(valorPorPeca.multiply(quantidade).setScale(2, RoundingMode.HALF_UP));
    }

    private void recalcularTotaisPlano(PlanoCorte plano) {
        List<PlanoCorteItem> itens = itemRepository.findByPlanoCorteIdOrderByIdAsc(plano.getId());

        BigDecimal area = BigDecimal.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        BigDecimal valor = BigDecimal.ZERO;
        for (PlanoCorteItem item : itens) {
            area = area.add(item.getAreaM2());
            peso = peso.add(item.getPesoKg());
            valor = valor.add(item.getValorTotal());
        }

        plano.setAreaTotalM2(area);
        plano.setPesoTotalKg(peso);
        plano.setValorTotalPlano(valor);
        planoCorteRepository.save(plano);
    }
}
