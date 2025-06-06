package com.nutrition.API_nutrition.security;

import jakarta.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static com.nutrition.API_nutrition.controller.UsersController.*;

/**
 * Permet la gestion du Flux de traitement d'une requête.
 * Ces beans sont objets de configuration
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Active les annotations @PreAuthorize, @PostAuthorize,
public class SecurityConfig  {

    /**
     * Lorsqu'une requête HTTP arrive à votre application :
     * <ul>
     *     <li>La requête passe d'abord par la chaîne de filtres de Spring Security
     *     (définie via {@link SecurityFilterChain}).</li>
     *     <li>Pour une API protégée par JWT, le filtre {@code BearerTokenAuthenticationFilter} (fourni par Spring)
     *     intercepte la requête.</li>
     *     <li>Ce filtre extrait le token JWT depuis l'en-tête HTTP {@code Authorization: Bearer [token]}.</li>
     *     <li>Le token est ensuite validé (signature, expiration, audience, etc.) par un {@code JwtDecoder}
     *     configuré automatiquement.</li>
     *     <li>Si le token est valide, les informations qu'il contient (claims) sont converties
     *     en un objet {@code Authentication}
     *         grâce à un {@code JwtAuthenticationConverter} personnalisé
     *         (comme {@code keycloakJwtAuthenticationConverter()}).</li>
     *     <li>Cette {@code Authentication} est placée dans le {@code SecurityContext},
     *     rendant l'utilisateur authentifié dans le reste du traitement.</li>
     *     <li>Spring Security applique ensuite les règles d'autorisation configurées
     *     (via {@code authorizeHttpRequests}).</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthenticationEntryPoint customEntryPoint ) throws Exception {
        log.info("Chargement de SecurityFilterChain...");
        return http
                .csrf(AbstractHttpConfigurer::disable) // Pour une API REST
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // Permet les iframes pour H2
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize

                        // Public
                        .requestMatchers(
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()

                        // /auth pas besoin de token JWT valide à fournir
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // /admin
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // /users
                        .requestMatchers(HttpMethod.PUT, BASE_USERS + REGISTER).hasAuthority("ROLE_USER_REALM")
                        .requestMatchers(HttpMethod.PUT, BASE_USERS + UPDATE_USER).hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.GET, BASE_USERS + GET_USER_ID).hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.DELETE, BASE_USERS + DELETE_USER).hasAuthority("ROLE_USER")

                        // Toutes les autres requêtes nécessitent une authentification donc qu’un JWT valide soit fourni
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customEntryPoint)
                ).build();
    }

    /**
     * Bean de configuration pour le convertisseur d'authentification JWT.
     * <p>
     * Cette méthode configure un {@link JwtAuthenticationConverter} afin d'extraire et de convertir
     * les rôles (authorities) présents dans le token JWT fourni par Keycloak. Elle utilise une
     * implémentation personnalisée {@link KeycloakRoleConverter} pour transformer les rôles
     * du token en autorités Spring Security, notamment en prenant en compte la section
     * {@code resource_access} des claims Keycloak.
     * </p>
     * <p>
     * Cette configuration est essentielle pour que Spring Security puisse correctement reconnaître
     * les rôles attribués aux utilisateurs lors de l'authentification et appliquer les règles
     * d'autorisation associées aux contrôleurs et aux ressources protégées.
     * </p>
     *
     * @return le {@link JwtAuthenticationConverter} configuré avec le convertisseur {@link KeycloakRoleConverter}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.info("Chargement du convertisseur d'authentification JWT ...");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

}
