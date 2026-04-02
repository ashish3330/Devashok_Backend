package com.realestate.emi;

import com.realestate.emi.entity.User;
import com.realestate.emi.enums.Role;
import com.realestate.emi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
public class RealestateEmiTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealestateEmiTrackerApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                log.info("Default admin user created: username=admin, password=admin123");
            }
        };
    }
}
