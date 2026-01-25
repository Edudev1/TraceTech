package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.entity.AppUser;
import com.tracetech.eah.tracetecheah.common.entity.Role;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/staff/login")
    public String staffLogin() {
        return "auth/staff-login";
    }


    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new AppUser());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") AppUser user,
                           BindingResult bindingResult,
                           Model model) {

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            bindingResult.rejectValue("username", "duplicate", "El usuario ya existe");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        userRepository.save(user);

        return "redirect:/login";
    }
}
