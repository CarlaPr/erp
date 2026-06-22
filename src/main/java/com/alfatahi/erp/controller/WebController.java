package com.alfatahi.erp.controller; // O pacote precisa estar correto no topo!

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // Tem que ser @Controller (NÃO use @RestController aqui)
public class WebController {

    @GetMapping("/")
    public String index() {
        // Redireciona a rota "/" para "/dashboard"
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        return "dashboard"; // O Spring vai procurar o arquivo dashboard.html
    }
}