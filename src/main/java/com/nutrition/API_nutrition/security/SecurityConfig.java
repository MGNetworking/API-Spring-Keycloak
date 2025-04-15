package com.nutrition.API_nutrition.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permet la gestion du Flux de traitement d'une requête.
 * Ces beans sont objets de configuration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Lorsqu'une requête HTTP arrive à votre application
     * <ul>
     *     <li>La requête passe d'abord par la chaîne de filtres Spring Security (configurée via SecurityFilterChain)</li>
     *     <li>Pour une API sécurisée par JWT, le filtre JwtAuthenticationFilter intercepte la requête</li>
     *     <li>Ce filtre extrait le token JWT de l'en-tête HTTP Authorization: Bearer [token]</li>
     *     <li>Le token est validé (signature, expiration, émetteur)</li>
     *     <li>Si valide, le contenu du token (claims) est extrait et converti en un objet Authentication</li>
     *     <li>Cette Authentication est placée dans le SecurityContext</li>
     *     <li>Les autorisations sont vérifiées par rapport aux règles définies</li>
     *
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
                        .requestMatchers("/h2-console/**", "/api/v1/auth/**").permitAll() // Endpoints d'authentification publics
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * Ce convertisseur fait le travail suivant:
     * <ul>
     *     <li>1. Reçoit un objet Jwt qui contient déjà tous les claims décodés du token</li>
     *     <li>2. Recherche le claim spécifique realm_access qui, dans Keycloak, contient la liste des rôles</li>
     *     <li>3. Extrait la liste des rôles de ce claim</li>
     *     <li>4. Convertit chaque rôle en un objet SimpleGrantedAuthority avec le préfixe ROLE_ (convention Spring Security)</li>
     *     <li>5. Retourne cette liste d'autorités qui sera utilisée pour les vérifications d'autorisation</li>
     * </ul>
     *
     * @return
     */
    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
