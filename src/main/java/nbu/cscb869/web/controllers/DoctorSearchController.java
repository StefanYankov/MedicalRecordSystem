package nbu.cscb869.web.controllers;

import nbu.cscb869.config.WebConstants;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.springframework.data.domain.Page;
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

    @GetMapping("/doctors/search")
    public String searchDoctorsBySpecialty(@RequestParam("specialtyId") Long specialtyId, Model model) {
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
