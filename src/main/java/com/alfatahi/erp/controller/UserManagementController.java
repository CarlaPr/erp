package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class UserManagementController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(AppUserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/create")
    public ResponseEntity<?> criarUsuario(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");
        String role     = body.get("role");

        if (username == null || username.isBlank())
            return ResponseEntity.badRequest().body(Map.of("erro", "username é obrigatório"));

        if (password == null || password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("erro", "senha deve ter pelo menos 6 caracteres"));

        if (!"GESTAO".equals(role) && !"VENDAS".equals(role) && !"TECNICO".equals(role))
            return ResponseEntity.badRequest().body(Map.of("erro", "role deve ser GESTAO,VENDAS ou TECNICO"));

        if (userRepository.findByUsername(username).isPresent())
            return ResponseEntity.badRequest().body(Map.of("erro", "Usuário '" + username + "' já existe"));

        AppUser novo = new AppUser();
        novo.setUsername(username);
        novo.setPassword(passwordEncoder.encode(password));
        novo.setRole(role);
        userRepository.save(novo);

        return ResponseEntity.ok(Map.of(
            "mensagem", "Usuário criado com sucesso",
            "username", username,
            "role", role
        ));
    }

    @GetMapping
    public ResponseEntity<?> listarUsuarios() {
        List<Map<String, String>> lista = userRepository.findAll().stream()
            .map(u -> Map.of(
                "username", u.getUsername(),
                "role",     u.getRole()
            ))
            .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{username}/senha")
    public ResponseEntity<?> trocarSenha(@PathVariable String username,
                                         @RequestBody Map<String, String> body) {

        String novaSenha = body.get("novaSenha");
        if (novaSenha == null || novaSenha.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("erro", "nova senha deve ter pelo menos 6 caracteres"));

        return userRepository.findByUsername(username)
            .map(u -> {
                u.setPassword(passwordEncoder.encode(novaSenha));
                userRepository.save(u);
                return ResponseEntity.ok(Map.of("mensagem", "Senha de '" + username + "' atualizada"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{username}/role")
    public ResponseEntity<?> trocarRole(@PathVariable String username,
                                        @RequestBody Map<String, String> body) {

        String novoRole = body.get("novoRole");
        if (!"GESTAO".equals(novoRole) && !"VENDAS".equals(novoRole) && !"TECNICO".equals(novoRole))
            return ResponseEntity.badRequest().body(Map.of("erro", "novo Role deve ser GESTAO ou VENDAS"));

        return userRepository.findByUsername(username)
            .map(u -> {
                u.setRole(novoRole);
                userRepository.save(u);
                return ResponseEntity.ok(Map.of("mensagem", "Perfil de '" + username + "' alterado para " + novoRole));
            })
            .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{username}")
    public ResponseEntity<?> removerUsuario(@PathVariable String username) {
        return userRepository.findByUsername(username)
            .map(u -> {
                userRepository.delete(u);
                return ResponseEntity.ok(Map.of("mensagem", "Usuário '" + username + "' removido"));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
