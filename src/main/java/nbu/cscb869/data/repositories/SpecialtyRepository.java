package nbu.cscb869.data.repositories;

import nbu.cscb869.data.models.Specialty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialtyRepository extends SoftDeleteRepository<Specialty, Long> {
    Optional<Specialty> findByName(String name);
    Optional<Specialty> findByNameAndIsDeletedFalse(String name);
}