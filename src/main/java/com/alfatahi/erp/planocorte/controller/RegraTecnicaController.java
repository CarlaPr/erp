package com.alfatahi.erp.planocorte.controller;

import com.alfatahi.erp.planocorte.dto.RegraTecnicaForm;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.DimensaoAjuste;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
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
@RequestMapping("/cut-plans/regras-tecnicas")
public class RegraTecnicaController {

    private final RegraTecnicaService regraTecnicaService;

    public RegraTecnicaController(RegraTecnicaService regraTecnicaService) {
        this.regraTecnicaService = regraTecnicaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("currentPage", "cut-plans-regras");
        model.addAttribute("regras", regraTecnicaService.listarTodas());
        model.addAttribute("regraForm", new RegraTecnicaForm());
        adicionarListasApoio(model);
        return "planocorte/regras-tecnicas";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        RegraTecnica regra = regraTecnicaService.buscarPorId(id);

        RegraTecnicaForm form = new RegraTecnicaForm();
        form.setId(regra.getId());
        form.setCategoria(regra.getCategoria());
        form.setCodigo(regra.getCodigo());
        form.setDescricao(regra.getDescricao());
        form.setDimensao(regra.getDimensao());
        form.setValorMm(regra.getValorMm());

        model.addAttribute("currentPage", "cut-plans-regras");
        model.addAttribute("regras", regraTecnicaService.listarTodas());
        model.addAttribute("regraForm", form);
        model.addAttribute("isEditing", true);
        adicionarListasApoio(model);
        return "planocorte/regras-tecnicas";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("regraForm") RegraTecnicaForm regraForm,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("currentPage", "cut-plans-regras");
            model.addAttribute("regras", regraTecnicaService.listarTodas());
            model.addAttribute("isEditing", regraForm.getId() != null);
            adicionarListasApoio(model);
            return "planocorte/regras-tecnicas";
        }
        try {
            regraTecnicaService.salvar(regraForm);
            redirectAttributes.addFlashAttribute("sucesso", "Regra salva com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível salvar: já existe um código igual nessa categoria.");
        }
        return "redirect:/cut-plans/regras-tecnicas";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        regraTecnicaService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Regra removida.");
        return "redirect:/cut-plans/regras-tecnicas";
    }

    private void adicionarListasApoio(Model model) {
        model.addAttribute("categorias", CategoriaServico.values());
        model.addAttribute("dimensoes", DimensaoAjuste.values());
    }
}
