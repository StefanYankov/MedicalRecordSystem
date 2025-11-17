package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.*;
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
    private final DoctorRepository doctorRepository;

    @Autowired
    public ModelMapperConfig(SpecialtyRepository specialtyRepository, DoctorRepository doctorRepository) {
        this.specialtyRepository = specialtyRepository;
        this.doctorRepository = doctorRepository;
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
            if (source == null) return null;
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
            if (source == null) return Collections.emptySet();
            return source.stream()
                    .map(Specialty::getName)
                    .collect(Collectors.toSet());
        };

        Converter<Long, Doctor> gpIdToDoctorConverter = context -> {
            Long gpId = context.getSource();
            if (gpId == null) return null;
            return doctorRepository.findById(gpId).orElse(null);
        };

        Converter<DoctorUpdateDTO, Doctor> doctorUpdateConverter = context -> {
            DoctorUpdateDTO source = context.getSource();
            Doctor destination = context.getDestination();

            if (source.getName() != null) destination.setName(source.getName());
            if (source.getUniqueIdNumber() != null) destination.setUniqueIdNumber(source.getUniqueIdNumber());
            if (source.getIsGeneralPractitioner() != null) destination.setGeneralPractitioner(source.getIsGeneralPractitioner());
            if (source.getIsApproved() != null) destination.setApproved(source.getIsApproved());
            if (source.getSpecialties() != null) {
                Set<Specialty> specialties = source.getSpecialties().stream()
                        .map(name -> specialtyRepository.findByName(name).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
                destination.setSpecialties(specialties);
            }
            return destination;
        };

        Converter<PatientUpdateDTO, Patient> patientUpdateConverter = context -> {
            PatientUpdateDTO source = context.getSource();
            Patient destination = context.getDestination();

            if (source.getName() != null) destination.setName(source.getName());
            if (source.getEgn() != null) destination.setEgn(source.getEgn());
            if (source.getKeycloakId() != null) destination.setKeycloakId(source.getKeycloakId());
            if (source.getLastInsurancePaymentDate() != null) destination.setLastInsurancePaymentDate(source.getLastInsurancePaymentDate());
            if (source.getGeneralPractitionerId() != null) {
                Doctor gp = doctorRepository.findById(source.getGeneralPractitionerId()).orElse(null);
                destination.setGeneralPractitioner(gp);
            }
            return destination;
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

        Converter<SickLeave, SickLeaveViewDTO> sickLeaveToViewConverter = context -> {
            SickLeave source = context.getSource();
            SickLeaveViewDTO destination = new SickLeaveViewDTO();
            destination.setId(source.getId());
            destination.setStartDate(source.getStartDate());
            destination.setDurationDays(source.getDurationDays());
            if (source.getVisit() != null) {
                destination.setVisitId(source.getVisit().getId());
            }
            return destination;
        };

        Converter<Treatment, TreatmentViewDTO> treatmentToViewConverter = context -> {
            Treatment source = context.getSource();
            if (source == null) return null;
            TreatmentViewDTO destination = new TreatmentViewDTO();
            destination.setId(source.getId());
            destination.setDescription(source.getDescription());
            if (source.getVisit() != null) {
                destination.setVisitId(source.getVisit().getId());
            }
            if (source.getMedicines() != null) {
                destination.setMedicines(source.getMedicines().stream()
                        .map(med -> mapper.map(med, MedicineViewDTO.class))
                        .collect(Collectors.toList()));
            }
            return destination;
        };
        
        Converter<TreatmentViewDTO, TreatmentUpdateDTO> treatmentViewToUpdateConverter = context -> {
            TreatmentViewDTO source = context.getSource();
            if (source == null) return null;
            TreatmentUpdateDTO destination = new TreatmentUpdateDTO();
            destination.setId(source.getId());
            destination.setDescription(source.getDescription());
            if (source.getMedicines() != null) {
                destination.setMedicines(source.getMedicines().stream()
                        .map(med -> mapper.map(med, MedicineUpdateDTO.class))
                        .collect(Collectors.toList()));
            }
            return destination;
        };

        Converter<Medicine, MedicineViewDTO> medicineToViewConverter = context -> {
            Medicine source = context.getSource();
            MedicineViewDTO destination = new MedicineViewDTO();
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setDosage(source.getDosage());
            destination.setFrequency(source.getFrequency());
            if (source.getTreatment() != null) {
                destination.setTreatmentId(source.getTreatment().getId());
            }
            return destination;
        };

        Converter<Patient, PatientViewDTO> patientToViewConverter = context -> {
            Patient source = context.getSource();
            PatientViewDTO destination = new PatientViewDTO();
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setEgn(source.getEgn());
            destination.setKeycloakId(source.getKeycloakId());
            destination.setLastInsurancePaymentDate(source.getLastInsurancePaymentDate());
            if (source.getGeneralPractitioner() != null) {
                destination.setGeneralPractitionerId(source.getGeneralPractitioner().getId());
                destination.setGeneralPractitionerName(source.getGeneralPractitioner().getName());
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
                .setConverter(doctorUpdateConverter)
                .addMappings(m -> m.skip(Doctor::setId));


        // Patient Mappings
        mapper.createTypeMap(Patient.class, PatientViewDTO.class).setConverter(patientToViewConverter);

        mapper.createTypeMap(PatientCreateDTO.class, Patient.class)
                .addMappings(m -> {
                    m.skip(Patient::setId);
                    m.using(gpIdToDoctorConverter).map(PatientCreateDTO::getGeneralPractitionerId, Patient::setGeneralPractitioner);
                });

        mapper.createTypeMap(PatientUpdateDTO.class, Patient.class)
                .setConverter(patientUpdateConverter)
                .addMappings(m -> m.skip(Patient::setId));


        // Visit, Treatment, Medicine, SickLeave Mappings
        mapper.createTypeMap(Visit.class, VisitViewDTO.class);
        mapper.createTypeMap(VisitViewDTO.class, VisitUpdateDTO.class).setConverter(visitViewToUpdateConverter);
        mapper.createTypeMap(Treatment.class, TreatmentViewDTO.class).setConverter(treatmentToViewConverter);
        mapper.createTypeMap(TreatmentViewDTO.class, TreatmentUpdateDTO.class).setConverter(treatmentViewToUpdateConverter);
        mapper.createTypeMap(Medicine.class, MedicineViewDTO.class).setConverter(medicineToViewConverter);
        mapper.createTypeMap(SickLeave.class, SickLeaveViewDTO.class).setConverter(sickLeaveToViewConverter);
        mapper.createTypeMap(MedicineViewDTO.class, MedicineUpdateDTO.class);

        return mapper;
    }
}
