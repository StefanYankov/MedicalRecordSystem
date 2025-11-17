package nbu.cscb869.web.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, WebRequest webRequest, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        String layout = "layouts/main-layout";
        String errorTitle = "Error";
        String errorMessage = "An unexpected error has occurred.";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                layout = "layouts/admin-layout";
            } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
                layout = "layouts/doctor-layout";
            } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
                layout = "layouts/patient-layout";
            }
        }

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
            model.addAttribute("statusCode", statusCode);
            model.addAttribute("error", httpStatus.getReasonPhrase());

            errorMessage = switch (statusCode) {
                case 403 -> {
                    errorTitle = "Access Denied";
                    yield "You do not have permission to access this page.";
                }
                case 404 -> {
                    errorTitle = "Page Not Found";
                    yield "The page you are looking for does not exist.";
                }
                case 500 -> {
                    errorTitle = "Internal Server Error";
                    yield "An unexpected error occurred on our end. Please try again later.";
                }
                default -> {
                    errorTitle = "Something Went Wrong";
                    yield "An unexpected error has occurred.";
                }
            };
        }

        // Add stack trace for development
        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
        if (errorDetails.containsKey("trace")) {
            model.addAttribute("stackTrace", errorDetails.get("trace"));
        }

        model.addAttribute("layout", layout);
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);

        logger.error("CustomErrorController handled error: URI [{}], Status [{}], Exception [{}]",
                requestUri, statusCode, (exception != null ? exception.getClass().getName() : "N/A"));

        return "error";
    }
}
