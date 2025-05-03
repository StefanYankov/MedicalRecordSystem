package nbu.cscb869.data.repositories;

import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.repositories.base.SoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for managing {@link SickLeave} entities with soft delete support.
 */
public interface SickLeaveRepository extends SoftDeleteRepository<SickLeave, Long> {
    /**
     * Retrieves a page of non-deleted sick leave records.
     * @param pageable pagination information
     * @return a page of sick leave entities where {@code isDeleted = false}
     */
    @Query("SELECT s FROM SickLeave s WHERE s.isDeleted = false")
    Page<SickLeave> findAllActive(Pageable pageable);

    /**
     * Identifies the year and month with the highest number of sick leaves.
     * @return a list of DTOs with year, month, and sick leave counts, sorted by count descending
     */
    @Query("SELECT new nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO(YEAR(s.startDate), MONTH(s.startDate), COUNT(s)) " +
            "FROM SickLeave s WHERE s.isDeleted = false " +
            "GROUP BY YEAR(s.startDate), MONTH(s.startDate) ORDER BY COUNT(s) DESC")
    List<YearMonthSickLeaveCountDTO> findYearMonthWithMostSickLeaves();
}