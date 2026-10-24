package com.tecsup.autosmart.config;

import com.tecsup.autosmart.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/mechanics/**").hasRole("MECANICO")
                        .requestMatchers("/vehicles/**").hasRole("CLIENTE")
                        
                        .requestMatchers(HttpMethod.POST, "/appointments").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/appointments/mine").hasRole("CLIENTE")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e

                        .authenticationEntryPoint((request, response, ex) ->
                                escribirError(response, HttpStatus.UNAUTHORIZED,
                                        "Token ausente, inválido o expirado", request.getRequestURI()))

                        .accessDeniedHandler((request, response, ex) ->
                                escribirError(response, HttpStatus.FORBIDDEN,
                                        "No tienes permisos para acceder a este recurso", request.getRequestURI()))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse response,
                               HttpStatus status, String mensaje, String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), status.value(), mensaje, path));
    }
}