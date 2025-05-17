package com.nutrition.API_nutrition.security;

import org.junit.jupiter.api.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class KeycloakIdentifyResolverTest {

    private final KeycloakIdentifyResolver resolver = new KeycloakIdentifyResolver();

    @Test
    void support_shouldReturnTrueWhenObjectIsKeycloakPrincipal() {
        KeycloakPrincipal<?> kc = mock(KeycloakPrincipal.class);
        assertTrue(this.resolver.support(kc));
    }

    @Test
    void support_shouldReturnTrueWhenObjectIsNotKeycloakPrincipal() {
        Object notKeycloakPrincipal = new Object();
        assertFalse(this.resolver.support(notKeycloakPrincipal));
    }

    @Test
    void resolver_shouldReturnTrueWhenObjectIsKeycloakPrincipal() {

        // Arrange
        String mockSubject = "subject";

        // Mocks
        KeycloakPrincipal<KeycloakSecurityContext> kcPrincipal = mock(KeycloakPrincipal.class);
        KeycloakSecurityContext securityContext = mock(KeycloakSecurityContext.class);
        AccessToken accessToken = mock(AccessToken.class);

        // Stubbing
        when(kcPrincipal.getKeycloakSecurityContext()).thenReturn(securityContext);
        when(securityContext.getToken()).thenReturn(accessToken);
        when(accessToken.getSubject()).thenReturn(mockSubject);

        // Act
        Optional<String> sub = resolver.resolver(kcPrincipal);

        // Assert
        assertTrue(sub.isPresent());
        assertEquals(mockSubject, sub.get());

    }

}