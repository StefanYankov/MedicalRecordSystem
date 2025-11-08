package nbu.cscb869.config;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * This component runs on application startup only when the 'dev' profile is active.
 * It seeds the database with initial data. Each seeding method is designed to be idempotent,
 * meaning it can be run safely multiple times without creating duplicate data or crashing.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final VisitRepository visitRepository;
    private final SickLeaveRepository sickLeaveRepository;

    public DataSeeder(SpecialtyRepository specialtyRepository, DoctorRepository doctorRepository, PatientRepository patientRepository, DiagnosisRepository diagnosisRepository, VisitRepository visitRepository, SickLeaveRepository sickLeaveRepository) {
        this.specialtyRepository = specialtyRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.visitRepository = visitRepository;
        this.sickLeaveRepository = sickLeaveRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedSpecialties();
        seedDoctors();
        seedDiagnoses();
        seedPatients();
        seedVisitsAndSickLeaves();
    }

    private void seedSpecialties() {
        if (specialtyRepository.count() == 0) {
            logger.info("Seeding Specialties...");
            specialtyRepository.saveAll(Arrays.asList(
                    Specialty.builder().name("General Practice").description("Primary care provider.").build(),
                    Specialty.builder().name("Cardiology").description("Heart and blood vessels.").build(),
                    Specialty.builder().name("Dermatology").description("Skin, hair, and nails.").build(),
                    Specialty.builder().name("Neurology").description("Nervous system disorders.").build(),
                    Specialty.builder().name("Pediatrics").description("Medical care of infants, children, and adolescents.").build()
            ));
        }
    }

    private void seedDoctors() {
        if (doctorRepository.count() == 0) {
            logger.info("Seeding Doctors...");
            Specialty gp = specialtyRepository.findByName("General Practice").orElseThrow();
            Specialty cardiology = specialtyRepository.findByName("Cardiology").orElseThrow();
            Specialty dermatology = specialtyRepository.findByName("Dermatology").orElseThrow();

            doctorRepository.saveAll(List.of(
                    Doctor.builder().name("Dr. John Smith").keycloakId("doc1-keycloak-id").uniqueIdNumber("DOC001").isGeneralPractitioner(true).specialties(new HashSet<>(Set.of(gp, cardiology))).build(),
                    Doctor.builder().name("Dr. Emily Jones").keycloakId("doc2-keycloak-id").uniqueIdNumber("DOC002").isGeneralPractitioner(true).specialties(new HashSet<>(Set.of(gp))).build(),
                    Doctor.builder().name("Dr. Alan Grant").keycloakId("doc3-keycloak-id").uniqueIdNumber("DOC003").isGeneralPractitioner(false).specialties(new HashSet<>(Set.of(dermatology))).build()
            ));
        }
    }

    private void seedDiagnoses() {
        if (diagnosisRepository.count() == 0) {
            logger.info("Seeding Diagnoses...");
            diagnosisRepository.saveAll(List.of(
                    Diagnosis.builder().name("Common Cold").description("Viral infectious disease of the upper respiratory tract.").build(),
                    Diagnosis.builder().name("Hypertension").description("High blood pressure.").build(),
                    Diagnosis.builder().name("Type 2 Diabetes").description("A chronic condition that affects the way the body processes blood sugar.").build()
            ));
        }
    }

    private void seedPatients() {
        Doctor drSmith = doctorRepository.findByUniqueIdNumber("DOC001").orElseGet(() -> {
            logger.warn("Could not find Dr. Smith by ID 'DOC001' during patient seeding. A new GP will be created.");
            Specialty gp = specialtyRepository.findByName("General Practice").orElseThrow();
            return doctorRepository.save(Doctor.builder().name("Dr. John Smith").keycloakId("doc1-keycloak-id").uniqueIdNumber("DOC001").isGeneralPractitioner(true).specialties(new HashSet<>(Set.of(gp))).build());
        });

        patientRepository.findByKeycloakId("b9cc3250-d069-4f74-b1de-03de57fc32d1").or(() -> {
            logger.info("Seeding patient Serafim Gerasimoff...");
            return Optional.of(patientRepository.save(Patient.builder().name("Serafim Gerasimoff").egn(DataUtils.generateValidEgn()).keycloakId("b9cc3250-d069-4f74-b1de-03de57fc32d1").generalPractitioner(drSmith).build()));
        });

        patientRepository.findByKeycloakId("patient2-keycloak-id").or(() -> {
            logger.info("Seeding patient Jane Doe...");
            return Optional.of(patientRepository.save(Patient.builder().name("Jane Doe").egn(DataUtils.generateValidEgn()).keycloakId("patient2-keycloak-id").generalPractitioner(drSmith).build()));
        });
    }

    private void seedVisitsAndSickLeaves() {
        patientRepository.findByKeycloakId("b9cc3250-d069-4f74-b1de-03de57fc32d1").ifPresent(patient1 -> {
            if (visitRepository.findByPatient(patient1, Pageable.unpaged()).isEmpty()) {
                logger.info("Seeding visits for patient {}...", patient1.getName());

                Doctor drSmith = doctorRepository.findByUniqueIdNumber("DOC001").orElseThrow();
                Doctor drGrant = doctorRepository.findByUniqueIdNumber("DOC003").orElseThrow();
                Diagnosis cold = diagnosisRepository.findByName("Common Cold").orElseThrow();
                Diagnosis hypertension = diagnosisRepository.findByName("Hypertension").orElseThrow();

                Visit visit1 = Visit.builder().patient(patient1).doctor(drSmith).visitDate(LocalDate.now().minusMonths(1)).visitTime(LocalTime.of(10, 30)).status(VisitStatus.COMPLETED).diagnosis(cold).build();
                Visit visit2 = Visit.builder().patient(patient1).doctor(drGrant).visitDate(LocalDate.now().minusWeeks(2)).visitTime(LocalTime.of(14, 0)).status(VisitStatus.COMPLETED).diagnosis(hypertension).build();
                Visit visit3 = Visit.builder().patient(patient1).doctor(drSmith).visitDate(LocalDate.now().minusDays(3)).visitTime(LocalTime.of(9, 0)).status(VisitStatus.COMPLETED).diagnosis(cold).build();

                visitRepository.saveAll(List.of(visit1, visit2, visit3));

                SickLeave sickLeave = SickLeave.builder().visit(visit3).startDate(visit3.getVisitDate()).durationDays(7).build();
                sickLeaveRepository.save(sickLeave);

                visit3.setSickLeave(sickLeave);
                visitRepository.save(visit3);
            }
        });
    }
}
