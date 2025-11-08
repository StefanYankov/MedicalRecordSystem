package nbu.cscb869.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class KeycloakAdminConfig {

    private final ClientRegistration clientRegistration;

    /**
     * Constructs the configuration by finding the specific OAuth2 client registration
     * for Keycloak, which is defined in application.yml.
     * @param clientRegistrationRepository The repository holding all configured OAuth2 clients.
     */
    public KeycloakAdminConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak");
    }

    /**
     * Creates a Keycloak admin client bean that can be used to interact with the Keycloak Admin API.
     * It uses the modern Spring Security OAuth2 client configuration as the single source of truth.
     * @return A configured Keycloak admin client.
     */
    @Bean
    public Keycloak keycloak() {
        String issuerUri = clientRegistration.getProviderDetails().getIssuerUri();
        // The serverUrl is the issuerUri without the /realms/{realm} part
        String serverUrl = issuerUri.substring(0, issuerUri.lastIndexOf("/realms"));
        // The realm is the last part of the issuerUri path
        String realm = issuerUri.substring(issuerUri.lastIndexOf("/") + 1);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientRegistration.getClientId())
                .clientSecret(clientRegistration.getClientSecret())
                .grantType("client_credentials") // This grant type is essential for machine-to-machine communication
                .build();
    }
}
