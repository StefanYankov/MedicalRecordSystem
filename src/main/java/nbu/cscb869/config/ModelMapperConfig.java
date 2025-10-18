package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.data.repositories.SpecialtyRepository;
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

        Converter<Doctor, Long> doctorToIdConverter = ctx -> ctx.getSource() == null ? null : ctx.getSource().getId();
        Converter<Visit, Long> visitToIdConverter = ctx -> ctx.getSource() == null ? null : ctx.getSource().getId();
        Converter<Treatment, Long> treatmentToIdConverter = ctx -> ctx.getSource() == null ? null : ctx.getSource().getId();

        // --- TypeMaps ---

        // Doctor Mappings
        mapper.createTypeMap(Doctor.class, DoctorViewDTO.class)
                .addMappings(m -> m.using(specialtiesToNamesConverter).map(Doctor::getSpecialties, DoctorViewDTO::setSpecialties));

        mapper.createTypeMap(DoctorCreateDTO.class, Doctor.class)
                .addMappings(m -> m.using(namesToSpecialtiesConverter).map(DoctorCreateDTO::getSpecialties, Doctor::setSpecialties));

        mapper.createTypeMap(DoctorUpdateDTO.class, Doctor.class)
                .addMappings(m -> {
                    m.skip(Doctor::setId);
                    m.using(namesToSpecialtiesConverter).map(DoctorUpdateDTO::getSpecialties, Doctor::setSpecialties);
                });

        // Patient Mappings
        mapper.createTypeMap(Patient.class, PatientViewDTO.class)
                .addMappings(m -> m.using(doctorToIdConverter).map(Patient::getGeneralPractitioner, PatientViewDTO::setGeneralPractitionerId));

        mapper.createTypeMap(PatientCreateDTO.class, Patient.class)
                .addMappings(m -> m.skip(Patient::setId));

        mapper.createTypeMap(PatientUpdateDTO.class, Patient.class)
                .addMappings(m -> m.skip(Patient::setId));

        // Treatment and Medicine Mappings
        mapper.createTypeMap(Treatment.class, TreatmentViewDTO.class)
                .addMappings(m -> m.using(visitToIdConverter).map(Treatment::getVisit, TreatmentViewDTO::setVisitId));

        mapper.createTypeMap(Medicine.class, MedicineViewDTO.class)
                .addMappings(m -> m.using(treatmentToIdConverter).map(Medicine::getTreatment, MedicineViewDTO::setTreatmentId));

        // SickLeave Mappings
        mapper.createTypeMap(SickLeave.class, SickLeaveViewDTO.class)
                .addMappings(m -> m.using(visitToIdConverter).map(SickLeave::getVisit, SickLeaveViewDTO::setVisitId));

        return mapper;
    }
}