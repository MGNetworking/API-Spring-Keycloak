package com.nutrition.API_nutrition.security;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Convertit les rôles Keycloak d'un JWT en objets {@link GrantedAuthority} de Spring Security.
 * Ce convertisseur extrait les rôles de la revendication "resource_access" du JWT, spécifiquement
 * pour le client "API_nutrition_front", et les mappe en autorités Spring Security.
 */
@Slf4j
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * Convertit les rôles d'un token JWT en une collection de {@link GrantedAuthority}.
     * La méthode récupère la revendication "resource_access" du JWT, extrait les rôles
     * associés au client "API_nutrition_front", et convertit chaque rôle en une
     * {@link SimpleGrantedAuthority}. Si aucun rôle n'est trouvé, une collection vide est renvoyée.
     *
     * @param jwt le token JWT contenant les informations de rôle
     * @return une collection de {@link GrantedAuthority} représentant les rôles
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        log.debug("realm_access: {}", resourceAccess);

        if (resourceAccess != null) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("API_nutrition_front");

            List<String> roles = (List<String>) clientAccess.get("roles");

            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            }
        }

        log.debug("Authorities list: {}", authorities);
        return authorities;
    }
}

