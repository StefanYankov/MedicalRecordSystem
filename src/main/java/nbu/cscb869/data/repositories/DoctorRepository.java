package nbu.cscb869.data.repositories;


import nbu.cscb869.data.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Doctor entities.
 */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUniqueIdNumber(String uniqueIdNumber);

    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdIncludingDeleted(Long id);
}