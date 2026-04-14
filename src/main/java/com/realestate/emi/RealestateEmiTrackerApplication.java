package com.realestate.emi;

import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.Role;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class RealestateEmiTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealestateEmiTrackerApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Organization defaultOrg = organizationRepository.findByCode("DEVASHOK")
                    .orElseGet(() -> {
                        Organization org = Organization.builder()
                                .name("DevAshok Enclave")
                                .code("DEVASHOK")
                                .address("Plot No. 42, Sector 18, Navi Mumbai")
                                .phone("+919876543210")
                                .email("admin@devashokenclave.in")
                                .reraNumber("MH/NAVI/2021/00342")
                                .isActive(true)
                                .build();
                        organizationRepository.save(org);
                        log.info("Default organization created: {}", org.getName());
                        return org;
                    });

            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                admin.setOrganization(defaultOrg);
                userRepository.save(admin);
                log.info("Default admin user created: username=admin, password=admin123");
            }
        };
    }
}
