package nbu.cscb869.data.seeding;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer {
    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final VisitRepository visitRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final TreatmentRepository treatmentRepository;
    private final MedicineRepository medicineRepository;

    @PostConstruct
    public void init() {
        // Specialty
        Specialty cardiology = specialtyRepository.findByName("Cardiology")
                .orElseGet(() -> {
                    Specialty newSpecialty = new Specialty();
                    newSpecialty.setName("Cardiology");
                    return specialtyRepository.save(newSpecialty);
                });

        // Doctor
        Doctor doctor = doctorRepository.findByUniqueIdNumber("DOC123")
                .orElseGet(() -> {
                    Doctor newDoctor = new Doctor();
                    newDoctor.setUniqueIdNumber("DOC123");
                    newDoctor.setName("Dr. Smith");
                    newDoctor.setSpecialties(Set.of(cardiology));
                    newDoctor.setIsGeneralPractitioner(true);
                    return doctorRepository.save(newDoctor);
                });

        // Patient
        Patient patient = patientRepository.findByEgn("1234567890")
                .orElseGet(() -> {
                    Patient newPatient = new Patient();
                    newPatient.setName("John Doe");
                    newPatient.setEgn("1234567890");
                    newPatient.setLastInsurancePaymentDate(LocalDate.now());
                    newPatient.setGeneralPractitioner(doctor);
                    return patientRepository.save(newPatient);
                });

        // Diagnosis
        Diagnosis diagnosis = diagnosisRepository.findByName("Hypertension")
                .orElseGet(() -> {
                    Diagnosis newDiagnosis = new Diagnosis();
                    newDiagnosis.setName("Hypertension");
                    return diagnosisRepository.save(newDiagnosis);
                });

        // Visit
        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(true);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visitRepository.save(visit);

        // Treatment
        Treatment treatment = new Treatment();
        treatment.setInstructions("Rest for 3 days");
        treatment.setVisit(visit);
        treatmentRepository.save(treatment);

        // Medicines
        Medicine medicine1 = new Medicine();
        medicine1.setName("Ibuprofen");
        medicine1.setDosage("200mg");
        medicine1.setFrequency("Twice daily");
        medicine1.setTreatment(treatment);

        Medicine medicine2 = new Medicine();
        medicine2.setName("Paracetamol");
        medicine2.setDosage("500mg");
        medicine2.setFrequency("As needed");
        medicine2.setTreatment(treatment);

        medicineRepository.saveAll(Set.of(medicine1, medicine2));

        // SickLeave
        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);
        sickLeaveRepository.save(sickLeave);
    }
}