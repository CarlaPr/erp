package com.alfatahi.erp.planocorte.controller;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.planocorte.dto.ElementoTecnicoForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteItemForm;
import com.alfatahi.erp.planocorte.dto.PlanoCorteVaoForm;
import com.alfatahi.erp.planocorte.entity.*;
import com.alfatahi.erp.planocorte.repository.FerragemRepository;
import com.alfatahi.erp.planocorte.service.CroquiService;
import com.alfatahi.erp.planocorte.service.PdfService;
import com.alfatahi.erp.planocorte.service.PlanoCorteService;
import com.alfatahi.erp.planocorte.service.VidroService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cut-plans")
public class PlanoCorteController {

    private final PlanoCorteService planoCorteService;
    private final VidroService vidroService;
    private final FerragemRepository ferragemRepository;
    private final CroquiService croquiService;
    private final PdfService pdfService;

    public PlanoCorteController(PlanoCorteService planoCorteService,
                                 VidroService vidroService,
                                 FerragemRepository ferragemRepository,
                                 CroquiService croquiService,
                                 PdfService pdfService) {
        this.planoCorteService = planoCorteService;
        this.vidroService = vidroService;
        this.ferragemRepository = ferragemRepository;
        this.croquiService = croquiService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("currentPage", "cut-plans");
        model.addAttribute("planos", planoCorteService.listar());
        model.addAttribute("planoCorteForm", new PlanoCorteForm());
        adicionarListasApoioNovo(model);
        return "planocorte/list";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        PlanoCorte plano = planoCorteService.buscarPorId(id);

        PlanoCorteForm form = new PlanoCorteForm();
        form.setId(plano.getId());
        form.setWorkOrderId(plano.getWorkOrder().getId());
        form.setCategoria(plano.getCategoria());
        form.setDescricao(plano.getDescricao());

        model.addAttribute("currentPage", "cut-plans");
        model.addAttribute("planos", planoCorteService.listar());
        model.addAttribute("planoCorteForm", form);
        model.addAttribute("isEditing", true);
        adicionarListasApoioNovo(model);
        return "planocorte/list";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("planoCorteForm") PlanoCorteForm form,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans");
            model.addAttribute("planos", planoCorteService.listar());
            model.addAttribute("isEditing", form.getId() != null);
            adicionarListasApoioNovo(model);
            return "planocorte/list";
        }
        try {
            if (form.getId() != null) {
                planoCorteService.editar(form.getId(), form);
                redirectAttributes.addFlashAttribute("sucesso", "Plano de corte atualizado.");
                return "redirect:/cut-plans";
            }
            PlanoCorte plano = planoCorteService.criar(form);
            redirectAttributes.addFlashAttribute("sucesso", "Plano criado. Agora adicione as peças.");
            return "redirect:/cut-plans/" + plano.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/cut-plans";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Plano de corte excluído.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "cut-plans");
        carregarDetalhe(id, model, new PlanoCorteItemForm(), new PlanoCorteVaoForm());
        return "planocorte/detalhe";
    }

