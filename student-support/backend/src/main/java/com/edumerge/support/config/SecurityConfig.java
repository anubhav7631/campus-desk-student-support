package com.edumerge.support.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(
            PasswordEncoder encoder,
            @Value("${DEMO_STUDENT_PASSWORD:student123}") String studentPassword,
            @Value("${DEMO_STAFF_PASSWORD:staff123}") String staffPassword,
            @Value("${DEMO_MANAGER_PASSWORD:manager123}") String managerPassword) {

        return new InMemoryUserDetailsManager(
            User.withUsername("student1")
                .password(encoder.encode(studentPassword))
                .roles("STUDENT")
                .build(),

            User.withUsername("student2")
                .password(encoder.encode(studentPassword))
                .roles("STUDENT")
                .build(),

            User.withUsername("staff1")
                .password(encoder.encode(staffPassword))
                .roles("STAFF")
                .build(),

            User.withUsername("staff2")
                .password(encoder.encode(staffPassword))
                .roles("STAFF")
                .build(),

            User.withUsername("manager")
                .password(encoder.encode(managerPassword))
                .roles("MANAGER")
                .build()
        );
    }

    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            @Value("${FRONTEND_ORIGIN:http://localhost:5173}") String frontendOrigin)
            throws Exception {

        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(frontendOrigin));
                config.setAllowedMethods(
                    List.of("GET", "POST", "PATCH", "OPTIONS")
                );
                config.setAllowedHeaders(
                    List.of("Authorization", "Content-Type")
                );
                return config;
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}