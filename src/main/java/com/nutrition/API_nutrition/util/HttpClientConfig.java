package com.nutrition.API_nutrition.util;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Component
public class HttpClientConfig {

    /**
     * Crée une requête d'authentification
     *
     * @param url    URL de la requête
     * @param header Type de contenu (ex: "application/x-www-form-urlencoded")
     * @param body   Corps de la requête
     * @return La requête HTTP prête à être envoyée
     */
    public HttpRequest postRequest(String url, String header, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", header)
                .POST(HttpRequest.BodyPublishers.ofString(body
                )).build();
    }

    /**
     * Crée une requête GET avec un token d'authentification
     *
     * @param url   URL de la ressource
     * @param token Token d'accès de l'utilisateur
     * @return La requête HTTP prête à être envoyée
     */
    public HttpRequest getRequest(String url, String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }
}