    @PostMapping("/{id}/itens")
    public String adicionarItem(@PathVariable Long id,
                                 @Valid @ModelAttribute("itemForm") PlanoCorteItemForm itemForm,
                                 BindingResult resultado,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans");
            carregarDetalhe(id, model, itemForm, new PlanoCorteVaoForm());
            return "planocorte/detalhe";
        }
        try {
            planoCorteService.adicionarItem(id, itemForm);
            redirectAttributes.addFlashAttribute("sucesso", "Peça adicionada.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/vaos")
    public String adicionarVao(@PathVariable Long id,
                                @Valid @ModelAttribute("vaoForm") PlanoCorteVaoForm vaoForm,
                                BindingResult resultado,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans");
            carregarDetalhe(id, model, new PlanoCorteItemForm(), vaoForm);
            return "planocorte/detalhe";
        }
        try {
            List<PlanoCorteItem> criados = planoCorteService.adicionarVao(id, vaoForm);
            redirectAttributes.addFlashAttribute("sucesso",
                    criados.size() + " folha(s) geradas automaticamente a partir do vão.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/elementos")
    public String adicionarElemento(@PathVariable Long id, @PathVariable Long itemId,
                                     @Valid @ModelAttribute("elementoForm") ElementoTecnicoForm elementoForm,
                                     BindingResult resultado,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans");
            carregarDetalhe(id, model, new PlanoCorteItemForm(), new PlanoCorteVaoForm());
            model.addAttribute("elementoForm", elementoForm);
            return "planocorte/detalhe";
        }
        try {
            PlanoCorteService.ResultadoElemento resultadoElemento = planoCorteService.adicionarElemento(id, itemId, elementoForm);
            if (resultadoElemento.avisos().isEmpty()) {
                redirectAttributes.addFlashAttribute("sucesso",
                        resultadoElemento.elementos().size() + " elemento(s) adicionado(s).");
            } else {
                redirectAttributes.addFlashAttribute("erro", String.join(" | ", resultadoElemento.avisos()));
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/excluir")
    public String removerItem(@PathVariable Long id, @PathVariable Long itemId,
                               RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.removerItem(id, itemId);
            redirectAttributes.addFlashAttribute("sucesso", "Peça removida.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/elementos/{index}/editar")
    public String editarElemento(@PathVariable Long id, @PathVariable Long itemId, @PathVariable int index,
                                  @ModelAttribute("elementoEditForm") ElementoTecnicoForm elementoForm,
                                  RedirectAttributes redirectAttributes) {
        try {
            PlanoCorteService.ResultadoElemento resultado = planoCorteService.editarElemento(id, itemId, index, elementoForm);
            if (resultado.avisos().isEmpty()) {
                redirectAttributes.addFlashAttribute("sucesso", "Elemento atualizado.");
            } else {
                redirectAttributes.addFlashAttribute("erro", String.join(" | ", resultado.avisos()));
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/elementos/{index}/excluir")
    public String removerElemento(@PathVariable Long id, @PathVariable Long itemId, @PathVariable int index,
                                   RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.removerElemento(id, itemId, index);
            redirectAttributes.addFlashAttribute("sucesso", "Elemento removido.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/furacoes/{index}/excluir")
    public String removerFuracao(@PathVariable Long id, @PathVariable Long itemId, @PathVariable int index,
                                  RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.removerFuracao(id, itemId, index);
            redirectAttributes.addFlashAttribute("sucesso", "Furação removida.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/anotacoes")
    public String adicionarAnotacao(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestParam TipoAnotacao tipo,
                                     @RequestParam BigDecimal x1Mm,
                                     @RequestParam BigDecimal y1Mm,
                                     @RequestParam(required = false) BigDecimal x2Mm,
                                     @RequestParam(required = false) BigDecimal y2Mm,
                                     @RequestParam(required = false) String texto,
                                     RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.adicionarAnotacao(id, itemId, tipo, x1Mm, y1Mm, x2Mm, y2Mm, texto);
            redirectAttributes.addFlashAttribute("sucesso", "Anotação adicionada ao croqui.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/anotacoes/{index}/excluir")
    public String removerAnotacao(@PathVariable Long id, @PathVariable Long itemId, @PathVariable int index,
                                   RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.removerAnotacao(id, itemId, index);
            redirectAttributes.addFlashAttribute("sucesso", "Anotação removida.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/puxador-h")
    public String adicionarPuxadorH(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestParam String lado,
                                     @RequestParam(required = false) BigDecimal tamanhoMm,
                                     @RequestParam(required = false) BigDecimal distanciaBordaMm,
                                     RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.adicionarPuxadorH(id, itemId, lado, tamanhoMm, distanciaBordaMm);
            String tamanhoDescricao = tamanhoMm != null && tamanhoMm.signum() > 0
                    ? tamanhoMm.stripTrailingZeros().toPlainString() + "mm"
                    : "padrão 300mm";
            String bordaDescricao = distanciaBordaMm != null && distanciaBordaMm.signum() > 0
                    ? distanciaBordaMm.stripTrailingZeros().toPlainString() + "mm"
                    : "padrão 300mm";
            redirectAttributes.addFlashAttribute("sucesso",
                    "Puxador H adicionado (lado " + lado.toLowerCase() + ", " + tamanhoDescricao + " entre furos, "
                            + bordaDescricao + " da borda).");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/puxador-simples")
    public String adicionarPuxadorSimples(@PathVariable Long id, @PathVariable Long itemId,
                                           @RequestParam String lado,
                                           RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.adicionarPuxadorSimples(id, itemId, lado);
            redirectAttributes.addFlashAttribute("sucesso", "Puxador simples adicionado (lado " + lado.toLowerCase() + ").");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/fechadura")
    public String adicionarFechadura(@PathVariable Long id, @PathVariable Long itemId,
                                      @RequestParam String lado,
                                      RedirectAttributes redirectAttributes) {
        try {
            planoCorteService.adicionarFechadura(id, itemId, lado);
            redirectAttributes.addFlashAttribute("sucesso", "Fechadura adicionada (lado " + lado.toLowerCase() + ").");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        planoCorteService.finalizar(id);
        redirectAttributes.addFlashAttribute("sucesso", "Plano finalizado.");
        return "redirect:/cut-plans/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        planoCorteService.reabrir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Plano reaberto para edição.");
        return "redirect:/cut-plans/" + id;
    }

    @GetMapping(value = "/{id}/itens/{itemId}/croqui.svg", produces = "image/svg+xml")
    @ResponseBody
    public String croquiSvg(@PathVariable Long id, @PathVariable Long itemId) {
        PlanoCorteItem item = planoCorteService.buscarItem(id, itemId);
        return croquiService.gerarSvg(item);
    }

    @GetMapping(value = "/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {

        PlanoCorte plano = planoCorteService.buscarPorId(id);

        List<PlanoCorteItem> itens =
                planoCorteService.listarItens(id);

        Map<TipoFuracao, Integer> resumoFuracoes =
                planoCorteService.resumoFuracoes(id);

        byte[] pdf = pdfService.gerarPdfPlanoCorte(
                plano,
                itens,
                resumoFuracoes
        );

        String nomeArquivo = pdfService.nomeArquivoPdf(plano);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + nomeArquivo + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void carregarDetalhe(Long id, Model model, PlanoCorteItemForm itemForm, PlanoCorteVaoForm vaoForm) {
        PlanoCorte plano = planoCorteService.buscarPorId(id);
        List<PlanoCorteItem> itens = planoCorteService.listarItens(id);

        Map<Long, String> croquis = new LinkedHashMap<>();
        for (PlanoCorteItem item : itens) {
            croquis.put(item.getId(), croquiService.gerarSvg(item));
        }



        Map<Integer, List<PlanoCorteItem>> itensPorGrupo = new LinkedHashMap<>();
        for (PlanoCorteItem item : itens) {
            if (item.getGrupoVao() != null) {
                itensPorGrupo.computeIfAbsent(item.getGrupoVao(), grupo -> new ArrayList<>()).add(item);
            }
        }

        Map<Integer, String> croquisVao = new LinkedHashMap<>();
        Map<Integer, Long> primeiroItemIdDoGrupo = new LinkedHashMap<>();
        Map<Long, Integer> folhaNumeroPorItem = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<PlanoCorteItem>> entrada : itensPorGrupo.entrySet()) {
            List<PlanoCorteItem> folhas = entrada.getValue();
            primeiroItemIdDoGrupo.put(entrada.getKey(), folhas.get(0).getId());
            for (int i = 0; i < folhas.size(); i++) {
                folhaNumeroPorItem.put(folhas.get(i).getId(), i + 1);
            }




            if (folhas.size() > 1) {
                croquisVao.put(entrada.getKey(), croquiService.gerarSvgVao(folhas));
            }
        }

        boolean usaCalculoAutomatico = planoCorteService.usaCalculoAutomatico(plano.getCategoria());

        model.addAttribute("plano", plano);
        model.addAttribute("itens", itens);
        model.addAttribute("croquis", croquis);
        model.addAttribute("itensPorGrupo", itensPorGrupo);
        model.addAttribute("croquisVao", croquisVao);
        model.addAttribute("primeiroItemIdDoGrupo", primeiroItemIdDoGrupo);
        model.addAttribute("folhaNumeroPorItem", folhaNumeroPorItem);
        model.addAttribute("usaCalculoAutomatico", usaCalculoAutomatico);
        model.addAttribute("resumoFuracoes", planoCorteService.resumoFuracoes(id));
        model.addAttribute("itemForm", itemForm);
        model.addAttribute("vaoForm", vaoForm);
        model.addAttribute("elementoForm", new ElementoTecnicoForm());
        model.addAttribute("vidros", vidroService.listarAtivos());
        model.addAttribute("tiposBorda", TipoBorda.values());
        model.addAttribute("tiposElemento", TipoElemento.valoresSelecionaveis());
        model.addAttribute("referenciasHorizontais", ReferenciaHorizontal.values());
        model.addAttribute("referenciasVerticais", ReferenciaVertical.values());
        model.addAttribute("ferragens", ferragemRepository.findAll());
    }

    private void adicionarListasApoioNovo(Model model) {
        List<WorkOrder> osDisponiveis = planoCorteService.listarOsDisponiveis();
        model.addAttribute("workOrders", osDisponiveis);
        model.addAttribute("categorias", CategoriaServico.values());
    }
}
