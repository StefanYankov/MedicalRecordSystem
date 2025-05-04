package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.services.data.dtos.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setImplicitMappingEnabled(false)
                .setDeepCopyEnabled(false);

        configureDiagnosisMappings(modelMapper);
        configurePatientMappings(modelMapper);
        configureSickLeaveMappings(modelMapper);
        configureDoctorMappings(modelMapper);
        configureVisitMappings(modelMapper);
        configureTreatmentMappings(modelMapper);
        configureMedicineMappings(modelMapper);
        configureSpecialtyMappings(modelMapper);

        return modelMapper;
    }

    private void configureDiagnosisMappings(ModelMapper modelMapper) {
        TypeMap<DiagnosisCreateDTO, Diagnosis> diagnosisCreateMap = modelMapper.typeMap(DiagnosisCreateDTO.class, Diagnosis.class)
                .setProvider(req -> new Diagnosis());
        diagnosisCreateMap.addMappings(mapper -> {
            mapper.map(DiagnosisCreateDTO::getName, Diagnosis::setName);
            mapper.map(DiagnosisCreateDTO::getDescription, Diagnosis::setDescription);
        });

        TypeMap<DiagnosisUpdateDTO, Diagnosis> diagnosisUpdateMap = modelMapper.typeMap(DiagnosisUpdateDTO.class, Diagnosis.class)
                .setProvider(req -> new Diagnosis());
        diagnosisUpdateMap.addMappings(mapper -> {
            mapper.map(DiagnosisUpdateDTO::getId, Diagnosis::setId);
            mapper.map(DiagnosisUpdateDTO::getName, Diagnosis::setName);
            mapper.map(DiagnosisUpdateDTO::getDescription, Diagnosis::setDescription);
        });

        TypeMap<Diagnosis, DiagnosisViewDTO> diagnosisViewMap = modelMapper.typeMap(Diagnosis.class, DiagnosisViewDTO.class)
                .setProvider(req -> new DiagnosisViewDTO());
        diagnosisViewMap.addMappings(mapper -> {
            mapper.map(Diagnosis::getId, DiagnosisViewDTO::setId);
            mapper.map(Diagnosis::getName, DiagnosisViewDTO::setName);
            mapper.map(Diagnosis::getDescription, DiagnosisViewDTO::setDescription);
        });
    }

    private void configurePatientMappings(ModelMapper modelMapper) {
        TypeMap<PatientCreateDTO, Patient> patientCreateMap = modelMapper.typeMap(PatientCreateDTO.class, Patient.class)
                .setProvider(req -> new Patient());
        patientCreateMap.addMappings(mapper -> {
            mapper.map(PatientCreateDTO::getName, Patient::setName);
            mapper.map(PatientCreateDTO::getEgn, Patient::setEgn);
            mapper.map(PatientCreateDTO::getLastInsurancePaymentDate, Patient::setLastInsurancePaymentDate);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Doctor doctor = new Doctor();
                doctor.setId(id);
                return doctor;
            }).map(PatientCreateDTO::getGeneralPractitionerId, Patient::setGeneralPractitioner);
        });

        TypeMap<PatientUpdateDTO, Patient> patientUpdateMap = modelMapper.typeMap(PatientUpdateDTO.class, Patient.class)
                .setProvider(req -> new Patient());
        patientUpdateMap.addMappings(mapper -> {
            mapper.map(PatientUpdateDTO::getId, Patient::setId);
            mapper.map(PatientUpdateDTO::getName, Patient::setName);
            mapper.map(PatientUpdateDTO::getLastInsurancePaymentDate, Patient::setLastInsurancePaymentDate);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Doctor doctor = new Doctor();
                doctor.setId(id);
                return doctor;
            }).map(PatientUpdateDTO::getGeneralPractitionerId, Patient::setGeneralPractitioner);
        });

        TypeMap<Patient, PatientViewDTO> patientViewMap = modelMapper.typeMap(Patient.class, PatientViewDTO.class)
                .setProvider(req -> new PatientViewDTO());
        patientViewMap.addMappings(mapper -> {
            mapper.map(Patient::getId, PatientViewDTO::setId);
            mapper.map(Patient::getName, PatientViewDTO::setName);
            mapper.map(Patient::getEgn, PatientViewDTO::setEgn);
            mapper.map(Patient::getLastInsurancePaymentDate, PatientViewDTO::setLastInsurancePaymentDate);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), DoctorViewDTO.class) : null)
                    .map(Patient::getGeneralPractitioner, PatientViewDTO::setGeneralPractitioner);
        });
    }

    private void configureSickLeaveMappings(ModelMapper modelMapper) {
        TypeMap<SickLeaveCreateDTO, SickLeave> sickLeaveCreateMap = modelMapper.typeMap(SickLeaveCreateDTO.class, SickLeave.class)
                .setProvider(req -> new SickLeave());
        sickLeaveCreateMap.addMappings(mapper -> {
            mapper.map(SickLeaveCreateDTO::getStartDate, SickLeave::setStartDate);
            mapper.map(SickLeaveCreateDTO::getDurationDays, SickLeave::setDurationDays);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Visit visit = new Visit();
                visit.setId(id);
                return visit;
            }).map(SickLeaveCreateDTO::getVisitId, SickLeave::setVisit);
        });

        TypeMap<SickLeaveUpdateDTO, SickLeave> sickLeaveUpdateMap = modelMapper.typeMap(SickLeaveUpdateDTO.class, SickLeave.class)
                .setProvider(req -> new SickLeave());
        sickLeaveUpdateMap.addMappings(mapper -> {
            mapper.map(SickLeaveUpdateDTO::getId, SickLeave::setId);
            mapper.map(SickLeaveUpdateDTO::getStartDate, SickLeave::setStartDate);
            mapper.map(SickLeaveUpdateDTO::getDurationDays, SickLeave::setDurationDays);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Visit visit = new Visit();
                visit.setId(id);
                return visit;
            }).map(SickLeaveUpdateDTO::getVisitId, SickLeave::setVisit);
        });

        TypeMap<SickLeave, SickLeaveViewDTO> sickLeaveViewMap = modelMapper.typeMap(SickLeave.class, SickLeaveViewDTO.class)
                .setProvider(req -> new SickLeaveViewDTO());
        sickLeaveViewMap.addMappings(mapper -> {
            mapper.map(SickLeave::getId, SickLeaveViewDTO::setId);
            mapper.map(SickLeave::getStartDate, SickLeaveViewDTO::setStartDate);
            mapper.map(SickLeave::getDurationDays, SickLeaveViewDTO::setDurationDays);
            mapper.using(ctx -> ctx.getSource() != null ? ((Visit) ctx.getSource()).getId() : null)
                    .map(SickLeave::getVisit, SickLeaveViewDTO::setVisitId);
        });
    }

    private void configureDoctorMappings(ModelMapper modelMapper) {
        TypeMap<DoctorCreateDTO, Doctor> doctorCreateMap = modelMapper.typeMap(DoctorCreateDTO.class, Doctor.class)
                .setProvider(req -> new Doctor());
        doctorCreateMap.addMappings(mapper -> {
            mapper.map(DoctorCreateDTO::getName, Doctor::setName);
            mapper.map(DoctorCreateDTO::getUniqueIdNumber, Doctor::setUniqueIdNumber);
            mapper.map(DoctorCreateDTO::isGeneralPractitioner, Doctor::setGeneralPractitioner);
            mapper.map(DoctorCreateDTO::getImageUrl, Doctor::setImageUrl);
            mapper.skip(Doctor::setSpecialties);
            mapper.skip(Doctor::setPatients);
            mapper.skip(Doctor::setVisits);
        });

        TypeMap<DoctorUpdateDTO, Doctor> doctorUpdateMap = modelMapper.typeMap(DoctorUpdateDTO.class, Doctor.class)
                .setProvider(req -> new Doctor());
        doctorUpdateMap.addMappings(mapper -> {
            mapper.map(DoctorUpdateDTO::getId, Doctor::setId);
            mapper.map(DoctorUpdateDTO::getName, Doctor::setName);
            mapper.map(DoctorUpdateDTO::isGeneralPractitioner, Doctor::setGeneralPractitioner);
            mapper.map(DoctorUpdateDTO::getImageUrl, Doctor::setImageUrl);
            mapper.skip(Doctor::setSpecialties);
            mapper.skip(Doctor::setPatients);
            mapper.skip(Doctor::setVisits);
        });

        TypeMap<Doctor, DoctorViewDTO> doctorViewMap = modelMapper.typeMap(Doctor.class, DoctorViewDTO.class)
                .setProvider(req -> new DoctorViewDTO());
        doctorViewMap.addMappings(mapper -> {
            mapper.map(Doctor::getId, DoctorViewDTO::setId);
            mapper.map(Doctor::getName, DoctorViewDTO::setName);
            mapper.map(Doctor::getUniqueIdNumber, DoctorViewDTO::setUniqueIdNumber);
            mapper.map(Doctor::isGeneralPractitioner, DoctorViewDTO::setGeneralPractitioner);
            mapper.map(Doctor::getImageUrl, DoctorViewDTO::setImageUrl);
            mapper.using(ctx -> {
                Set<Specialty> specialties = (Set<Specialty>) ctx.getSource();
                if (specialties == null) return null;
                return specialties.stream()
                        .map(specialty -> modelMapper.map(specialty, SpecialtyViewDTO.class))
                        .collect(Collectors.toSet());
            }).map(Doctor::getSpecialties, DoctorViewDTO::setSpecialties);
        });
    }

    private void configureVisitMappings(ModelMapper modelMapper) {
        TypeMap<VisitCreateDTO, Visit> visitCreateMap = modelMapper.typeMap(VisitCreateDTO.class, Visit.class)
                .setProvider(req -> new Visit());
        visitCreateMap.addMappings(mapper -> {
            mapper.map(VisitCreateDTO::getVisitDate, Visit::setVisitDate);
            mapper.map(VisitCreateDTO::isSickLeaveIssued, Visit::setSickLeaveIssued);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Patient patient = new Patient();
                patient.setId(id);
                return patient;
            }).map(VisitCreateDTO::getPatientId, Visit::setPatient);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Doctor doctor = new Doctor();
                doctor.setId(id);
                return doctor;
            }).map(VisitCreateDTO::getDoctorId, Visit::setDoctor);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setId(id);
                return diagnosis;
            }).map(VisitCreateDTO::getDiagnosisId, Visit::setDiagnosis);
            mapper.skip(Visit::setSickLeave);
            mapper.skip(Visit::setTreatment);
        });

        TypeMap<VisitUpdateDTO, Visit> visitUpdateMap = modelMapper.typeMap(VisitUpdateDTO.class, Visit.class)
                .setProvider(req -> new Visit());
        visitUpdateMap.addMappings(mapper -> {
            mapper.map(VisitUpdateDTO::getId, Visit::setId);
            mapper.map(VisitUpdateDTO::getVisitDate, Visit::setVisitDate);
            mapper.map(VisitUpdateDTO::isSickLeaveIssued, Visit::setSickLeaveIssued);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Patient patient = new Patient();
                patient.setId(id);
                return patient;
            }).map(VisitUpdateDTO::getPatientId, Visit::setPatient);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Doctor doctor = new Doctor();
                doctor.setId(id);
                return doctor;
            }).map(VisitUpdateDTO::getDoctorId, Visit::setDoctor);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setId(id);
                return diagnosis;
            }).map(VisitUpdateDTO::getDiagnosisId, Visit::setDiagnosis);
            mapper.skip(Visit::setSickLeave);
            mapper.skip(Visit::setTreatment);
        });

        TypeMap<Visit, VisitViewDTO> visitViewMap = modelMapper.typeMap(Visit.class, VisitViewDTO.class)
                .setProvider(req -> new VisitViewDTO());
        visitViewMap.addMappings(mapper -> {
            mapper.map(Visit::getId, VisitViewDTO::setId);
            mapper.map(Visit::getVisitDate, VisitViewDTO::setVisitDate);
            mapper.map(Visit::isSickLeaveIssued, VisitViewDTO::setSickLeaveIssued);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), PatientViewDTO.class) : null)
                    .map(Visit::getPatient, VisitViewDTO::setPatient);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), DoctorViewDTO.class) : null)
                    .map(Visit::getDoctor, VisitViewDTO::setDoctor);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), DiagnosisViewDTO.class) : null)
                    .map(Visit::getDiagnosis, VisitViewDTO::setDiagnosis);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), SickLeaveViewDTO.class) : null)
                    .map(Visit::getSickLeave, VisitViewDTO::setSickLeave);
            mapper.using(ctx -> ctx.getSource() != null ? modelMapper.map(ctx.getSource(), TreatmentViewDTO.class) : null)
                    .map(Visit::getTreatment, VisitViewDTO::setTreatment);
        });
    }

    private void configureTreatmentMappings(ModelMapper modelMapper) {
        TypeMap<TreatmentCreateDTO, Treatment> treatmentCreateMap = modelMapper.typeMap(TreatmentCreateDTO.class, Treatment.class)
                .setProvider(req -> new Treatment());
        treatmentCreateMap.addMappings(mapper -> {
            mapper.map(TreatmentCreateDTO::getDescription, Treatment::setDescription);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Visit visit = new Visit();
                visit.setId(id);
                return visit;
            }).map(TreatmentCreateDTO::getVisitId, Treatment::setVisit);
            mapper.skip(Treatment::setMedicines);
        });

        TypeMap<TreatmentUpdateDTO, Treatment> treatmentUpdateMap = modelMapper.typeMap(TreatmentUpdateDTO.class, Treatment.class)
                .setProvider(req -> new Treatment());
        treatmentUpdateMap.addMappings(mapper -> {
            mapper.map(TreatmentUpdateDTO::getId, Treatment::setId);
            mapper.map(TreatmentUpdateDTO::getDescription, Treatment::setDescription);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Visit visit = new Visit();
                visit.setId(id);
                return visit;
            }).map(TreatmentUpdateDTO::getVisitId, Treatment::setVisit);
            mapper.skip(Treatment::setMedicines);
        });

        TypeMap<Treatment, TreatmentViewDTO> treatmentViewMap = modelMapper.typeMap(Treatment.class, TreatmentViewDTO.class)
                .setProvider(req -> new TreatmentViewDTO());
        treatmentViewMap.addMappings(mapper -> {
            mapper.map(Treatment::getId, TreatmentViewDTO::setId);
            mapper.map(Treatment::getDescription, TreatmentViewDTO::setDescription);
            mapper.using(ctx -> ctx.getSource() != null ? ((Visit) ctx.getSource()).getId() : null)
                    .map(Treatment::getVisit, TreatmentViewDTO::setVisitId);
            mapper.using(ctx -> {
                List<Medicine> medicines = (List<Medicine>) ctx.getSource();
                if (medicines == null) return null;
                return medicines.stream()
                        .map(medicine -> modelMapper.map(medicine, MedicineViewDTO.class))
                        .collect(Collectors.toList());
            }).map(Treatment::getMedicines, TreatmentViewDTO::setMedicines);
        });
    }

    private void configureMedicineMappings(ModelMapper modelMapper) {
        TypeMap<MedicineCreateDTO, Medicine> medicineCreateMap = modelMapper.typeMap(MedicineCreateDTO.class, Medicine.class)
                .setProvider(req -> new Medicine());
        medicineCreateMap.addMappings(mapper -> {
            mapper.map(MedicineCreateDTO::getName, Medicine::setName);
            mapper.map(MedicineCreateDTO::getDosage, Medicine::setDosage);
            mapper.map(MedicineCreateDTO::getFrequency, Medicine::setFrequency);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Treatment treatment = new Treatment();
                treatment.setId(id);
                return treatment;
            }).map(MedicineCreateDTO::getTreatmentId, Medicine::setTreatment);
        });

        TypeMap<MedicineUpdateDTO, Medicine> medicineUpdateMap = modelMapper.typeMap(MedicineUpdateDTO.class, Medicine.class)
                .setProvider(req -> new Medicine());
        medicineUpdateMap.addMappings(mapper -> {
            mapper.map(MedicineUpdateDTO::getId, Medicine::setId);
            mapper.map(MedicineUpdateDTO::getName, Medicine::setName);
            mapper.map(MedicineUpdateDTO::getDosage, Medicine::setDosage);
            mapper.map(MedicineUpdateDTO::getFrequency, Medicine::setFrequency);
            mapper.using(ctx -> {
                Long id = (Long) ctx.getSource();
                if (id == null) return null;
                Treatment treatment = new Treatment();
                treatment.setId(id);
                return treatment;
            }).map(MedicineUpdateDTO::getTreatmentId, Medicine::setTreatment);
        });

        TypeMap<Medicine, MedicineViewDTO> medicineViewMap = modelMapper.typeMap(Medicine.class, MedicineViewDTO.class)
                .setProvider(req -> new MedicineViewDTO());
        medicineViewMap.addMappings(mapper -> {
            mapper.map(Medicine::getId, MedicineViewDTO::setId);
            mapper.map(Medicine::getName, MedicineViewDTO::setName);
            mapper.map(Medicine::getDosage, MedicineViewDTO::setDosage);
            mapper.map(Medicine::getFrequency, MedicineViewDTO::setFrequency);
            mapper.using(ctx -> ctx.getSource() != null ? ((Treatment) ctx.getSource()).getId() : null)
                    .map(Medicine::getTreatment, MedicineViewDTO::setTreatmentId);
        });
    }

    private void configureSpecialtyMappings(ModelMapper modelMapper) {
        TypeMap<SpecialtyCreateDTO, Specialty> specialtyCreateMap = modelMapper.typeMap(SpecialtyCreateDTO.class, Specialty.class)
                .setProvider(req -> new Specialty());
        specialtyCreateMap.addMappings(mapper -> {
            mapper.map(SpecialtyCreateDTO::getName, Specialty::setName);
            mapper.map(SpecialtyCreateDTO::getDescription, Specialty::setDescription);
        });

        TypeMap<SpecialtyUpdateDTO, Specialty> specialtyUpdateMap = modelMapper.typeMap(SpecialtyUpdateDTO.class, Specialty.class)
                .setProvider(req -> new Specialty());
        specialtyUpdateMap.addMappings(mapper -> {
            mapper.map(SpecialtyUpdateDTO::getId, Specialty::setId);
            mapper.map(SpecialtyUpdateDTO::getName, Specialty::setName);
            mapper.map(SpecialtyUpdateDTO::getDescription, Specialty::setDescription);
        });

        TypeMap<Specialty, SpecialtyViewDTO> specialtyViewMap = modelMapper.typeMap(Specialty.class, SpecialtyViewDTO.class)
                .setProvider(req -> new SpecialtyViewDTO());
        specialtyViewMap.addMappings(mapper -> {
            mapper.map(Specialty::getId, SpecialtyViewDTO::setId);
            mapper.map(Specialty::getName, SpecialtyViewDTO::setName);
            mapper.map(Specialty::getDescription, SpecialtyViewDTO::setDescription);
        });
    }
}