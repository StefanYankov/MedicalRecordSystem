package nbu.cscb869.config;

import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final List<String> roleHierarchy;

    public SecurityConfig(PatientService patientService, DoctorService doctorService, List<String> roleHierarchy) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.roleHierarchy = roleHierarchy;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationSuccessHandler customAuthenticationSuccessHandler, LogoutSuccessHandler oidcLogoutSuccessHandler) throws Exception {
        http
                .addFilterAfter(new ProfileCompletionFilter(patientService, doctorService, roleHierarchy), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/welcome", "/logout-success",
                                "/doctors", "/doctors/search",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/error", "/favicon.ico", "/.well-known/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/profile/complete", "/doctor/profile/complete").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customAuthenticationSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper()))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")));
        return http.build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:8080/logout-success");
        return oidcLogoutSuccessHandler;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcAuth) {
                    logger.debug("OIDC UserInfo Claims: {}", oidcAuth.getAttributes());
                    mappedAuthorities.addAll(extractRoles(oidcAuth.getAttributes()));
                } else if (authority instanceof OAuth2UserAuthority oauth2Auth) {
                    logger.debug("OAuth2 UserInfo Claims: {}", oauth2Auth.getAttributes());
                    mappedAuthorities.addAll(extractRoles(oauth2Auth.getAttributes()));
                }
            });

            mappedAuthorities.addAll(authorities);
            logger.info("Authorities after initial extraction: {}", mappedAuthorities);

            Set<String> roles = mappedAuthorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            if (roles.contains("ROLE_ADMIN")) {
                logger.debug("User has ADMIN role. Mapping to ROLE_ADMIN only.");
                mappedAuthorities.removeIf(a -> a.getAuthority().startsWith("ROLE_") && !a.getAuthority().equals("ROLE_ADMIN"));
            } else if (roles.contains("ROLE_DOCTOR")) {
                logger.debug("User has DOCTOR role. Mapping to ROLE_DOCTOR only.");
                mappedAuthorities.removeIf(a -> a.getAuthority().startsWith("ROLE_") && !a.getAuthority().equals("ROLE_DOCTOR"));
            } else if (roles.contains("ROLE_PATIENT")) {
                logger.debug("User has PATIENT role. Mapping to ROLE_PATIENT only.");
                mappedAuthorities.removeIf(a -> a.getAuthority().startsWith("ROLE_") && !a.getAuthority().equals("ROLE_PATIENT"));
            }

            logger.info("Final mapped authorities for web login: {}", mappedAuthorities);
            return mappedAuthorities;
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            logger.debug("JWT Claims: {}", jwt.getClaims());
            Collection<GrantedAuthority> authorities = extractRoles(jwt.getClaims());
            logger.info("Extracted authorities from JWT: {}", authorities);
            return authorities;
        });
        return converter;
    }

    private Collection<GrantedAuthority> extractRoles(Map<String, Object> claims) {
        Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
        if (resourceAccess == null) {
            logger.warn("No 'resource_access' claim found in token.");
            return Collections.emptyList();
        }

        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("medical-record-system");
        if (clientAccess == null) {
            logger.warn("No 'medical-record-system' client access found in 'resource_access' claim.");
            return Collections.emptyList();
        }

        Collection<String> roles = (Collection<String>) clientAccess.get("roles");
        if (roles == null || roles.isEmpty()) {
            logger.warn("No 'roles' found within the 'medical-record-system' client access.");
            return Collections.emptyList();
        }

        logger.info("Found roles in token: {}", roles);
        return roles.stream()
                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
