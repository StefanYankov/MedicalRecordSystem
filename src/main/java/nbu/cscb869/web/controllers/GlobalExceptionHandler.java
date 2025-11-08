package nbu.cscb869.web.controllers;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.PatientInsuranceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Profile({"test", "production"})
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String USER_FRIENDLY_INSURANCE_MESSAGE = "We were unable to confirm that your health insurance has been paid for the last six months. Please verify your status on the official NHIF website.";

    @ExceptionHandler(PatientInsuranceException.class)
    public ModelAndView handlePatientInsuranceException(PatientInsuranceException ex) {
        logger.error("PatientInsuranceException caught: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView();
        mav.setViewName("errors/insurance-invalid");
        mav.addObject("errorMessage", USER_FRIENDLY_INSURANCE_MESSAGE);
        return mav;
    }

    @ExceptionHandler(InvalidInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleInvalidInputException(InvalidInputException ex) {
        logger.warn("Handling InvalidInputException: {}", ex.getMessage());
        return createErrorModelAndView("error", ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleEntityNotFoundException(EntityNotFoundException ex) {
        logger.warn("Handling EntityNotFoundException: {}", ex.getMessage());
        return createErrorModelAndView("error", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Handling AccessDeniedException: {}", ex.getMessage());
        return createErrorModelAndView("error", "You are not authorized to perform this action.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGenericException(Exception ex) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return createErrorModelAndView("error", "An unexpected error occurred. Please try again later.");
    }

    private ModelAndView createErrorModelAndView(String viewName, String message) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName(viewName);
        mav.addObject("errorMessage", message);
        return mav;
    }
}
