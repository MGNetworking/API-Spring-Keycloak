package com.nutrition.API_nutrition.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class JwtIdentifyResolverTest {

    private final JwtIdentifyResolver resolver = new JwtIdentifyResolver();

    @Test
    void support_shouldReturnTrueWhenObjectIsJwt() {
        Jwt jwt = mock(Jwt.class);
        assertTrue(this.resolver.support(jwt));
    }


    @Test
    void support_shouldReturnFalseWhenObjectIsNotJwt() {
        Object notJwt = new Object();
        assertFalse(this.resolver.support(notJwt));
    }

    @Test
    void resolver_shouldReturnSubClaim() {
        // Arrange
        String subJwt = "123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(subJwt);

        // Act
        Optional<String> sub = resolver.resolver(jwt);

        // Assert
        assertTrue(sub.isPresent());
        assertEquals(subJwt, sub.get());
    }
}