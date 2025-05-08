package nbu.cscb869.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 with Keycloak")
                                .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
                                        .authorizationCode(new io.swagger.v3.oas.models.security.OAuthFlow()
                                                .authorizationUrl("http://localhost:8081/realms/medical-realm/protocol/openid-connect/auth")
                                                .tokenUrl("http://localhost:8081/realms/medical-realm/protocol/openid-connect/token")
                                                .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                        .addString("openid", "OpenID Connect scope"))))))
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Medical Record System API")
                        .version("0.0.1-SNAPSHOT"));
    }
}