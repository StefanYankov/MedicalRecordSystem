package nbu.cscb869.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockKeycloakUserSecurityContextFactory.class)
public @interface WithMockKeycloakUser {

    String keycloakId() default "test-user-id";

    String[] authorities() default {"ROLE_USER"};

    String email() default "test.user@example.com";
}
