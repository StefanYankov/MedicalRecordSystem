package nbu.cscb869.data.seeding;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Initializes sample data for development environment.
 */
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
        // Specialties
        Specialty cardiology = specialtyRepository.findByName("Cardiology")
                .orElseGet(() -> {
                    Specialty specialty = new Specialty();
                    specialty.setName("Cardiology");
                    return specialtyRepository.save(specialty);
                });

        Specialty neurology = specialtyRepository.findByName("Neurology")
                .orElseGet(() -> {
                    Specialty specialty = new Specialty();
                    specialty.setName("Neurology");
                    return specialtyRepository.save(specialty);
                });

        // Doctors
        Doctor gp1 = doctorRepository.findByUniqueIdNumber("DOC123")
                .orElseGet(() -> {
                    Doctor doctor = new Doctor();
                    doctor.setUniqueIdNumber("DOC123");
                    doctor.setName("Dr. John Smith");
                    doctor.setSpecialties(Set.of(cardiology));
                    doctor.setGeneralPractitioner(true);
                    return doctorRepository.save(doctor);
                });

        Doctor gp2 = doctorRepository.findByUniqueIdNumber("DOC456")
                .orElseGet(() -> {
                    Doctor doctor = new Doctor();
                    doctor.setUniqueIdNumber("DOC456");
                    doctor.setName("Dr. Anna Brown");
                    doctor.setSpecialties(Set.of(neurology));
                    doctor.setGeneralPractitioner(true);
                    return doctorRepository.save(doctor);
                });

        Doctor specialist = doctorRepository.findByUniqueIdNumber("DOC789")
                .orElseGet(() -> {
                    Doctor doctor = new Doctor();
                    doctor.setUniqueIdNumber("DOC789");
                    doctor.setName("Dr. Mark Wilson");
                    doctor.setSpecialties(Set.of(cardiology, neurology));
                    doctor.setGeneralPractitioner(false);
                    return doctorRepository.save(doctor);
                });

        // Patients
        Patient patient1 = patientRepository.findByEgn("7501010010")
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setName("John Doe");
                    patient.setEgn("7501010010");
                    patient.setLastInsurancePaymentDate(LocalDate.now());
                    patient.setGeneralPractitioner(gp1);
                    return patientRepository.save(patient);
                });

        Patient patient2 = patientRepository.findByEgn("8002020016")
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setName("Jane Roe");
                    patient.setEgn("8002020016");
                    patient.setLastInsurancePaymentDate(LocalDate.now().minusMonths(5));
                    patient.setGeneralPractitioner(gp1);
                    return patientRepository.save(patient);
                });

        Patient patient3 = patientRepository.findByEgn("8503030022")
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setName("Alice Green");
                    patient.setEgn("8503030022");
                    patient.setLastInsurancePaymentDate(LocalDate.now().minusMonths(7));
                    patient.setGeneralPractitioner(gp2);
                    return patientRepository.save(patient);
                });

        // Diagnoses
        Diagnosis hypertension = diagnosisRepository.findByName("Hypertension")
                .orElseGet(() -> {
                    Diagnosis diagnosis = new Diagnosis();
                    diagnosis.setName("Hypertension");
                    diagnosis.setDescription("High blood pressure");
                    return diagnosisRepository.save(diagnosis);
                });

        Diagnosis flu = diagnosisRepository.findByName("Flu")
                .orElseGet(() -> {
                    Diagnosis diagnosis = new Diagnosis();
                    diagnosis.setName("Flu");
                    diagnosis.setDescription("Viral infection");
                    return diagnosisRepository.save(diagnosis);
                });

        Diagnosis migraine = diagnosisRepository.findByName("Migraine")
                .orElseGet(() -> {
                    Diagnosis diagnosis = new Diagnosis();
                    diagnosis.setName("Migraine");
                    diagnosis.setDescription("Severe headache");
                    return diagnosisRepository.save(diagnosis);
                });

        // Visits
        Visit visit1 = new Visit();
        visit1.setVisitDate(LocalDate.now().minusDays(10));
        visit1.setSickLeaveIssued(true);
        visit1.setPatient(patient1);
        visit1.setDoctor(gp1);
        visit1.setDiagnosis(hypertension);
        visitRepository.save(visit1);

        Visit visit2 = new Visit();
        visit2.setVisitDate(LocalDate.now().minusDays(5));
        visit2.setSickLeaveIssued(false);
        visit2.setPatient(patient1);
        visit2.setDoctor(specialist);
        visit2.setDiagnosis(flu);
        visitRepository.save(visit2);

        Visit visit3 = new Visit();
        visit3.setVisitDate(LocalDate.now().minusMonths(1));
        visit3.setSickLeaveIssued(true);
        visit3.setPatient(patient2);
        visit3.setDoctor(gp1);
        visit3.setDiagnosis(hypertension);
        visitRepository.save(visit3);

        Visit visit4 = new Visit();
        visit4.setVisitDate(LocalDate.now().minusMonths(2));
        visit4.setSickLeaveIssued(false);
        visit4.setPatient(patient3);
        visit4.setDoctor(gp2);
        visit4.setDiagnosis(migraine);
        visitRepository.save(visit4);

        // Treatments
        Treatment treatment1 = new Treatment();
        treatment1.setVisit(visit1);
        Medicine medicine1 = new Medicine();
        medicine1.setName("Lisinopril");
        medicine1.setDosage("10mg");
        medicine1.setFrequency("Once daily");
        medicine1.setTreatment(treatment1);
        Medicine medicine2 = new Medicine();
        medicine2.setName("Aspirin");
        medicine2.setDosage("75mg");
        medicine2.setFrequency("Once daily");
        medicine2.setTreatment(treatment1);
        treatment1.setMedicines(List.of(medicine1, medicine2));
        treatmentRepository.save(treatment1);

        Treatment treatment2 = new Treatment();
        treatment2.setVisit(visit2);
        Medicine medicine3 = new Medicine();
        medicine3.setName("Paracetamol");
        medicine3.setDosage("500mg");
        medicine3.setFrequency("As needed");
        medicine3.setTreatment(treatment2);
        treatment2.setMedicines(List.of(medicine3));
        treatmentRepository.save(treatment2);

        // Sick Leaves
        SickLeave sickLeave1 = new SickLeave();
        sickLeave1.setStartDate(LocalDate.now().minusDays(10));
        sickLeave1.setDurationDays(5);
        sickLeave1.setVisit(visit1);
        sickLeaveRepository.save(sickLeave1);

        SickLeave sickLeave2 = new SickLeave();
        sickLeave2.setStartDate(LocalDate.now().minusMonths(1));
        sickLeave2.setDurationDays(3);
        sickLeave2.setVisit(visit3);
        sickLeaveRepository.save(sickLeave2);
    }
}