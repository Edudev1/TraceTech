package com.tracetech.eah.tracetecheah.web.config;

import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ SOLO 1 UserDetailsService (combinado)
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository, PasswordEncoder encoder) {

        // Internos (admin/tech) en memoria
        UserDetails admin = User.withUsername("adminedu")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails tech = User.withUsername("tech")
                .password(encoder.encode("tech123"))
                .roles("TECH")
                .build();

        InMemoryUserDetailsManager inMemory = new InMemoryUserDetailsManager(admin, tech);

        return username -> {
            // 1) Primero internos
            try {
                return inMemory.loadUserByUsername(username);
            } catch (UsernameNotFoundException ex) {
                // 2) Si no está en memoria -> BD (clientes)
                return userRepository.findByUsername(username)
                        .map(u -> User.withUsername(u.getUsername())
                                .password(u.getPassword())     // BCrypt guardado en BD
                                .roles(u.getRole().name())     // CLIENT
                                .disabled(!u.isEnabled())
                                .build())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // públicos
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/login", "/staff/login", "/register").permitAll()

                // staff-only
                .requestMatchers("/tickets/*/edit").hasAnyRole("ADMIN", "TECH")
                .requestMatchers("/tickets/*/status", "/tickets/*/assign", "/tickets/*/assign-me")
                .hasAnyRole("ADMIN", "TECH")

                // todo lo demás de tickets requiere login
                .requestMatchers("/tickets/**").authenticated()

                .anyRequest().permitAll()
        );


        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/tickets", true)
                .failureUrl("/login?error")
                .permitAll()
        );

        http.logout(Customizer.withDefaults());

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
