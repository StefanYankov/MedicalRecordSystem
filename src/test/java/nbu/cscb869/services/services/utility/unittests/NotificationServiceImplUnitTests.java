package nbu.cscb869.services.services.utility.unittests;

import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.utility.NotificationServiceImpl;
import nbu.cscb869.services.services.utility.contracts.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplUnitTests {

    @Mock
    private EmailService emailService;

    @Mock
    private TemplateEngine templateEngine;

    private NotificationServiceImpl notificationService;

    private VisitViewDTO testVisitViewDTO;
    private String testPatientEmail;

    @BeforeEach
    void SetUp() {
        notificationService = new NotificationServiceImpl(emailService, templateEngine);

        // Initialize common test data
        PatientViewDTO patientViewDTO = new PatientViewDTO();
        patientViewDTO.setName("John Doe");

        DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
        doctorViewDTO.setName("Dr. Smith");

        testVisitViewDTO = new VisitViewDTO();
        testVisitViewDTO.setId(1L);
        testVisitViewDTO.setPatient(patientViewDTO);
        testVisitViewDTO.setDoctor(doctorViewDTO);
        testVisitViewDTO.setVisitDate(LocalDate.of(2023, 1, 15));
        testVisitViewDTO.setVisitTime(LocalTime.of(10, 30));

        testPatientEmail = "john.doe@example.com";
    }

    @Test
    void SendVisitConfirmation_WithValidDataAndEmail_ShouldSendHtmlEmail_HappyPath() {
        // Arrange
        String expectedHtmlBody = "<html><body>Visit Confirmation</body></html>";
        when(templateEngine.process(eq("email/visit-confirmation"), any(Context.class)))
                .thenReturn(expectedHtmlBody);

        // Act
        notificationService.sendVisitConfirmation(testVisitViewDTO, testPatientEmail);

        // Assert
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/visit-confirmation"), contextCaptor.capture());
        verify(emailService).sendHtmlMessage(testPatientEmail, "Visit Confirmation for " + testVisitViewDTO.getVisitDate(), expectedHtmlBody);

        Context capturedContext = contextCaptor.getValue();
        assertEquals(testVisitViewDTO.getPatient().getName(), capturedContext.getVariable("patientName"));
        assertEquals(testVisitViewDTO.getDoctor().getName(), capturedContext.getVariable("doctorName"));
        assertEquals(testVisitViewDTO.getVisitDate(), capturedContext.getVariable("visitDate"));
        assertEquals(testVisitViewDTO.getVisitTime(), capturedContext.getVariable("visitTime"));
    }

    @Test
    void SendVisitConfirmation_WithNullPatientEmail_ShouldNotSendEmail_ErrorCase() {
        // Arrange
        String nullPatientEmail = null;

        // Act
        notificationService.sendVisitConfirmation(testVisitViewDTO, nullPatientEmail);

        // Assert
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(emailService);
    }

    @Test
    void SendVisitConfirmation_WithBlankPatientEmail_ShouldNotSendEmail_ErrorCase() {
        // Arrange
        String blankPatientEmail = " ";

        // Act
        notificationService.sendVisitConfirmation(testVisitViewDTO, blankPatientEmail);

        // Assert
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(emailService);
    }

    @Test
    void SendVisitConfirmation_WhenTemplateEngineFails_ShouldLogAndNotThrow_ErrorCase() {
        // Arrange
        when(templateEngine.process(eq("email/visit-confirmation"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing error"));

        // Act & Assert (expect no exception to be thrown, but error to be logged internally)
        notificationService.sendVisitConfirmation(testVisitViewDTO, testPatientEmail);

        verify(templateEngine).process(eq("email/visit-confirmation"), any(Context.class));
        verifyNoInteractions(emailService); // Email service should not be called if template fails
    }

    @Test
    void SendVisitConfirmation_WhenEmailServiceFails_ShouldLogAndNotThrow_ErrorCase() {
        // Arrange
        String expectedHtmlBody = "<html><body>Visit Confirmation</body></html>";
        when(templateEngine.process(eq("email/visit-confirmation"), any(Context.class)))
                .thenReturn(expectedHtmlBody);
        doThrow(new RuntimeException("Email sending failed"))
                .when(emailService).sendHtmlMessage(anyString(), anyString(), anyString());

        // Act & Assert (expect no exception to be thrown, but error to be logged internally)
        notificationService.sendVisitConfirmation(testVisitViewDTO, testPatientEmail);

        verify(templateEngine).process(eq("email/visit-confirmation"), any(Context.class));
        verify(emailService).sendHtmlMessage(testPatientEmail, "Visit Confirmation for " + testVisitViewDTO.getVisitDate(), expectedHtmlBody);
        // The service catches the exception, so no exception is rethrown here.
    }

    @Test
    void SendVisitConfirmation_WithNullPatientInVisitDTO_ShouldNotSendEmail_EdgeCase() {
        // Arrange
        testVisitViewDTO.setPatient(null);

        // Act
        notificationService.sendVisitConfirmation(testVisitViewDTO, testPatientEmail);

        // Assert
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(emailService);
    }

    @Test
    void SendVisitConfirmation_WithNullDoctorInVisitDTO_ShouldNotSendEmail_EdgeCase() {
        // Arrange
        testVisitViewDTO.setDoctor(null);

        // Act
        notificationService.sendVisitConfirmation(testVisitViewDTO, testPatientEmail);

        // Assert
        // This case will likely throw a NullPointerException when accessing getDoctor().getName() in the service
        // unless the service has null checks. Let's assume the service should handle this gracefully
        // or that the DTO is always valid. For now, we'll verify no email is sent.
        // If the service were to throw, this test would fail, indicating a missing null check.
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(emailService);
    }
}
