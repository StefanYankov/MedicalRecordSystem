package nbu.cscb869.web.controllers.doctor;

import jakarta.validation.Valid;
import nbu.cscb869.services.data.dtos.VisitDocumentationDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.VisitService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class VisitDocumentationController {

    private final VisitService visitService;
    private final DiagnosisService diagnosisService;

    public VisitDocumentationController(VisitService visitService, DiagnosisService diagnosisService) {
        this.visitService = visitService;
        this.diagnosisService = diagnosisService;
    }

    @GetMapping("/visit/document/{id}")
    @PreAuthorize("hasRole('DOCTOR') and @visitServiceImpl.getById(#id).doctor.keycloakId == authentication.name")
    public String showDocumentVisitForm(@PathVariable("id") Long id, Model model) throws ExecutionException, InterruptedException {
        VisitViewDTO visit = visitService.getById(id);
        
        VisitDocumentationDTO documentationDTO = new VisitDocumentationDTO();
        documentationDTO.setVisitId(id);

        model.addAttribute("visit", visit);
        model.addAttribute("documentation", documentationDTO);
        model.addAttribute("allDiagnoses", diagnosisService.getAll(0, 100, "name", true, null).get().getContent());

        return "visit/document";
    }

    @PostMapping("/visit/document/{id}")
    @PreAuthorize("hasRole('DOCTOR') and @visitServiceImpl.getById(#id).doctor.keycloakId == authentication.name")
    public String documentVisit(@PathVariable("id") Long id, 
                              @Valid @ModelAttribute("documentation") VisitDocumentationDTO documentationDTO, 
                              BindingResult bindingResult, Model model) throws ExecutionException, InterruptedException {
        if (bindingResult.hasErrors()) {
            VisitViewDTO visit = visitService.getById(id);
            model.addAttribute("visit", visit);
            model.addAttribute("allDiagnoses", diagnosisService.getAll(0, 100, "name", true, null).get().getContent());
            return "visit/document";
        }

        visitService.documentVisit(id, documentationDTO);
        return "redirect:/schedule";
    }
}
