package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends SoftDeleteRepository<Doctor, Long> {
    Optional<Doctor> findByUniqueIdNumber(String uniqueIdNumber);
    Page<Doctor> findByIsGeneralPractitionerTrue(Pageable pageable);
    long countPatientsByGeneralPractitioner(Doctor doctor);
    long countVisitsByDoctor(Doctor doctor);
    @Query("SELECT d, COUNT(sl) FROM Doctor d LEFT JOIN Visit v ON v.doctor = d LEFT JOIN SickLeave sl ON sl.visit = v WHERE v.isDeleted = false AND sl.isDeleted = false GROUP BY d")
    Page<Object[]> findDoctorsBySickLeaveCount(Pageable pageable);
}