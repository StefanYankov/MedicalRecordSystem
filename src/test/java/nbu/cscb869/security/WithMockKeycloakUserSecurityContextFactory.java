package nbu.cscb869.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WithMockKeycloakUserSecurityContextFactory implements WithSecurityContextFactory<WithMockKeycloakUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockKeycloakUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        List<GrantedAuthority> authorities = Arrays.stream(customUser.authorities())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(customUser.keycloakId())
                .claim("email", customUser.email())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
        context.setAuthentication(token);
        return context;
    }
}
