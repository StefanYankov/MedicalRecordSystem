package nbu.cscb869.web.controllers;

import nbu.cscb869.config.WebConstants;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.specifications.DoctorSpecification;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DoctorSearchController {

    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;

    public DoctorSearchController(DoctorService doctorService, SpecialtyService specialtyService) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
    }

    @GetMapping("/doctors")
    public String showAllDoctors(Model model, @RequestParam(required = false) String filter) {
        Specification<Doctor> spec = Specification.where(DoctorSpecification.isApproved())
                                                  .and(DoctorSpecification.fetchSpecialties());

        if (filter != null && !filter.isBlank()) {
            spec = spec.and(Specification.where(DoctorSpecification.nameContains(filter))
                                         .or(DoctorSpecification.specialtyNameContains(filter)));
        }

        Page<DoctorViewDTO> doctors = doctorService.findByCriteria(spec, 0, WebConstants.MAX_PAGE_SIZE, "name", true);
        model.addAttribute("doctors", doctors);
        model.addAttribute("filter", filter);
        return "doctors/list";
    }

    @GetMapping("/doctors/search")
    public String searchDoctorsBySpecialty(@RequestParam(value = "specialtyId", required = false) Long specialtyId, Model model) {
        if (specialtyId == null) {
            return "redirect:/";
        }

        Page<DoctorViewDTO> doctors = doctorService.findAllBySpecialty(
                specialtyId,
                0,
                WebConstants.MAX_PAGE_SIZE,
                "name",
                true
        );

        SpecialtyViewDTO specialty = specialtyService.getById(specialtyId);

        model.addAttribute("specialtyName", specialty.getName());
        model.addAttribute("doctors", doctors.getContent());

        return "doctors/search-results";
    }
}
