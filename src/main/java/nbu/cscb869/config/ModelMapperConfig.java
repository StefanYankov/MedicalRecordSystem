package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.services.data.dtos.*;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // Doctor: Map specialties to specialtyNames
        mapper.typeMap(Doctor.class, DoctorViewDTO.class)
                .addMappings(mapping -> mapping.using(toSpecialtyNamesConverter())
                        .map(Doctor::getSpecialties, DoctorViewDTO::setSpecialtyNames));

        // Patient: Map generalPractitioner to generalPractitionerName
        mapper.typeMap(Patient.class, PatientViewDTO.class)
                .addMappings(mapping -> mapping.using(toGeneralPractitionerNameConverter())
                        .map(Patient::getGeneralPractitioner, PatientViewDTO::setGeneralPractitionerName));

        // SickLeave: Map visit to visitDetails
        mapper.typeMap(SickLeave.class, SickLeaveViewDTO.class)
                .addMappings(mapping -> mapping.using(toVisitDetailsConverter())
                        .map(SickLeave::getVisit, SickLeaveViewDTO::setVisitDetails));

        // Treatment: Map visit to visitDetails, medicines to medicineNames
        mapper.typeMap(Treatment.class, TreatmentViewDTO.class)
                .addMappings(mapping -> {
                    mapping.using(toVisitDetailsConverter())
                            .map(Treatment::getVisit, TreatmentViewDTO::setVisitDetails);
                    mapping.using(toMedicineNamesConverter())
                            .map(Treatment::getMedicines, TreatmentViewDTO::setMedicineNames);
                });

        // Medicine: Map treatment to treatmentDetails
        mapper.typeMap(Medicine.class, MedicineViewDTO.class)
                .addMappings(mapping -> mapping.using(toTreatmentDetailsConverter())
                        .map(Medicine::getTreatment, MedicineViewDTO::setTreatmentDetails));

        return mapper;
    }

    // Converters
    private Converter<Set<Specialty>, Set<String>> toSpecialtyNamesConverter() {
        return context -> {
            Set<Specialty> specialties = context.getSource();
            if (specialties == null) {
                return new HashSet<>();
            }
            Set<String> specialtyNames = specialties.stream()
                    .map(Specialty::getName)
                    .collect(Collectors.toSet());
            return specialtyNames;
        };
    }

    private Converter<Doctor, String> toGeneralPractitionerNameConverter() {
        return context -> {
            Doctor doctor = context.getSource();
            if (doctor == null) {
                return null;
            }
            String doctorName = doctor.getName();
            return doctorName;
        };
    }

    private Converter<Visit, String> toVisitDetailsConverter() {
        return context -> {
            Visit visit = context.getSource();
            if (visit == null) {
                return null;
            }
            String visitDetails = String.format("Visit on %s with %s",
                    visit.getVisitDate(), visit.getDoctor().getName());
            return visitDetails;
        };
    }

    private Converter<List<Medicine>, List<String>> toMedicineNamesConverter() {
        return context -> {
            List<Medicine> medicines = context.getSource();
            if (medicines == null) {
                return new ArrayList<>();
            }
            List<String> medicineNames = medicines.stream()
                    .map(Medicine::getName)
                    .collect(Collectors.toList());
            return medicineNames;
        };
    }

    private Converter<Treatment, String> toTreatmentDetailsConverter() {
        return context -> {
            Treatment treatment = context.getSource();
            if (treatment == null) {
                return null;
            }
            String treatmentDetails = String.format("Treatment for Visit on %s",
                    treatment.getVisit().getVisitDate());
            return treatmentDetails;
        };
    }
}