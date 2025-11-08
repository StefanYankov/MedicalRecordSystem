package nbu.cscb869.web.controllers;

import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.config.WebConstants;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.viewmodels.TimeSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for handling visit-related web requests, primarily for scheduling and viewing details.
 */
@Controller
@RequestMapping("/visits")
public class VisitSchedulingController {
    private static final Logger logger = LoggerFactory.getLogger(VisitSchedulingController.class);

    private final VisitService visitService;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public VisitSchedulingController(final VisitService visitService, final DoctorService doctorService, final PatientService patientService) {
        this.visitService = visitService;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @GetMapping("/schedule/{doctorId}")
    @PreAuthorize("hasRole('PATIENT')")
    public String showScheduleVisitForm(
            @PathVariable("doctorId") final Long doctorId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            final Model model) {

        logger.debug("GET /visits/schedule/{}: Displaying form for doctor ID {}", doctorId, doctorId);
        DoctorViewDTO doctor = doctorService.getById(doctorId);
        model.addAttribute("doctor", doctor);
        model.addAttribute("visitData", new VisitCreateDTO());

        if (date != null) {
            logger.debug("Date parameter provided: {}. Calculating time slots.", date);
            // Only check for SCHEDULED visits to determine availability
            Set<LocalTime> bookedTimes = visitService.getVisitsByDoctorAndStatusAndDateRange(doctorId, VisitStatus.SCHEDULED, date, date, 0, WebConstants.MAX_PAGE_SIZE)
                    .getContent().stream()
                    .map(VisitViewDTO::getVisitTime)
                    .collect(Collectors.toSet());
            logger.debug("Found {} booked slots for doctor {} on {}", bookedTimes.size(), doctorId, date);

            List<TimeSlot> timeSlots = new ArrayList<>();
            LocalTime slotTime = ValidationConfig.VISIT_START_TIME;
            while (slotTime.isBefore(ValidationConfig.VISIT_END_TIME)) {
                boolean isAvailable = !bookedTimes.contains(slotTime);
                timeSlots.add(new TimeSlot(slotTime, isAvailable));
                slotTime = slotTime.plusMinutes(30);
            }
            logger.debug("Generated {} total time slots.", timeSlots.size());

            model.addAttribute("timeSlots", timeSlots);
            model.addAttribute("selectedDate", date);
        } else {
            logger.debug("No date parameter provided. Displaying initial date selection form.");
        }

        return "visits/schedule";
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasRole('PATIENT')")
    public String processScheduleVisit(@ModelAttribute("visitData") VisitCreateDTO visitCreateDTO, @AuthenticationPrincipal OidcUser principal) {
        logger.debug("POST /visits/schedule: Processing visit scheduling request for doctor ID {}", visitCreateDTO.getDoctorId());
        PatientViewDTO patient = patientService.getByKeycloakId(principal.getSubject());
        logger.debug("Identified patient {} (ID: {}) from security context.", patient.getName(), patient.getId());

        visitCreateDTO.setPatientId(patient.getId());
        logger.debug("Attempting to schedule visit for patient ID {} with doctor ID {} on {} at {}",
                visitCreateDTO.getPatientId(), visitCreateDTO.getDoctorId(), visitCreateDTO.getVisitDate(), visitCreateDTO.getVisitTime());

        VisitViewDTO scheduledVisit = visitService.scheduleNewVisitByPatient(visitCreateDTO);
        logger.info("Successfully scheduled visit with ID {}. Redirecting to details page.", scheduledVisit.getId());

        return "redirect:/visits/" + scheduledVisit.getId();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public String getVisitDetails(@PathVariable("id") final Long id, final Model model) {
        logger.debug("GET /visits/{}: Displaying details for visit ID {}", id, id);
        VisitViewDTO visit = visitService.getById(id);
        model.addAttribute("visit", visit);
        logger.info("Successfully retrieved details for visit ID {}.", id);
        return "visits/details";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public String cancelVisit(@PathVariable("id") final Long id) {
        logger.debug("POST /visits/{}/cancel: Attempting to cancel visit ID {} by patient.", id, id);
        visitService.cancelVisit(id);
        logger.info("Successfully cancelled visit ID {}. Redirecting to medical history.", id);
        return "redirect:/profile/history?visitCancelled=true";
    }
}
