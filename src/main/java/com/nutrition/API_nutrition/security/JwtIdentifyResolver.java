package com.nutrition.API_nutrition.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class JwtIdentifyResolver implements UserIdentifyResolver {
    @Override
    public boolean support(Object principal) {
        return principal instanceof Jwt;
    }

    @Override
    public Optional<String> resolver(Object principal) {

        if (principal == null) {
            return Optional.empty();
        }

        Jwt jwt = (Jwt) principal;
        String sub = jwt.getClaimAsString("sub");
        log.info("JWT subject: {}", sub);

        return Optional.of(sub);
    }
}
