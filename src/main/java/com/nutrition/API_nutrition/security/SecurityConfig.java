package com.nutrition.API_nutrition.security;

import jakarta.ws.rs.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permet la gestion du Flux de traitement d'une requête.
 * Ces beans sont objets de configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Active les annotations @PreAuthorize, @PostAuthorize,
public class SecurityConfig {

    /**
     * Lorsqu'une requête HTTP arrive à votre application :
     * <ul>
     *     <li>La requête passe d'abord par la chaîne de filtres de Spring Security (définie via {@link SecurityFilterChain}).</li>
     *     <li>Pour une API protégée par JWT, le filtre {@code BearerTokenAuthenticationFilter} (fourni par Spring) intercepte la requête.</li>
     *     <li>Ce filtre extrait le token JWT depuis l'en-tête HTTP {@code Authorization: Bearer [token]}.</li>
     *     <li>Le token est ensuite validé (signature, expiration, audience, etc.) par un {@code JwtDecoder} configuré automatiquement.</li>
     *     <li>Si le token est valide, les informations qu'il contient (claims) sont converties en un objet {@code Authentication}
     *         grâce à un {@code JwtAuthenticationConverter} personnalisé (comme {@code keycloakJwtAuthenticationConverter()}).</li>
     *     <li>Cette {@code Authentication} est placée dans le {@code SecurityContext}, rendant l'utilisateur authentifié dans le reste du traitement.</li>
     *     <li>Spring Security applique ensuite les règles d'autorisation configurées (via {@code authorizeHttpRequests}).</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Pour une API REST
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // Permet les iframes pour H2
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize

                        // Public
                        .requestMatchers(
                                "/h2-console/**",
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/v1/users/register")
                        .permitAll()

                        // Admin
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // Utilisateur authentifié
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/user").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasAuthority("ROLE_USER")

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(Customizer.withDefaults())
                ).build();
    }
}
