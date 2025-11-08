package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.web.viewmodels.DoctorEditViewModel;
import org.hibernate.collection.spi.PersistentCollection;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ModelMapperConfig {

    private final SpecialtyRepository specialtyRepository;

    @Autowired
    public ModelMapperConfig(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setPropertyCondition(context ->
                        !(context.getSource() instanceof PersistentCollection) || ((PersistentCollection) context.getSource()).wasInitialized()
                );

        // --- Converters ---

        Converter<Set<String>, Set<Specialty>> namesToSpecialtiesConverter = context -> {
            Set<String> source = context.getSource();
            if (source == null) return new HashSet<>();
            return source.stream()
                    .map(name -> specialtyRepository.findByName(name)
                            .orElseGet(() -> {
                                Specialty specialty = new Specialty();
                                specialty.setName(name);
                                return specialtyRepository.save(specialty);
                            }))
                    .collect(Collectors.toCollection(HashSet::new));
        };

        Converter<Set<Specialty>, Set<String>> specialtiesToNamesConverter = context -> {
            Set<Specialty> source = context.getSource();
            if (source == null) {
                return Collections.emptySet();
            }
            return source.stream()
                    .map(Specialty::getName)
                    .collect(Collectors.toSet());
        };

        Converter<VisitViewDTO, VisitUpdateDTO> visitViewToUpdateConverter = context -> {
            VisitViewDTO source = context.getSource();
            VisitUpdateDTO destination = new VisitUpdateDTO();
            destination.setId(source.getId());
            destination.setVisitDate(source.getVisitDate());
            destination.setVisitTime(source.getVisitTime());
            destination.setNotes(source.getNotes());
            destination.setStatus(source.getStatus());
            if (source.getPatient() != null) {
                destination.setPatientId(source.getPatient().getId());
            }
            if (source.getDoctor() != null) {
                destination.setDoctorId(source.getDoctor().getId());
            }
            if (source.getDiagnosis() != null) {
                destination.setDiagnosisId(source.getDiagnosis().getId());
            }
            return destination;
        };

        // --- TypeMaps ---

        // Doctor Mappings
        mapper.createTypeMap(Doctor.class, DoctorViewDTO.class)
                .addMappings(m -> {
                    m.using(specialtiesToNamesConverter).map(Doctor::getSpecialties, DoctorViewDTO::setSpecialties);
                    m.map(Doctor::isGeneralPractitioner, DoctorViewDTO::setGeneralPractitioner);
                });

        mapper.createTypeMap(DoctorViewDTO.class, DoctorEditViewModel.class)
                .addMappings(m -> m.map(DoctorViewDTO::isGeneralPractitioner, DoctorEditViewModel::setGeneralPractitioner));

        mapper.createTypeMap(DoctorCreateDTO.class, Doctor.class)
                .addMappings(m -> m.using(namesToSpecialtiesConverter).map(DoctorCreateDTO::getSpecialties, Doctor::setSpecialties));

        mapper.createTypeMap(DoctorUpdateDTO.class, Doctor.class)
                .addMappings(m -> {
                    m.skip(Doctor::setId);
                    m.using(namesToSpecialtiesConverter).map(DoctorUpdateDTO::getSpecialties, Doctor::setSpecialties);
                });

        // Patient Mappings
        mapper.createTypeMap(Patient.class, PatientViewDTO.class)
                .addMappings(m -> {
                    // ModelMapper with STRICT matching should handle direct field mappings by convention.
                    // Explicitly map only derived fields.
                    m.map(src -> src.getGeneralPractitioner().getId(), PatientViewDTO::setGeneralPractitionerId);
                    m.map(src -> src.getGeneralPractitioner().getName(), PatientViewDTO::setGeneralPractitionerName);
                });

        mapper.createTypeMap(PatientCreateDTO.class, Patient.class)
                .addMappings(m -> m.skip(Patient::setId));

        mapper.createTypeMap(PatientUpdateDTO.class, Patient.class)
                .addMappings(m -> m.skip(Patient::setId));

        // Visit Mappings
        mapper.createTypeMap(VisitViewDTO.class, VisitUpdateDTO.class).setConverter(visitViewToUpdateConverter);

        // Treatment and Medicine Mappings
        mapper.createTypeMap(Treatment.class, TreatmentViewDTO.class)
                .addMappings(m -> m.map(src -> src.getVisit().getId(), TreatmentViewDTO::setVisitId));

        mapper.createTypeMap(Medicine.class, MedicineViewDTO.class)
                .addMappings(m -> m.map(src -> src.getTreatment().getId(), MedicineViewDTO::setTreatmentId));

        // SickLeave Mappings
        mapper.createTypeMap(SickLeave.class, SickLeaveViewDTO.class)
                .addMappings(m -> m.map(src -> src.getVisit().getId(), SickLeaveViewDTO::setVisitId));

        return mapper;
    }
}