package nbu.cscb869.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A filter that intercepts requests for authenticated users to ensure they have completed their profile.
 * This filter acts as a secondary check after the CustomAuthenticationSuccessHandler.
 * It prevents users from navigating away from profile completion if they haven't finished it.
 */
public class ProfileCompletionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ProfileCompletionFilter.class);

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final List<String> roleHierarchy;

    public ProfileCompletionFilter(PatientService patientService, DoctorService doctorService, List<String> roleHierarchy) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String requestURI = request.getRequestURI();
            String keycloakId = oidcUser.getSubject();

            if (isEssentialPage(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            Set<String> userRoles = oidcUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            String primaryRole = roleHierarchy.stream()
                    .filter(userRoles::contains)
                    .findFirst()
                    .orElse(null);

            if ("ROLE_PATIENT".equals(primaryRole)) {
                try {
                    patientService.getByKeycloakId(keycloakId);
                } catch (EntityNotFoundException e) {
                    logger.debug("Patient profile not found for user {}. Redirecting to /profile/complete.", keycloakId);
                    response.sendRedirect("/profile/complete");
                    return;
                }
            } else if ("ROLE_DOCTOR".equals(primaryRole)) {
                try {
                    doctorService.getByKeycloakId(keycloakId);
                } catch (EntityNotFoundException e) {
                    logger.debug("Doctor profile not found for user {}. Redirecting to /doctor/profile/complete.", keycloakId);
                    response.sendRedirect("/doctor/profile/complete");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isEssentialPage(String requestURI) {
        return requestURI.equals("/welcome") ||
               requestURI.equals("/profile/complete") ||
               requestURI.equals("/doctor/profile/complete") ||
               requestURI.startsWith("/css") ||
               requestURI.startsWith("/js") ||
               requestURI.startsWith("/images") ||
               requestURI.equals("/logout") ||
               requestURI.equals("/logout-success") ||
               requestURI.equals("/error");
    }
}
