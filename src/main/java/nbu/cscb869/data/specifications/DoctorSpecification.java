package nbu.cscb869.data.specifications;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import org.springframework.data.jpa.domain.Specification;

public class DoctorSpecification {

    /**
     * Creates a JPA Specification to find doctors who have a specific specialty.
     *
     * @param specialty The Specialty entity to search for.
     * @return A Specification for querying Doctors.
     */
    public static Specification<Doctor> hasSpecialty(Specialty specialty) {
        return (root, query, criteriaBuilder) -> {
            // Join the Doctor's 'specialties' collection
            // and check if the given specialty is a member of that collection.
            return criteriaBuilder.isMember(specialty, root.get("specialties"));
        };
    }
}
