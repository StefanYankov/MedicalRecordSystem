package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SickLeaveRepository extends SoftDeleteRepository<SickLeave, Long> {
    Page<SickLeave> findByPatient(Patient patient, Pageable pageable);
    Page<SickLeave> findByDoctor(Doctor doctor, Pageable pageable);
    @Query("SELECT MONTH(sl.startDate), COUNT(sl) FROM SickLeave sl WHERE YEAR(sl.startDate) = :year AND sl.isDeleted = false GROUP BY MONTH(sl.startDate)")
    Page<Object[]> findMonthWithMostSickLeaves(int year, Pageable pageable);
}