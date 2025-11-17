package nbu.cscb869.data.specifications;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import org.springframework.data.jpa.domain.Specification;

/**
 * A utility class that provides reusable {@link Specification} objects for querying {@link Doctor} entities.
 * This class centralizes the construction of complex, programmatic query criteria, making repository queries
 * more readable and maintainable. Each method returns a Specification that can be combined with others
 * to build dynamic queries.
 */
public class DoctorSpecification {

    /**
     * Creates a specification to find doctors who have a specific specialty.
     *
     * @param specialty The {@link Specialty} entity to filter by.
     * @return A {@link Specification} that can be used in repository queries.
     */
    public static Specification<Doctor> hasSpecialty(Specialty specialty) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isMember(specialty, root.get("specialties"));
    }

    /**
     * Creates a specification to find doctors who are marked as General Practitioners.
     *
     * @return A {@link Specification} that filters for general practitioners.
     */
    public static Specification<Doctor> isGeneralPractitioner() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("isGeneralPractitioner"));
    }

    /**
     * Creates a specification to find doctors who have been approved by an admin.
     *
     * @return A {@link Specification} that filters for approved doctors.
     */
    public static Specification<Doctor> isApproved() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("isApproved"));
    }

    /**
     * Creates a specification to find doctors whose name contains the given filter string (case-insensitive).
     *
     * @param filter The string to search for within the doctor's name.
     * @return A {@link Specification} for a case-insensitive "like" search on the name.
     */
    public static Specification<Doctor> nameContains(String filter) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + filter.toLowerCase() + "%");
    }

    /**
     * Creates a specification to find doctors whose specialty name contains the given filter string (case-insensitive).
     * This involves a join to the specialties table.
     *
     * @param filter The string to search for within the specialty names.
     * @return A {@link Specification} for a case-insensitive "like" search on specialty names.
     */
    public static Specification<Doctor> specialtyNameContains(String filter) {
        return (root, query, criteriaBuilder) -> {
            Join<Doctor, Specialty> specialtyJoin = root.join("specialties", JoinType.LEFT);
            return criteriaBuilder.like(criteriaBuilder.lower(specialtyJoin.get("name")), "%" + filter.toLowerCase() + "%");
        };
    }

    /**
     * Creates a specification that eagerly fetches the 'specialties' collection for each doctor.
     * This is a performance optimization to prevent the N+1 query problem by using a JOIN FETCH.
     * It also ensures the query returns distinct doctors.
     *
     * @return A {@link Specification} that modifies the query to fetch associated specialties.
     */
    public static Specification<Doctor> fetchSpecialties() {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("specialties", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }
}
