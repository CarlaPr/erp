package com.alfatahi.erp.planocorte.controller;

import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.dto.AcessorioForm;
import com.alfatahi.erp.planocorte.dto.FerragemForm;
import com.alfatahi.erp.planocorte.dto.PerfilAluminioForm;
import com.alfatahi.erp.planocorte.dto.ResultadoImportacao;
import com.alfatahi.erp.planocorte.dto.SiliconeForm;
import com.alfatahi.erp.planocorte.dto.VidroForm;
import com.alfatahi.erp.planocorte.entity.Acessorio;
import com.alfatahi.erp.planocorte.entity.Ferragem;
import com.alfatahi.erp.planocorte.entity.PerfilAluminio;
import com.alfatahi.erp.planocorte.entity.Silicone;
import com.alfatahi.erp.planocorte.entity.TipoAcessorio;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.alfatahi.erp.planocorte.entity.TipoVidro;
import com.alfatahi.erp.planocorte.entity.Vidro;
import com.alfatahi.erp.planocorte.service.AcessorioService;
import com.alfatahi.erp.planocorte.service.FerragemService;
import com.alfatahi.erp.planocorte.service.ImportacaoVidroService;
import com.alfatahi.erp.planocorte.service.PerfilAluminioService;
import com.alfatahi.erp.planocorte.service.SiliconeService;
import com.alfatahi.erp.planocorte.service.VidroService;
import com.alfatahi.erp.repository.SupplierRepository;
import com.opencsv.exceptions.CsvException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Controller
@RequestMapping("/cut-plans/catalogo")
public class CatalogoInsumoController {

    private final VidroService vidroService;
    private final PerfilAluminioService perfilAluminioService;
    private final FerragemService ferragemService;
    private final SiliconeService siliconeService;
    private final AcessorioService acessorioService;
    private final ImportacaoVidroService importacaoVidroService;
    private final SupplierRepository supplierRepository;

    public CatalogoInsumoController(VidroService vidroService,
                                     PerfilAluminioService perfilAluminioService,
                                     FerragemService ferragemService,
                                     SiliconeService siliconeService,
                                     AcessorioService acessorioService,
                                     ImportacaoVidroService importacaoVidroService,
                                     SupplierRepository supplierRepository) {
        this.vidroService = vidroService;
        this.perfilAluminioService = perfilAluminioService;
        this.ferragemService = ferragemService;
        this.siliconeService = siliconeService;
        this.acessorioService = acessorioService;
        this.importacaoVidroService = importacaoVidroService;
        this.supplierRepository = supplierRepository;
    }

    @GetMapping
    public String pagina(@RequestParam(defaultValue = "vidros") String tab, Model model) {
        carregarTudo(model, tab);
        return "planocorte/catalogo";
    }

    @GetMapping("/vidros/{id}/editar")
    public String editarVidro(@PathVariable Long id, Model model) {
        Vidro vidro = vidroService.buscarPorId(id);
        VidroForm form = new VidroForm();
        form.setId(vidro.getId());
        form.setNome(vidro.getNome());
        form.setFabricante(vidro.getFabricante());
        form.setSupplierId(vidro.getSupplier() != null ? vidro.getSupplier().getId() : null);
        form.setTipo(vidro.getTipo());
        form.setEspessura(vidro.getEspessura());
        form.setCor(vidro.getCor());
        form.setAcabamento(vidro.getAcabamento());
        form.setValorPorM2(vidro.getValorPorM2());
        form.setValorMinimo(vidro.getValorMinimo());
        form.setPesoPorM2(vidro.getPesoPorM2());
        form.setDataVigencia(vidro.getDataVigencia());
        form.setDataValidade(vidro.getDataValidade());
        form.setObservacoes(vidro.getObservacoes());

        carregarTudo(model, "vidros");
        model.addAttribute("vidroForm", form);
        model.addAttribute("isEditingVidro", true);
        model.addAttribute("modalAberto", "vidro");
        return "planocorte/catalogo";
    }

