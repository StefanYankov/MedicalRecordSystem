package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.SickLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for managing {@link SickLeave} entities with soft delete support.
 */
public interface SickLeaveRepository extends JpaRepository<SickLeave, Long> {
    /**
     * Retrieves a page of non-deleted sick leave records.
     *
     * @param pageable pagination information
     * @return a page of sick leave entities
     */
    Page<SickLeave> findAll(Pageable pageable);

    /**
     * Identifies the year and month with the highest number of sick leaves.
     *
     * @return a list of DTOs with year, month, and sick leave counts, sorted by count descending
     */
    @Query("SELECT new nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO(YEAR(s.startDate), MONTH(s.startDate), COUNT(s)) " +
            "FROM SickLeave s GROUP BY YEAR(s.startDate), MONTH(s.startDate) ORDER BY COUNT(s) DESC")
    List<YearMonthSickLeaveCountDTO> findYearMonthWithMostSickLeaves();

    /**
     * Retrieves a list of doctors with their sick leave counts, sorted by count descending.
     *
     * @return a list of DTOs with doctor and sick leave count
     */
    @Query("SELECT new nbu.cscb869.data.dto.DoctorSickLeaveCountDTO(v.doctor, COUNT(s)) " +
            "FROM SickLeave s JOIN s.visit v GROUP BY v.doctor ORDER BY COUNT(s) DESC")
    List<DoctorSickLeaveCountDTO> findDoctorsWithMostSickLeaves();
}