package nbu.cscb869.web.api.controllers;

import jakarta.validation.Valid;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing doctor-related operations.
 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * Constructs a new DoctorController with the specified DoctorService.
     *
     * @param doctorService the service for handling doctor operations
     */
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Creates a new doctor with the provided DTO and optional image.
     *
     * @param dto   the DTO containing doctor creation data
     * @param image the optional image file to upload
     * @return ResponseEntity containing the created DoctorViewDTO
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<DoctorViewDTO> createDoctor(
            @Valid @RequestPart("dto") DoctorCreateDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        DoctorViewDTO doctor = doctorService.create(dto, image);
        return ResponseEntity.ok(doctor);
    }
}