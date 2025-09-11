package com.nelumbo.park.config;

import com.nelumbo.park.config.security.JwtAuthenticationFilter;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

    @Autowired(required = false)
    private JwtService jwtService;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // Se añaden dos argumentos String vacíos para applicationJson y applicationJsonCharset
        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(jwtService, userRepository, "", "");

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/users/**").hasAuthority("ADMIN")
                        .requestMatchers("/parkings/**").hasAnyAuthority("ADMIN", "SOCIO")
                        .requestMatchers("/parking-rankings/**").hasAuthority("ADMIN")
                        .requestMatchers("/partners-rankings/**").hasAuthority("ADMIN")
                        .requestMatchers("/rankings/**").hasAnyAuthority("ADMIN", "SOCIO")
                        .requestMatchers("/vehicles/**").hasAnyAuthority("ADMIN", "SOCIO")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
