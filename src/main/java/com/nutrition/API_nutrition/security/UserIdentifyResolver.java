package com.nutrition.API_nutrition.security;

import java.util.Optional;

/**
 * Interface définissant un résolveur permettant d'extraire l'identifiant d'un utilisateur
 * à partir d'un objet d'authentification (principal), tel qu'un {@link org.springframework.security.oauth2.jwt.Jwt}
 * ou un {@link org.keycloak.KeycloakPrincipal}, par exemple.
 * <p>
 * Elle est conçue pour être utilisée dans un mécanisme de résolution dynamique,
 * basé sur le type du principal authentifié.
 */
public interface UserIdentifyResolver {

    /**
     * Indique si ce résolveur est capable de traiter le type donné de principal.
     *
     * @param principal l'objet principal issu du contexte d'authentification (ex : Jwt, KeycloakPrincipal, etc.)
     * @return {@code true} si ce résolveur peut gérer ce type d'objet, sinon {@code false}
     */
    boolean support(Object principal);

    /**
     * Extrait l'identifiant de l'utilisateur à partir de l'objet principal fourni.
     * <p>
     * Cette méthode est supposée être appelée uniquement si {@link #support(Object)} a retourné {@code true}.
     *
     * @param principal l'objet principal issu du contexte d'authentification
     * @return Optional<String> l'identifiant de l'utilisateur (par exemple, le champ {@code sub} d'un JWT)
     */
    Optional<String> resolver(Object principal);
}
