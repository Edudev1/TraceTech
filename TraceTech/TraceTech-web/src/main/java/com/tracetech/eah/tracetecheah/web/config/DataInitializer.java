package com.tracetech.eah.tracetecheah.web.config;

import com.tracetech.eah.tracetecheah.common.entity.AppUser;
import com.tracetech.eah.tracetecheah.common.entity.Role;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("adminedu").isEmpty()) {
                AppUser admin = new AppUser();
                admin.setFirstName("Eduardo");
                admin.setLastName("Admin");
                admin.setPhone("600000001");
                admin.setUsername("adminedu");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
            }

            if (userRepository.findByUsername("tech").isEmpty()) {
                AppUser tech = new AppUser();
                tech.setFirstName("Tech");
                tech.setLastName("Support");
                tech.setPhone("600000002");
                tech.setUsername("tech");
                tech.setPassword(passwordEncoder.encode("tech123"));
                tech.setRole(Role.TECH);
                tech.setEnabled(true);
                userRepository.save(tech);
            }
        };
    }
}