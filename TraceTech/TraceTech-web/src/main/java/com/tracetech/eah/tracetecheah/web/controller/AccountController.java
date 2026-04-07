package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.dto.ChangePasswordDto;
import com.tracetech.eah.tracetecheah.common.entity.AppUser;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/account")
    public String account(Model model, Authentication auth) {
        AppUser user = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "account/profile";
    }

    @GetMapping("/account/password")
    public String passwordForm(Model model) {
        model.addAttribute("passwordForm", new ChangePasswordDto());
        return "account/password";
    }

    @PostMapping("/account/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordDto passwordForm,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {

        AppUser user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (!passwordEncoder.matches(passwordForm.getCurrentPassword(), user.getPassword())) {
            bindingResult.rejectValue("currentPassword", "invalid", "La contraseña actual no es correcta");
        }

        if (!passwordForm.getNewPassword().equals(passwordForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Las contraseñas no coinciden");
        }

        if (passwordEncoder.matches(passwordForm.getNewPassword(), user.getPassword())) {
            bindingResult.rejectValue("newPassword", "same", "La nueva contraseña no puede ser igual a la actual");
        }

        if (bindingResult.hasErrors()) {
            return "account/password";
        }

        user.setPassword(passwordEncoder.encode(passwordForm.getNewPassword()));
        userRepository.save(user);
        

        redirectAttributes.addFlashAttribute("successMessage", "Contraseña actualizada correctamente.");
        return "redirect:/account";
    }
}