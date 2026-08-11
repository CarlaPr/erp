package com.alfatahi.erp.planocorte.controller;

import com.alfatahi.erp.planocorte.dto.ParametroServicoForm;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.OrigemParametro;
import com.alfatahi.erp.planocorte.entity.ParametroServico;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cut-plans/parametros-servico")
public class ParametroServicoController {

    private final ParametroServicoService parametroServicoService;

    public ParametroServicoController(ParametroServicoService parametroServicoService) {
        this.parametroServicoService = parametroServicoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("currentPage", "cut-plans-parametros");
        model.addAttribute("parametros", parametroServicoService.listarTodos());
        model.addAttribute("parametroForm", new ParametroServicoForm());
        adicionarListasApoio(model);
        return "planocorte/parametros-servico";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        ParametroServico parametro = parametroServicoService.buscarPorId(id);

        ParametroServicoForm form = new ParametroServicoForm();
        form.setId(parametro.getId());
        form.setCategoria(parametro.getCategoria());
        form.setCodigo(parametro.getCodigo());
        form.setDescricao(parametro.getDescricao());
        form.setValor(parametro.getValor());
        form.setValorTexto(parametro.getValorTexto());
        form.setOrigem(parametro.getOrigem());

        model.addAttribute("currentPage", "cut-plans-parametros");
        model.addAttribute("parametros", parametroServicoService.listarTodos());
        model.addAttribute("parametroForm", form);
        model.addAttribute("isEditing", true);
        adicionarListasApoio(model);
        return "planocorte/parametros-servico";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("parametroForm") ParametroServicoForm form,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans-parametros");
            model.addAttribute("parametros", parametroServicoService.listarTodos());
            model.addAttribute("isEditing", form.getId() != null);
            adicionarListasApoio(model);
            return "planocorte/parametros-servico";
        }
        try {
            parametroServicoService.salvar(form);
            redirectAttributes.addFlashAttribute("sucesso", "Parâmetro salvo com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível salvar: já existe um código igual nessa categoria.");
        }
        return "redirect:/cut-plans/parametros-servico";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        parametroServicoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Parâmetro removido.");
        return "redirect:/cut-plans/parametros-servico";
    }

    private void adicionarListasApoio(Model model) {
        model.addAttribute("categorias", CategoriaServico.values());
        model.addAttribute("origens", OrigemParametro.values());
    }
}
