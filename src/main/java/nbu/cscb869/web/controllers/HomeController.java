package nbu.cscb869.web.controllers;

import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private final SpecialtyService specialtyService;
    private final Environment environment;

    public HomeController(SpecialtyService specialtyService, Environment environment) {
        this.specialtyService = specialtyService;
        this.environment = environment;
    }

    @GetMapping("/")
    public String home(Model model) throws ExecutionException, InterruptedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            logger.info("User '{}' has authorities: {}", authentication.getName(), authentication.getAuthorities());
        }

        // Build Keycloak registration URL
        String issuerUri = environment.getProperty("spring.security.oauth2.client.provider.keycloak.issuer-uri");
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.keycloak.client-id");
        String redirectUri = UriComponentsBuilder.fromHttpUrl(environment.getProperty("app.base-url", "http://localhost:8080"))
                .path("/login/oauth2/code/keycloak").toUriString();

        if (issuerUri != null && clientId != null) {
            String registerUrl = UriComponentsBuilder.fromHttpUrl(issuerUri)
                    .path("/protocol/openid-connect/registrations")
                    .queryParam("client_id", clientId)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid email")
                    .queryParam("redirect_uri", redirectUri)
                    .toUriString();
            model.addAttribute("registerUrl", registerUrl);
        }


        CompletableFuture<Page<SpecialtyViewDTO>> specialtiesFuture = specialtyService.getAll(0, 100, "name", true);
        model.addAttribute("specialties", specialtiesFuture.get().getContent());

        return "home";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        logger.info("User successfully logged out. Displaying logout confirmation page.");
        return "logout-success";
    }
}
