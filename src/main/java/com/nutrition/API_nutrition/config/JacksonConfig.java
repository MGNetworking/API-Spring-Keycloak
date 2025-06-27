package com.nutrition.API_nutrition.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * Configuration Jackson pour la sérialisation/désérialisation JSON.
 * <p>
 * Cette classe configure un {@link ObjectMapper} personnalisé pour gérer
 * les types de données temporelles Java 8+ et optimiser la sérialisation JSON.
 * </p>
 *
 * @author Votre nom
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure et fournit un ObjectMapper personnalisé pour l'application.
     * <p>
     * Cette méthode configure l'ObjectMapper avec les paramètres suivants :
     * </p>
     * <ul>
     *   <li>Support des types temporels Java 8+ (LocalDateTime, ZonedDateTime, etc.)</li>
     *   <li>Sérialisation des dates au format ISO-8601 plutôt qu'en timestamps</li>
     *   <li>Inclusion du fuseau horaire dans la sérialisation des dates</li>
     *   <li>Exclusion des champs null des réponses JSON</li>
     * </ul>
     *
     * @return ObjectMapper configuré pour l'application
     * @see JavaTimeModule
     * @see SerializationFeature#WRITE_DATES_AS_TIMESTAMPS
     * @see SerializationFeature#WRITE_DATES_WITH_ZONE_ID
     * @see JsonInclude.Include#NON_NULL
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enregistrer JavaTimeModule pour gérer LocalDateTime, ZonedDateTime, etc.
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);
        // Désactiver la sérialisation des dates comme tableaux
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Activer l'écriture des dates avec le fuseau horaire
        mapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        // Optionnel : ignorer les champs null dans les réponses JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
