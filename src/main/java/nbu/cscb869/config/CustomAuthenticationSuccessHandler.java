package nbu.cscb869.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private final PatientService patientService;
    private final DoctorService doctorService;

    public CustomAuthenticationSuccessHandler(PatientService patientService, DoctorService doctorService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        setDefaultTargetUrl("/profile/dashboard"); // Default for patients
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String keycloakId = oidcUser.getSubject();
            logger.debug("Handling successful authentication for user {} with authorities: {}", keycloakId, authentication.getAuthorities());

            if (hasAuthority(authentication, "ROLE_ADMIN")) {
                logger.info("User is an ADMIN. Redirecting to admin dashboard.");
                getRedirectStrategy().sendRedirect(request, response, "/admin/dashboard");
                return;
            }

            try {
                // Check for a doctor profile first
                DoctorViewDTO doctor = doctorService.getByKeycloakId(keycloakId);
                if (doctor.isApproved()) {
                    logger.info("User is an APPROVED DOCTOR. Redirecting to doctor dashboard.");
                    getRedirectStrategy().sendRedirect(request, response, "/doctor/dashboard");
                } else {
                    logger.info("User is a PENDING DOCTOR. Redirecting to pending approval page.");
                    getRedirectStrategy().sendRedirect(request, response, "/doctor/profile/pending");
                }
                return;
            } catch (EntityNotFoundException e) {
                // No doctor profile found, continue to check for patient profile
            }

            try {
                // Check for a patient profile
                patientService.getByKeycloakId(keycloakId);
                logger.info("User is a PATIENT. Redirecting to patient dashboard.");
                super.onAuthenticationSuccess(request, response, authentication); // Use default target URL
            } catch (EntityNotFoundException e) {
                // No patient profile found either
                logger.info("User has no existing profile. Redirecting to welcome page.");
                getRedirectStrategy().sendRedirect(request, response, "/welcome");
            }

        } else {
            logger.warn("Authentication principal is not an OidcUser. Type: {}. Delegating to default handler.", authentication.getPrincipal().getClass().getName());
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