    @PostMapping("/vidros")
    public String salvarVidro(@Valid @ModelAttribute("vidroForm") VidroForm form,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            carregarTudo(model, "vidros");
            model.addAttribute("isEditingVidro", form.getId() != null);
            return "planocorte/catalogo";
        }
        vidroService.salvar(form);
        redirectAttributes.addFlashAttribute("sucesso", "Vidro salvo com sucesso.");
        return "redirect:/cut-plans/catalogo?tab=vidros";
    }

    @PostMapping("/vidros/{id}/inativar")
    public String inativarVidro(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        vidroService.inativar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Vidro inativado.");
        return "redirect:/cut-plans/catalogo?tab=vidros";
    }

    @GetMapping(value = "/vidros/{id}/historico", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object historicoVidro(@PathVariable Long id) {
        return vidroService.historico(id);
    }

    @PostMapping("/vidros/importar")
    public String importarVidros(@RequestParam("arquivo") MultipartFile arquivo,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (arquivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Selecione um arquivo .csv ou .xlsx.");
            return "redirect:/cut-plans/catalogo?tab=vidros";
        }
        String nomeArquivo = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename().toLowerCase(Locale.ROOT)
                : "";
        try {
            ResultadoImportacao resultado;
            if (nomeArquivo.endsWith(".xlsx") || nomeArquivo.endsWith(".xls")) {
                resultado = importacaoVidroService.importarXlsx(arquivo.getInputStream());
            } else if (nomeArquivo.endsWith(".csv")) {
                resultado = importacaoVidroService.importarCsv(arquivo.getInputStream());
            } else {
                redirectAttributes.addFlashAttribute("erro", "Formato não suportado. Envie .csv ou .xlsx.");
                return "redirect:/cut-plans/catalogo?tab=vidros";
            }
            redirectAttributes.addFlashAttribute("sucesso",
                    resultado.getCriados() + " vidro(s) criado(s), " + resultado.getAtualizados() + " atualizado(s)."
                            + (resultado.isComErros() ? " Algumas linhas tiveram problemas — confira o log do servidor." : ""));
        } catch (IOException | CsvException e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível ler o arquivo: " + e.getMessage());
        }
        return "redirect:/cut-plans/catalogo?tab=vidros";
    }

    @GetMapping("/vidros/importar/modelo.csv")
    @ResponseBody
    public ResponseEntity<byte[]> modeloCsv() {
        byte[] conteudo = importacaoVidroService.modeloCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modelo-vidros.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(conteudo);
    }

    @GetMapping("/aluminio/{id}/editar")
    public String editarPerfil(@PathVariable Long id, Model model) {
        PerfilAluminio perfil = perfilAluminioService.buscarPorId(id);
        PerfilAluminioForm form = new PerfilAluminioForm();
        form.setId(perfil.getId());
        form.setLinha(perfil.getLinha());
        form.setPerfil(perfil.getPerfil());
        form.setCor(perfil.getCor());
        form.setFabricante(perfil.getFabricante());
        form.setSupplierId(perfil.getSupplier() != null ? perfil.getSupplier().getId() : null);
        form.setValorPorMetro(perfil.getValorPorMetro());

        carregarTudo(model, "aluminio");
        model.addAttribute("perfilForm", form);
        model.addAttribute("isEditingPerfil", true);
        model.addAttribute("modalAberto", "perfil");
        return "planocorte/catalogo";
    }

    @PostMapping("/aluminio")
    public String salvarPerfil(@Valid @ModelAttribute("perfilForm") PerfilAluminioForm form,
                                BindingResult resultado,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            carregarTudo(model, "aluminio");
            model.addAttribute("isEditingPerfil", form.getId() != null);
            return "planocorte/catalogo";
        }
        perfilAluminioService.salvar(form);
        redirectAttributes.addFlashAttribute("sucesso", "Perfil de alumínio salvo com sucesso.");
        return "redirect:/cut-plans/catalogo?tab=aluminio";
    }

    @PostMapping("/aluminio/{id}/inativar")
    public String inativarPerfil(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        perfilAluminioService.inativar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Perfil inativado.");
        return "redirect:/cut-plans/catalogo?tab=aluminio";
    }

    @GetMapping("/ferragens/{id}/editar")
    public String editarFerragem(@PathVariable Long id, Model model) {
        Ferragem ferragem = ferragemService.buscarPorId(id);
        FerragemForm form = new FerragemForm();
        form.setId(ferragem.getId());
        form.setCategoria(ferragem.getCategoria());
        form.setProduto(ferragem.getProduto());
        form.setCodigo(ferragem.getCodigo());
        form.setSupplierId(ferragem.getSupplier() != null ? ferragem.getSupplier().getId() : null);
        form.setValor(ferragem.getValor());
        form.setUnidade(ferragem.getUnidade());
        form.setTipoElementoPadrao(ferragem.getTipoElementoPadrao());
        form.setDiametroFuracaoMm(ferragem.getDiametroFuracaoMm());
        form.setQuantidadeFuros(ferragem.getQuantidadeFuros());
        form.setDistanciaBordaMm(ferragem.getDistanciaBordaMm());
        form.setDistanciaTopoMm(ferragem.getDistanciaTopoMm());
        form.setLarguraMm(ferragem.getLarguraMm());
        form.setAlturaMm(ferragem.getAlturaMm());
        form.setComprimentoMm(ferragem.getComprimentoMm());
        form.setProfundidadeMm(ferragem.getProfundidadeMm());
        form.setRaioMm(ferragem.getRaioMm());
        form.setAnguloGraus(ferragem.getAnguloGraus());

        carregarTudo(model, "ferragens");
        model.addAttribute("ferragemForm", form);
        model.addAttribute("isEditingFerragem", true);
        model.addAttribute("modalAberto", "ferragem");
        return "planocorte/catalogo";
    }

    @PostMapping("/ferragens")
    public String salvarFerragem(@Valid @ModelAttribute("ferragemForm") FerragemForm form,
                                  BindingResult resultado,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            carregarTudo(model, "ferragens");
            model.addAttribute("isEditingFerragem", form.getId() != null);
            return "planocorte/catalogo";
        }
        ferragemService.salvar(form);
        redirectAttributes.addFlashAttribute("sucesso", "Ferragem salva com sucesso.");
        return "redirect:/cut-plans/catalogo?tab=ferragens";
    }

    @PostMapping("/ferragens/{id}/inativar")
    public String inativarFerragem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ferragemService.inativar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Ferragem inativada.");
        return "redirect:/cut-plans/catalogo?tab=ferragens";
    }

    @GetMapping("/silicones/{id}/editar")
    public String editarSilicone(@PathVariable Long id, Model model) {
        Silicone silicone = siliconeService.buscarPorId(id);
        SiliconeForm form = new SiliconeForm();
        form.setId(silicone.getId());
        form.setMarca(silicone.getMarca());
        form.setCor(silicone.getCor());
        form.setEmbalagem(silicone.getEmbalagem());
        form.setSupplierId(silicone.getSupplier() != null ? silicone.getSupplier().getId() : null);
        form.setValor(silicone.getValor());

        carregarTudo(model, "silicones");
        model.addAttribute("siliconeForm", form);
        model.addAttribute("isEditingSilicone", true);
        model.addAttribute("modalAberto", "silicone");
        return "planocorte/catalogo";
    }

    @PostMapping("/silicones")
    public String salvarSilicone(@Valid @ModelAttribute("siliconeForm") SiliconeForm form,
                                  BindingResult resultado,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            carregarTudo(model, "silicones");
            model.addAttribute("isEditingSilicone", form.getId() != null);
            return "planocorte/catalogo";
        }
        siliconeService.salvar(form);
        redirectAttributes.addFlashAttribute("sucesso", "Silicone salvo com sucesso.");
        return "redirect:/cut-plans/catalogo?tab=silicones";
    }

    @PostMapping("/silicones/{id}/inativar")
    public String inativarSilicone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        siliconeService.inativar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Silicone inativado.");
        return "redirect:/cut-plans/catalogo?tab=silicones";
    }

    @GetMapping("/acessorios/{id}/editar")
    public String editarAcessorio(@PathVariable Long id, Model model) {
        Acessorio acessorio = acessorioService.buscarPorId(id);
        AcessorioForm form = new AcessorioForm();
        form.setId(acessorio.getId());
        form.setNome(acessorio.getNome());
        form.setTipo(acessorio.getTipo());
        form.setUnidade(acessorio.getUnidade());
        form.setSupplierId(acessorio.getSupplier() != null ? acessorio.getSupplier().getId() : null);
        form.setValor(acessorio.getValor());

        carregarTudo(model, "acessorios");
        model.addAttribute("acessorioForm", form);
        model.addAttribute("isEditingAcessorio", true);
        model.addAttribute("modalAberto", "acessorio");
        return "planocorte/catalogo";
    }

    @PostMapping("/acessorios")
    public String salvarAcessorio(@Valid @ModelAttribute("acessorioForm") AcessorioForm form,
                                   BindingResult resultado,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            carregarTudo(model, "acessorios");
            model.addAttribute("isEditingAcessorio", form.getId() != null);
            return "planocorte/catalogo";
        }
        acessorioService.salvar(form);
        redirectAttributes.addFlashAttribute("sucesso", "Acessório salvo com sucesso.");
        return "redirect:/cut-plans/catalogo?tab=acessorios";
    }

    @PostMapping("/acessorios/{id}/inativar")
    public String inativarAcessorio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        acessorioService.inativar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Acessório inativado.");
        return "redirect:/cut-plans/catalogo?tab=acessorios";
    }

    private void carregarTudo(Model model, String tab) {
        model.addAttribute("currentPage", "cut-plans-catalogo");
        model.addAttribute("activeTab", tab);

        model.addAttribute("vidros", vidroService.listarTodos());
        model.addAttribute("perfis", perfilAluminioService.listarTodos());
        model.addAttribute("ferragens", ferragemService.listarTodas());
        model.addAttribute("silicones", siliconeService.listarTodos());
        model.addAttribute("acessorios", acessorioService.listarTodos());

        if (!model.containsAttribute("vidroForm")) model.addAttribute("vidroForm", new VidroForm());
        if (!model.containsAttribute("perfilForm")) model.addAttribute("perfilForm", new PerfilAluminioForm());
        if (!model.containsAttribute("ferragemForm")) model.addAttribute("ferragemForm", new FerragemForm());
        if (!model.containsAttribute("siliconeForm")) model.addAttribute("siliconeForm", new SiliconeForm());
        if (!model.containsAttribute("acessorioForm")) model.addAttribute("acessorioForm", new AcessorioForm());

        java.util.List<Supplier> suppliers = supplierRepository.findAll();
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("tiposVidro", TipoVidro.values());
        model.addAttribute("tiposElemento", TipoElemento.values());
        model.addAttribute("tiposAcessorio", TipoAcessorio.values());
    }
}
