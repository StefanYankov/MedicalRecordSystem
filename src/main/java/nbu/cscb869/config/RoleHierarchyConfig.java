package nbu.cscb869.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Configuration for establishing a clear role hierarchy and associated landing pages.
 * This centralizes the logic for role-based redirection, making it easier to manage and extend.
 */
@Configuration
public class RoleHierarchyConfig {

    /**
     * Defines the priority of roles, from highest to lowest.
     * This ordered list is crucial for determining a user's primary role when they have multiple.
     */
    @Bean
    public List<String> roleHierarchy() {
        return List.of(
                "ROLE_ADMIN",
                "ROLE_DOCTOR",
                "ROLE_PATIENT"
        );
    }

    /**
     * Maps high-priority roles to their specific dashboard/landing pages.
     */
    @Bean
    public Map<String, String> roleRedirectMap() {
        return Map.of(
                "ROLE_ADMIN", "/admin",
                "ROLE_DOCTOR", "/dashboard/doctor"
        );
    }
}
