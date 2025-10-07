package com.nelumbo.park.config;

import com.nelumbo.park.config.security.JwtAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public TestSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/partners-rankings/week").hasRole("ADMIN")
                        .requestMatchers("/parking-rankings/week").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                        .requestMatchers("/indicators/**").hasAnyRole("ADMIN", "SOCIO")
                        .requestMatchers("/vehicles/**").hasAnyRole("ADMIN", "SOCIO")
                        .requestMatchers(HttpMethod.GET, "/parkings/**").hasAnyRole("ADMIN", "SOCIO")
                        .requestMatchers(HttpMethod.POST, "/parkings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/parkings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/parkings/**").hasRole("ADMIN")
                        .requestMatchers("/rankings/**").hasAnyRole("ADMIN", "SOCIO")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(403))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(403))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<UserDetails> users = Arrays.asList(
                User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("password"))
                        .roles("USER")
                        .build(),
                User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .roles("ADMIN")
                        .build(),
                User.builder()
                        .username("socio")
                        .password(passwordEncoder.encode("socio"))
                        .roles("SOCIO")
                        .build()
        );
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}